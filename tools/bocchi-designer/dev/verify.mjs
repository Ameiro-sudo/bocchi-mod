/* ============================================================================
 * verify.mjs - Bocchi Designer 行为快照采集 (重构等价性验证基建)
 *
 * 用 headless 浏览器打开 Designer, 依次切到三个舞台, 采集:
 *   - #stage 内所有带 id 元素的几何 (相对 stage 坐标, 已除以缩放) + 关键计算样式
 *   - #jsonPreview 的 design.json 内容
 *   - console 警告/错误与 pageerror
 * 产物为单个 JSON; 配合 compare.mjs 对比重构前后是否逐属性等价。
 *
 * EXCLUDE 表: 定时器/常驻动画驱动的属性天然不稳定, 不参与对比。
 *
 * 用法:
 *   node dev/verify.mjs --url http://127.0.0.1:8833/ --out dev/dumps/base.json [--shots dev/shots]
 * 依赖: puppeteer-core (默认复用 D:/project/blog/node_modules, 可用 PUPPETEER_REQUIRE 覆盖)
 *       + 本机 Edge (EDGE_PATH 可覆盖)
 * ==========================================================================*/
import { createRequire } from "module";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));

function arg(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 ? process.argv[i + 1] : def;
}
const URL_BASE = arg("url", "http://127.0.0.1:8833/");
const OUT = arg("out", path.join(ROOT, "dev", "dumps", "baseline.json"));
const SHOTS = arg("shots", null);

const EDGE = process.env.EDGE_PATH || "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const requireFrom = process.env.PUPPETEER_REQUIRE || "D:/project/blog/package.json";
const puppeteer = createRequire(requireFrom)("puppeteer-core");

/* 排除表: [stage, elementId, 属性名正则] */
/* 属性过滤用字符串形式的正则 (page.evaluate 序列化不保留 RegExp 对象) */
const EXCLUDE = [
  ["splash", "progressFill", "width"],
  ["splash", "loadingIcon", "backgroundPosition|backgroundImage"],
  ["splash", "tapText", "opacity"],
  ["misayos", "mTachie", "^transform$"],
  ["misayos", "mRecord", "^transform$"],
];
function excluded(stage, id, prop) {
  return EXCLUDE.some(([s, e, re]) => s === stage && e === id && re.test(prop));
}

const STAGES = [
  { tab: "swMisayos", name: "misayos" },
  { tab: "swPoulsen", name: "poulsen" },
  { tab: "swSplash", name: "splash" },
];

/* 在浏览器里执行的采集函数 */
function dumpStage(stageName, EXCLUDE) {
  const excluded = (id, prop) =>
    EXCLUDE.some(([s, e, src]) => s === stageName && e === id && new RegExp(src).test(prop));
  const stage = document.getElementById("stage");
  /* 只采集当前舞台容器内的元素 (隐藏舞台的行内样式残留会引入时间性噪声) */
  const CONTAINER = { misayos: "misayosStage", poulsen: "poulsenStage", splash: "splashStage" };
  const scope = document.getElementById(CONTAINER[stageName]) || stage;
  /* 几何取行内样式的 px 值 (layout.js 的写入值, 浮点精确且不受旋转动画影响);
     无行内值的属性回退 offset 链 (整数精度)。禁用 getBoundingClientRect:
     它返回旋转变换后的包围盒, 随环境动画抖动。 */
  function geom(el) {
    const num = (v) => (v && v.endsWith("px") ? parseFloat(v) : null);
    const g = {
      left: num(el.style.left), top: num(el.style.top),
      width: num(el.style.width), height: num(el.style.height),
    };
    if (g.left == null || g.top == null || g.width == null || g.height == null) {
      let x = 0, y = 0, n = el;
      while (n && n !== stage) { x += n.offsetLeft; y += n.offsetTop; n = n.offsetParent; }
      if (g.left == null) g.left = x;
      if (g.top == null) g.top = y;
      if (g.width == null) g.width = el.offsetWidth;
      if (g.height == null) g.height = el.offsetHeight;
    }
    return g;
  }
  const PROPS = ["fontSize", "letterSpacing", "opacity", "color", "backgroundColor",
    "backgroundImage", "borderTopWidth", "lineHeight", "textAlign", "display"];
  const out = { elements: {} };
  for (const el of scope.querySelectorAll("[id]")) {
    if (el.id === "stage") continue;
    const rec = { rect: geom(el), styles: {}, attrs: {} };
    for (const k of ["left", "top", "width", "height"]) rec.rect[k] = Math.round(rec.rect[k] * 1000) / 1000;
    const tr = el.style.transform;
    if (tr && !excluded(el.id, "transform")) rec.attrs.inlineTransform = tr;
    const cs = getComputedStyle(el);
    for (const p of PROPS) {
      if (excluded(el.id, p)) continue;
      rec.styles[p] = cs[p];
    }
    if (el.dataset.baseRot != null) rec.attrs.baseRot = el.dataset.baseRot;
    if (el.tagName === "IMG") { rec.attrs.naturalW = el.naturalWidth; rec.attrs.naturalH = el.naturalHeight; }
    out.elements[el.id] = rec;
  }
  out.childCounts = {};
  for (const el of scope.querySelectorAll("[id]")) {
    if (el.id && el.id !== "stage") out.childCounts[el.id] = el.children.length;
  }
  const pre = document.getElementById("jsonPreview");
  out.designJSON = pre ? JSON.parse(pre.textContent) : null;
  out.statusText = (document.getElementById("status") || {}).textContent || "";
  return out;
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: EDGE,
    headless: true,
    args: ["--no-first-run", "--disable-sync", "--disable-gpu", "--font-render-hinting=none"],
  });
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1600, height: 900, deviceScaleFactor: 1 });

    const consoleMsgs = [];
    page.on("console", (m) => { if (["error", "warning"].includes(m.type())) consoleMsgs.push(`[${m.type()}] ${m.text()}`); });
    page.on("pageerror", (e) => consoleMsgs.push(`[pageerror] ${e.message}`));

    await page.goto(URL_BASE, { waitUntil: "networkidle2", timeout: 30000 });
    await page.evaluate(() => document.fonts.ready);
    await new Promise(r => setTimeout(r, 600)); // boot 尾部的 fonts.ready 重排

    const dump = { url: URL_BASE, viewport: "1600x900@1", stages: {}, console: [] };
    for (const s of STAGES) {
      await page.evaluate((tab) => document.getElementById(tab).click(), s.tab);
      await new Promise(r => setTimeout(r, 2400)); // 入场动画 (700ms + 40ms 级联) 完全落定
      dump.stages[s.name] = await page.evaluate(dumpStage, s.name, EXCLUDE);
      if (SHOTS) {
        fs.mkdirSync(SHOTS, { recursive: true });
        await page.screenshot({ path: path.join(SHOTS, `${s.name}.png`) });
      }
    }

    /* 交互烟测: 选中立绘 -> 方向键微调 -> Esc 取消; 导出按钮存在 */
    await page.evaluate(() => document.getElementById("swMisayos").click());
    await new Promise(r => setTimeout(r, 900));
    const smoke = await page.evaluate(() => {
      const res = {};
      const tachie = document.getElementById("mTachie");
      tachie.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, clientX: 700, clientY: 300 }));
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowLeft" }));
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowRight", shiftKey: true }));
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
      res.selBoxHiddenAfterEsc = document.getElementById("selBox").style.display === "none";
      res.exportBtnExists = !!document.getElementById("exportBtn");
      res.rangeInputCount = document.querySelectorAll(".controls input[type=range]").length;
      return res;
    });
    await new Promise(r => setTimeout(r, 500)); // scheduleSave debounce 250ms 落盘
    dump.smoke = {
      ...smoke,
      ovTachieXAfterNudge: await page.evaluate(() => {
        const st = JSON.parse(localStorage.getItem("bocchi-designer:v1"));
        return st && st.ov ? st.ov.tachieX : null;
      }),
    };

    /* 滑杆联动探针: 走真实 DOM 路径 (label 定位面板宽度滑杆 -> 改值触发 input -> 等两帧量几何) */
    const sliderProbe = await page.evaluate(() => {
      let row = null;
      for (const r of document.querySelectorAll(".slider-row")) {
        if (r.querySelector("label") && r.querySelector("label").textContent.includes("面板宽度")) { row = r; break; }
      }
      if (!row) return null;
      const input = row.querySelector("input[type=range]");
      const before = document.getElementById("mPanel").getBoundingClientRect().width;
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set;
      setter.call(input, "120");
      input.dispatchEvent(new Event("input", { bubbles: true }));
      return new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(() => {
        resolve({
          before: Math.round(before * 1000) / 1000,
          after: Math.round(document.getElementById("mPanel").getBoundingClientRect().width * 1000) / 1000,
        });
      })));
    });
    dump.sliderProbe = sliderProbe;

    dump.console = consoleMsgs;
    fs.mkdirSync(path.dirname(OUT), { recursive: true });
    fs.writeFileSync(OUT, JSON.stringify(dump, null, 1));
    console.log("dumped:", OUT);
    console.log("console warn/error:", consoleMsgs.length ? consoleMsgs.join(" | ") : "(none)");
    console.log("smoke:", JSON.stringify(dump.smoke));
    console.log("sliderProbe:", JSON.stringify(sliderProbe));
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });








