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
 *
 * 依赖 (均为外部环境, 不写进 package.json —— 本项目刻意保持零 npm 依赖):
 *   - puppeteer-core: 按 PUPPETEER_REQUIRE -> PUPPETEER_CORE_PATH -> 逐级向上查找
 *                     node_modules/puppeteer-core -> 裸 import (Node 标准解析/NODE_PATH)
 *                     的顺序解析; 全部失败时报出「试过什么 + 怎么修」, 见下方 loadPuppeteer()。
 *                     典型用法: PUPPETEER_CORE_PATH=<任意目录>/node_modules/puppeteer-core
 *   - 浏览器可执行文件: EDGE_PATH / PUPPETEER_EXECUTABLE_PATH / CHROME_PATH 环境变量优先,
 *                     否则按平台探测 Edge / Chrome / Chromium 常见安装路径,
 *                     最后交给 puppeteer.executablePath() 兜底。见下方 resolveBrowserExecutable()。
 * ==========================================================================*/
import { createRequire } from "node:module";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.dirname(HERE);

function arg(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 ? process.argv[i + 1] : def;
}
const URL_BASE = arg("url", "http://127.0.0.1:8833/");
const OUT = arg("out", path.join(ROOT, "dev", "dumps", "baseline.json"));
const SHOTS = arg("shots", null);

/* ---------------------------------------------------------------------------
 * puppeteer-core 解析
 *
 * 本项目刻意不在 package.json 里声明 puppeteer-core (零 npm 依赖是产品卖点),
 * 因此这里按顺序试多个来源, 而不是硬编码某个仓库的 node_modules 路径:
 *   1. PUPPETEER_REQUIRE   —— 指向某个 package.json, 从它所在位置解析
 *                             (覆盖口保留; 可指向任意一个装了 puppeteer-core 的仓库)
 *   2. PUPPETEER_CORE_PATH —— 直接指向 puppeteer-core 的模块入口 (目录或 .js 均可)
 *   3. 从本文件位置逐级向上找 node_modules/puppeteer-core
 *                             (dev/ -> tools/bocchi-designer/ -> 仓库根 -> 更上层)
 *   4. 裸 import("puppeteer-core") —— 走 Node 标准解析
 *                             (命中全局/父级 node_modules; ESM 不认 NODE_PATH,
 *                              故 NODE_PATH 另走一次 CJS 解析)
 * 全部失败时报出「试过哪些 + 怎么修」, 而不是裸的 MODULE_NOT_FOUND 堆栈。
 *
 * 常见用法:
 *   npm i --prefix C:\tools\pptr puppeteer-core
 *   PUPPETEER_CORE_PATH=C:\tools\pptr\node_modules\puppeteer-core node dev/verify.mjs
 *   # 或让 Node 标准解析兜底:
 *   NODE_PATH=C:\tools\pptr\node_modules node dev/verify.mjs
 * -------------------------------------------------------------------------*/
const puppeteerAttempts = [];

/* CJS 包经 ESM loader 加载时具名导出不可靠, 一律取 default 再退回命名空间 */
function unwrap(ns) {
  return ns && typeof ns === "object" && "default" in ns ? ns.default : ns;
}

/* 把 puppeteer-core 的入口 (目录/文件均可) 归一成可 import 的绝对文件 URL */
function entryOf(target) {
  const abs = path.resolve(target);
  if (fs.existsSync(abs) && fs.statSync(abs).isDirectory()) {
    /* 交给 Node 按 package.json 的 main/exports 解析, 不用手拼 index.js */
    return createRequire(path.join(abs, "__puppeteer_core_resolve__.js")).resolve(abs);
  }
  return abs;
}

async function loadPuppeteer() {
  /* 1) PUPPETEER_REQUIRE: 指向含 puppeteer-core 的 package.json */
  const requireFrom = process.env.PUPPETEER_REQUIRE;
  if (requireFrom) {
    const abs = path.resolve(requireFrom);
    try {
      const mod = createRequire(abs)("puppeteer-core");
      puppeteerAttempts.push(`PUPPETEER_REQUIRE=${abs} -> 命中`);
      return mod;
    } catch (e) {
      puppeteerAttempts.push(`PUPPETEER_REQUIRE=${abs} -> 失败 (${e.code || e.message})`);
    }
  } else {
    puppeteerAttempts.push("PUPPETEER_REQUIRE=<未设置>");
  }

  /* 2) PUPPETEER_CORE_PATH: 直接指向 puppeteer-core 模块入口 */
  const corePath = process.env.PUPPETEER_CORE_PATH;
  if (corePath) {
    const abs = path.resolve(corePath);
    try {
      const mod = unwrap(await import(pathToFileURL(entryOf(abs)).href));
      puppeteerAttempts.push(`PUPPETEER_CORE_PATH=${abs} -> 命中`);
      return mod;
    } catch (e) {
      puppeteerAttempts.push(`PUPPETEER_CORE_PATH=${abs} -> 失败 (${e.code || e.message})`);
    }
  } else {
    puppeteerAttempts.push("PUPPETEER_CORE_PATH=<未设置>");
  }

  /* 3) 从本文件位置逐级向上找 node_modules/puppeteer-core */
  for (let dir = HERE; ; ) {
    const candidate = path.join(dir, "node_modules", "puppeteer-core");
    if (fs.existsSync(candidate)) {
      try {
        const mod = unwrap(await import(pathToFileURL(entryOf(candidate)).href));
        puppeteerAttempts.push(`向上查找: ${candidate} -> 命中`);
        return mod;
      } catch (e) {
        puppeteerAttempts.push(`向上查找: ${candidate} -> 失败 (${e.code || e.message})`);
      }
    } else {
      puppeteerAttempts.push(`向上查找: ${candidate} -> 不存在`);
    }
    const up = path.dirname(dir);
    if (up === dir) break; // 到达文件系统根
    dir = up;
  }

  /* 4) 裸 import: 走 Node 标准解析 (沿本文件位置向上命中全局/父级 node_modules)。
        注意 ESM 解析器不认 NODE_PATH, 所以 NODE_PATH 另走一次 CJS 解析。 */
  try {
    const mod = unwrap(await import("puppeteer-core"));
    puppeteerAttempts.push('裸 import("puppeteer-core") -> 命中');
    return mod;
  } catch (e) {
    puppeteerAttempts.push(`裸 import("puppeteer-core") -> 失败 (${e.code || e.message})`);
  }
  const nodePath = (process.env.NODE_PATH || "").split(path.delimiter).filter(Boolean);
  if (nodePath.length) {
    try {
      const dirs = nodePath.map((d) => path.resolve(d));
      const entry = createRequire(path.join(dirs[0], "__puppeteer_core_resolve__.js"))
        .resolve("puppeteer-core", { paths: dirs });
      const mod = unwrap(await import(pathToFileURL(entry).href));
      puppeteerAttempts.push(`NODE_PATH (${nodePath.join(path.delimiter)}) -> 命中`);
      return mod;
    } catch (e) {
      puppeteerAttempts.push(`NODE_PATH (${nodePath.join(path.delimiter)}) -> 失败 (${e.code || e.message})`);
    }
  } else {
    puppeteerAttempts.push("NODE_PATH=<未设置>");
  }

  throw new Error(
    "找不到 puppeteer-core —— verify.mjs 需要它来驱动 headless 浏览器。\n" +
    "\n" +
    "已按顺序尝试:\n" +
    puppeteerAttempts.map((t) => "  - " + t).join("\n") +
    "\n" +
    "\n" +
    "怎么修 (任选其一; 无需改动本项目的 package.json, 它刻意保持零 npm 依赖):\n" +
    "  a) 装到任意目录, 再指过去 (推荐, 不污染本仓库):\n" +
    "       npm i --prefix C:\\tools\\pptr puppeteer-core\n" +
    "       $env:PUPPETEER_CORE_PATH = \"C:\\tools\\pptr\\node_modules\\puppeteer-core\"\n" +
    "  b) 指向一个已经装了 puppeteer-core 的 package.json:\n" +
    "       $env:PUPPETEER_REQUIRE = \"C:\\path\\to\\other-repo\\package.json\"\n" +
    "  c) 装在本项目的标准位置 (脚本会自动向上查找到):\n" +
    "       cd " + ROOT + " ; npm i --no-save puppeteer-core\n" +
    "  d) 交给 Node 标准解析兜底:\n" +
    "       $env:NODE_PATH = \"C:\\tools\\pptr\\node_modules\"\n"
  );
}

/* ---------------------------------------------------------------------------
 * 浏览器可执行文件发现
 * 优先级: 环境变量 > 本平台常见安装路径 > puppeteer 自带的 executablePath()
 * -------------------------------------------------------------------------*/
function browserCandidates() {
  const out = [];
  /* 1) 环境变量优先 (EDGE_PATH 是历史覆盖口, 保持最高优先级) */
  for (const key of ["EDGE_PATH", "PUPPETEER_EXECUTABLE_PATH", "CHROME_PATH"]) {
    const v = process.env[key];
    if (v) out.push([`环境变量 ${key}`, v]);
  }
  /* 2) 本平台常见安装路径 */
  if (process.platform === "win32") {
    const pf = process.env.ProgramFiles || "C:\\Program Files";
    const pf86 = process.env["ProgramFiles(x86)"] || "C:\\Program Files (x86)";
    const local = process.env.LOCALAPPDATA || path.join(os.homedir(), "AppData", "Local");
    const EDGE = (root, chan) => `${root}\\Microsoft\\Edge${chan ? " " + chan : ""}\\Application\\msedge.exe`;
    const CHROME = (root, chan) => `${root}\\Google\\Chrome${chan ? " " + chan : ""}\\Application\\chrome.exe`;
    for (const p of [
      EDGE(pf86), EDGE(pf), EDGE(local),
      EDGE(pf86, "Beta"), EDGE(pf86, "Dev"), EDGE(pf86, "SxS"),
      CHROME(pf), CHROME(pf86), CHROME(local),
      CHROME(pf, "Beta"), CHROME(pf86, "Beta"),
      `${pf}\\Chromium\\Application\\chrome.exe`,
      `${local}\\Chromium\\Application\\chrome.exe`,
    ]) out.push(["Windows 常见路径", p]);
  } else if (process.platform === "darwin") {
    const APP = (n) => `/Applications/${n}.app/Contents/MacOS/${n}`;
    const HOME_APP = (n) => path.join(os.homedir(), "Applications", `${n}.app`, "Contents", "MacOS", n);
    for (const [n, list] of [
      ["Microsoft Edge", [APP("Microsoft Edge"), HOME_APP("Microsoft Edge")]],
      ["Google Chrome", [APP("Google Chrome"), HOME_APP("Google Chrome")]],
      ["Chromium", [APP("Chromium"), HOME_APP("Chromium")]],
      ["Brave Browser", [APP("Brave Browser"), HOME_APP("Brave Browser")]],
      ["Microsoft Edge Beta", [APP("Microsoft Edge Beta")]],
      ["Google Chrome Canary", [APP("Google Chrome Canary")]],
    ]) for (const p of list) out.push([`macOS ${n}`, p]);
  } else {
    for (const p of [
      "/usr/bin/google-chrome-stable", "/usr/bin/google-chrome", "/opt/google/chrome/chrome",
      "/usr/bin/chromium", "/usr/bin/chromium-browser", "/snap/bin/chromium",
      "/usr/bin/microsoft-edge-stable", "/usr/bin/microsoft-edge", "/snap/bin/microsoft-edge",
      "/usr/bin/brave-browser",
      "/var/lib/flatpak/exports/bin/com.microsoft.Edge",
      "/var/lib/flatpak/exports/bin/com.google.Chrome",
    ]) out.push(["Linux 常见路径", p]);
  }
  return out;
}

function resolveBrowserExecutable(pptr) {
  const tried = [];
  const isFile = (p) => { try { return fs.statSync(p).isFile(); } catch { return false; } };
  for (const [src, p] of browserCandidates()) {
    if (isFile(p)) return { path: p, source: src };
    tried.push(`${src}: ${p} -> 不存在`);
  }
  /* 3) 最后交给 puppeteer 自己解析 (装了浏览器的 puppeteer 可返回 executablePath) */
  if (typeof (pptr && pptr.executablePath) === "function") {
    try {
      const p = pptr.executablePath();
      if (p && isFile(p)) return { path: p, source: "puppeteer.executablePath()" };
      tried.push(`puppeteer.executablePath(): ${p || "<空>"} -> 不可用`);
    } catch (e) {
      tried.push(`puppeteer.executablePath() -> 抛错 (${String(e.message).split("\n")[0]})`);
    }
  } else {
    tried.push("puppeteer.executablePath(): 当前 puppeteer 没有该 API");
  }
  throw new Error(
    "找不到浏览器可执行文件 —— puppeteer 已加载, 但没有可用的 Chrome/Edge/Chromium。\n" +
    "\n" +
    "已按顺序尝试:\n" +
    tried.map((t) => "  - " + t).join("\n") +
    "\n" +
    "\n" +
    "怎么修 (任选其一):\n" +
    "  a) 直接指定本机浏览器 (三端通用):\n" +
    "       $env:PUPPETEER_EXECUTABLE_PATH = \"<浏览器可执行文件的绝对路径>\"\n" +
    "       Windows 常见: C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe\n" +
    "                      C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe\n" +
    "       macOS  常见: /Applications/Google Chrome.app/Contents/MacOS/Google Chrome\n" +
    "       Linux  常见: /usr/bin/chromium\n" +
    "  b) 装一个浏览器 (装完重跑本脚本, 探测会自动命中):\n" +
    "       winget install --id Microsoft.Edge        (Windows)\n" +
    "       brew install --cask google-chrome         (macOS)\n" +
    "       sudo apt install -y chromium               (Debian/Ubuntu)\n" +
    "  c) 让 puppeteer 自带浏览器 (puppeteer-core 不含浏览器, 需另装 @puppeteer/browsers):\n" +
    "       npx @puppeteer/browsers install chrome@stable\n" +
    "       然后把 PUPPETEER_EXECUTABLE_PATH 指到它下载的 chrome 可执行文件上\n"
  );
}

let puppeteer, browserExe;
try {
  puppeteer = await loadPuppeteer();
  browserExe = resolveBrowserExecutable(puppeteer);
} catch (e) {
  /* 依赖缺失属于「环境没配好」, 打印可操作文案即可, 不必甩用户一脸堆栈 */
  console.error("\n" + e.message + "\n");
  process.exit(1);
}

/* 视口元数据: 从 page 实际设置读取, 不写死字面量。
   否则改了 setViewport 的尺寸, 两份不同视口的快照仍会比出「等价」。 */
function formatViewport(vp) {
  if (!vp) return "unknown";
  const dsf = vp.deviceScaleFactor == null ? 1 : vp.deviceScaleFactor;
  return `${vp.width}x${vp.height}@${dsf}`;
}

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
  console.error("[verify] puppeteer-core 来源:", puppeteerAttempts.filter((t) => t.includes("命中")).join(" | ") || "(见失败清单)");
  console.error("[verify] 浏览器:", browserExe.path, "(" + browserExe.source + ")");
  const browser = await puppeteer.launch({
    executablePath: browserExe.path,
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

    const dump = { url: URL_BASE, viewport: formatViewport(page.viewport()), stages: {}, console: [] };
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








