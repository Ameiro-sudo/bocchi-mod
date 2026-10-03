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
 *   node dev/verify.mjs --out dev/dumps/base.json [--shots dev/shots]
 *   node dev/verify.mjs --url http://127.0.0.1:8833/   # 复用已在跑的服务
 *
 * 默认自起一个临时端口的静态服务指向仓库根, 无需先手工 npm run serve;
 * 传 --url 则改用外部地址 (自起服务被跳过)。
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
import http from "node:http";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.dirname(HERE);

function arg(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 ? process.argv[i + 1] : def;
}
/* null = 未指定, 主流程会自起静态服务 */
let URL_BASE = arg("url", null);
/* 默认写到 head.json 而不是 baseline.json: 裸跑一次就把基线覆盖掉, 于是「改坏
   了」和「重新定义了基线」变成同一个动作, 门禁就再也拦不住任何东西。重建基线
   必须是显式动作, 只有 npm run verify:baseline 才干这件事。 */
const OUT = arg("out", path.join(ROOT, "dev", "dumps", "head.json"));
const SHOTS = arg("shots", null);

/* 快照结构版本: 变更采集内容时 +1。compare 会拿它判「基线是不是同一代产物」,
   避免拿旧基线比新快照得到一堆无意义的键差异。 */
const SCHEMA_VERSION = 3;

/* ---------------------------------------------------------------------------
 * 零依赖静态服务: 让 verify.mjs 成为单命令门禁 (否则要先手工 npm run serve,
 * 且起错目录/端口被占都会变成「环境问题」而非「代码回归」)。
 * -------------------------------------------------------------------------*/
const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".gif": "image/gif",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".otf": "font/otf",
};

function startStaticServer(root) {
  return new Promise((resolve, reject) => {
    const server = http.createServer((req, res) => {
      let pathname;
      try {
        pathname = decodeURIComponent(new URL(req.url, "http://127.0.0.1").pathname);
      } catch {
        res.writeHead(400); return res.end("bad url");
      }
      if (pathname.endsWith("/")) pathname += "index.html";
      const file = path.join(root, path.normalize(pathname));
      // 归一化后必须仍落在 root 内, 否则 403 —— 防目录穿越
      if (file !== root && !file.startsWith(root + path.sep)) {
        res.writeHead(403); return res.end("forbidden");
      }
      fs.readFile(file, (err, buf) => {
        if (err) { res.writeHead(404, { "content-type": "text/plain; charset=utf-8" }); return res.end("not found: " + pathname); }
        res.writeHead(200, {
          "content-type": MIME[path.extname(file).toLowerCase()] || "application/octet-stream",
          "cache-control": "no-store",
        });
        res.end(buf);
      });
    });
    server.on("error", reject);
    server.listen(0, "127.0.0.1", () => resolve({ server, url: `http://127.0.0.1:${server.address().port}/` }));
  });
}

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
/* 每条排除都必须写明理由 —— 排除表本身就是「已知不稳定」的契约, 无理由的排除
   等于把真回归藏起来。EXCLUDE_VERSION 会写进快照, 改动本表时顺手 +1,
   compare 见到基线与新快照版本不同会直接判不兼容, 避免拿旧基线误判等价。 */
const EXCLUDE_VERSION = 2;
const EXCLUDE = [
  // 进度条宽度由 splash.js 的常驻 rAF 定时器推进, 与采样时刻强相关。
  ["splash", "progressFill", "width"],
  // loadingIcon 的 backgroundPosition/backgroundImage 同样由该定时器改写。
  ["splash", "loadingIcon", "backgroundPosition|backgroundImage"],
  // tapText 在入场后 1s 淡出 (main.js 点击淡出链路), 采样落在淡出窗口内。
  ["splash", "tapText", "opacity"],
  // 选中态 transform 由拖拽/键盘微调写入, transform 是旋转矩阵, 序列化后
  // 浮点尾数不可重现; 其余几何属性仍在采集面内。
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
    /* 排除表同时作用于 styles 与 rect: 定时器驱动的宽度改的是行内 width,
       只排 styles.width 而留着 rect.width 会让同一份代码两次采集就不等价
       (实测 progressFill.rect.width 在 442~452 间抖动)。 */
    for (const k of ["left", "top", "width", "height"]) {
      if (excluded(el.id, k)) delete rec.rect[k];
      else rec.rect[k] = Math.round(rec.rect[k] * 1000) / 1000;
    }
    const tr = el.style.transform;
    if (tr && !excluded(el.id, "transform")) rec.attrs.inlineTransform = tr;
    const cs = getComputedStyle(el);
    for (const p of PROPS) {
      if (excluded(el.id, p)) continue;
      rec.styles[p] = cs[p];
    }
    /* baseRot / naturalW / naturalH 同样走排除表: baseRot 由入场动画写入,
       图片固有尺寸依赖解码时机, 都可能造成采样时刻相关的噪声。 */
    if (el.dataset.baseRot != null && !excluded(el.id, "baseRot")) rec.attrs.baseRot = el.dataset.baseRot;
    if (el.tagName === "IMG" && !excluded(el.id, "naturalW")) {
      rec.attrs.naturalW = el.naturalWidth;
      rec.attrs.naturalH = el.naturalHeight;
    }
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

/* 工具外壳 (header + #controls 面板) 的采集。
   此前采集面只有三个舞台容器, 于是 panels.js / index.html / css/ui.css 怎么改门禁
   都是绿的 —— 只量三分之一的尺子比没有尺子更危险, 因为它给的是假保证。
   这里把外壳按「结构路径」摊平成有序数组: 路径由子节点下标组成 (0.2.1), 不含
   class 名, 所以改类名不会炸快照; 但增删控件、改滑杆 min/max、改分区标题或折叠
   状态一定会被抓出来。排除表用 stage="chrome" 命名空间。 */
function dumpChrome(EXCLUDE) {
  const excluded = (id, prop) =>
    EXCLUDE.some(([s, e, src]) => s === "chrome" && e === id && new RegExp(src).test(prop));
  const round = (n) => Math.round(n * 1000) / 1000;
  const norm = (s) => (s || "").replace(/\s+/g, " ").trim();

  function scan(root) {
    const nodes = [];
    const r = root.getBoundingClientRect();
    const visit = (el, path) => {
      const key = el.id || path;
      const sig = { path, tag: el.tagName };
      if (el.id && !excluded(key, "id")) sig.id = el.id;
      const cls = el.getAttribute("class");
      if (cls && !excluded(key, "class")) sig.class = cls;
      /* 只收元素自身的直接文本节点, 不含后代 —— 后代会在各自条目里各记一次 */
      let text = "";
      for (const n of el.childNodes) if (n.nodeType === 3) text += n.nodeValue;
      text = norm(text);
      /* jsonPreview 的完整内容已由 stages.*.designJSON 逐项比过, 这里只留长度
         指纹, 免得同一份 JSON 在快照里出现两遍并撑爆 diff 输出 */
      if (el.id === "jsonPreview") text = "<designJSON len=" + text.length + ">";
      if (text && !excluded(key, "text")) sig.text = text.slice(0, 120);
      if (el.tagName === "INPUT" || el.tagName === "SELECT" || el.tagName === "TEXTAREA") {
        const attrs = {};
        for (const k of ["type", "min", "max", "step", "placeholder", "accept"]) {
          const v = el.getAttribute(k);
          if (v !== null && !excluded(key, k)) attrs[k] = v;
        }
        attrs.value = el.value;
        sig.attrs = attrs;
      }
      if (el.tagName === "BUTTON") {
        sig.attrs = { type: el.getAttribute("type"), disabled: el.disabled };
        if (el.classList.contains("on") || el.classList.contains("active")) sig.state = "on";
      }
      if (getComputedStyle(el).display === "none") sig.hidden = true;
      nodes.push(sig);
      for (let i = 0; i < el.children.length; i++) visit(el.children[i], path + "." + i);
    };
    for (let i = 0; i < root.children.length; i++) visit(root.children[i], String(i));
    return { rect: { x: round(r.x), y: round(r.y), w: round(r.width), h: round(r.height) }, nodes };
  }

  const out = {};
  const header = document.querySelector("header");
  const controls = document.getElementById("controls");
  if (header) out.header = scan(header);
  if (controls) out.controls = scan(controls);
  const sel = document.getElementById("selBox");
  if (sel) {
    const r = sel.getBoundingClientRect();
    out.selBox = {
      display: sel.style.display || getComputedStyle(sel).display,
      handles: [...sel.querySelectorAll("[data-h]")].map((h) => h.dataset.h).join(","),
      rect: { w: round(r.width), h: round(r.height) },
    };
  }
  return out;
}

(async () => {
  console.error("[verify] puppeteer-core 来源:", puppeteerAttempts.filter((t) => t.includes("命中")).join(" | ") || "(见失败清单)");
  console.error("[verify] 浏览器:", browserExe.path, "(" + browserExe.source + ")");
  let srv = null;
  let browser = null;
  try {
    if (!URL_BASE) {
      srv = await startStaticServer(ROOT);
      URL_BASE = srv.url;
      console.error("[verify] 自起静态服务:", srv.url, "(临时端口, 等效 npm run serve)");
    }
    browser = await puppeteer.launch({
      executablePath: browserExe.path,
      headless: true,
      args: ["--no-first-run", "--disable-sync", "--disable-gpu", "--font-render-hinting=none"],
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1600, height: 900, deviceScaleFactor: 1 });

    const consoleMsgs = [];
    // 先原样收着: 内置资源探针逐个 HEAD 一次, 已知缺失的 meiryo-bold.ttf 必然
    // 404, Chrome 会记成一条 console error。等探针把「它到底请求了哪些 URL」交回来
    // 之后, 再按 URL 精确剔除这几条 —— 凭 404 文本或路径正则去猜, 迟早会连带
    // 吃掉一条真的错误, 而多一条无人认领的噪声只会让人不再看这行输出。
    page.on("console", (m) => {
      if (!["error", "warning"].includes(m.type())) return;
      consoleMsgs.push({ type: m.type(), text: m.text(), url: (m.location() && m.location().url) || "" });
    });
    page.on("pageerror", (e) => consoleMsgs.push({ type: "pageerror", text: e.message, url: "" }));

    await page.goto(URL_BASE, { waitUntil: "networkidle2", timeout: 30000 });
    await page.evaluate(() => document.fonts.ready);
    await new Promise(r => setTimeout(r, 600)); // boot 尾部的 fonts.ready 重排

    /* 只记 pathname: 端口是临时分配的, 写进快照会让每次采集都「不等价」 */
    const dump = {
      schemaVersion: SCHEMA_VERSION,
      excludeVersion: EXCLUDE_VERSION,
      page: new URL(URL_BASE).pathname,
      viewport: formatViewport(page.viewport()),
      stages: {},
      console: [],
    };
    for (const s of STAGES) {
      await page.evaluate((tab) => document.getElementById(tab).click(), s.tab);
      await new Promise(r => setTimeout(r, 2400)); // 入场动画 (700ms + 40ms 级联) 完全落定
      dump.stages[s.name] = await page.evaluate(dumpStage, s.name, EXCLUDE);
      if (SHOTS) {
        fs.mkdirSync(SHOTS, { recursive: true });
        await page.screenshot({ path: path.join(SHOTS, `${s.name}.png`) });
      }
    }

    /* 工具外壳在烟测之前采: 烟测会改选中态与滑杆值, 采在之后就掺进了交互噪声。
       先切回默认舞台再采, 这样外壳快照不依赖 STAGES 数组的末项是哪一个。 */
    await page.evaluate(() => document.getElementById("swMisayos").click());
    await new Promise(r => setTimeout(r, 900));
    // 内置资源可达性探针是异步的 (HEAD 请求)。不等它落定就采快照, 差异会随机
    // 出现又随机消失 —— 那是最坏的一种门禁故障: 它只在别人机器上红。
    dump.assetProbe = await page.evaluate(() => globalThis.__bocchi.assetsReady);
    // 按路径精确剔除探针自己造成的 404 (见 consoleMsgs 处的注释)。
    // 比对的是 pathname 而非完整 URL: 服务跑在随机端口上, 拿完整 URL 比对会让
    // 每条 console 记录的过滤都落空。
    const probedPaths = new Set(dump.assetProbe.probed || []);
    const consoleFiltered = consoleMsgs.filter(m => {
      if (!m.url || !probedPaths.size) return true;
      try { return !probedPaths.has(new URL(m.url).pathname.replace(/^\//, "")); }
      catch { return true; }
    });
    dump.chrome = await page.evaluate(dumpChrome, EXCLUDE);

    /* 交互烟测: 选中立绘 -> 方向键微调 -> Esc 取消; 导出按钮存在。
     * mousedown 之后必须补一发 mouseup: 真实用户一定会松手, 而拖拽手势靠
     * beginGesture/endGesture 成对开关一个全局静音区 —— 只按不松, 静音区就漏了,
     * 之后整个撤销栈静默失效, 后面所有行为门禁都会跟着失真。 */
    const smoke = await page.evaluate(() => {
      const res = {};
      const tachie = document.getElementById("mTachie");
      tachie.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, clientX: 700, clientY: 300 }));
      window.dispatchEvent(new MouseEvent("mouseup", { bubbles: true }));
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowLeft" }));
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowRight", shiftKey: true }));
      window.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
      res.selBoxHiddenAfterEsc = document.getElementById("selBox").style.display === "none";
      res.exportBtnExists = !!document.getElementById("exportBtn");
      res.rangeInputCount = document.querySelectorAll(".controls input[type=range]").length;
      return res;
    });
    await new Promise(r => setTimeout(r, 500)); // scheduleSave debounce 250ms 落盘

    /* UI 不变量探针: 快照采不到的东西, 只能写成断言。
     * 快照的采集面是 header + #controls, 舞台 DOM 与状态栏文案都在采集面之外 ——
     * 「#misayosStage 被打上 data-sel 导致整块画布 hover 描粉色虚线」和「Esc 之后
     * 状态栏永久变空」这两类回归, compare.mjs 报 0 处差异, 页面照坏。 */
    const uiProbe = await page.evaluate(() => {
      const failures = [];
      const stage = document.getElementById("misayosStage");
      // stage.css 的 [data-sel]:hover 会给整块 1280x720 画布描一圈虚线
      if (stage.hasAttribute("data-sel")) failures.push("#misayosStage 不应带 data-sel (整块画布会被 hover 描边)");
      // SEL_HIT 里这四个都映射到 "title", 可点却没 hover 反馈
      for (const id of ["mBocchi", "mRock", "mBoxGotoh", "mBoxGirl"]) {
        const el = document.getElementById(id);
        if (!el) failures.push("#" + id + " 不存在");
        else if (!el.hasAttribute("data-sel")) failures.push("#" + id + " 可点击但没有 data-sel (无 hover 反馈)");
      }
      // 撤销/重做按钮可用态由 history 的栈变化通知驱动。走到这里时, 上面的烟测已经
      // 做过方向键微调 => 撤销栈非空、重做栈为空。这条断言抓的是「notify 没接上」
      // 或者「只接了 push 没接 undo」这类接错线的错。
      const u = document.getElementById("btnUndo"), r = document.getElementById("btnRedo");
      if (u.disabled) failures.push("烟测已压入撤销记录, 但 btnUndo 仍是 disabled (栈变化通知没接上)");
      if (!r.disabled) failures.push("重做栈为空, 但 btnRedo 未 disabled");
      // 资源路径必须真的可编辑。Design.java 与 design.js 早就支持 namespace:path,
      // 但面板上曾经是一个只读 span —— 能力齐备, 界面没给入口。
      const pn = document.getElementById("rn_textures_bocchi");
      if (!pn) failures.push("#rn_textures_bocchi 不存在");
      else if (pn.tagName !== "INPUT") failures.push("资源路径不是可编辑输入框 (tag=" + pn.tagName + ")");
      else if (!/^client\/textures\//.test(pn.value)) failures.push("资源路径初值不是内置默认路径: " + pn.value);

      // Esc 取消选中后状态栏必须回到舞台基线文案, 而不是空串
      const st = (document.getElementById("status").textContent || "").trim();
      if (!st) failures.push("Esc 取消选中后状态栏为空 (clearSel 未恢复舞台基线文案)");
      if (!/misayos/.test(st)) failures.push("状态栏文案不含当前舞台名: " + JSON.stringify(st.slice(0, 40)));
      return { ok: failures.length === 0, failures, statusLen: st.length };
    });
    if (!uiProbe.ok) throw new Error("UI 不变量探针失败:\n  - " + uiProbe.failures.join("\n  - "));

    /* 内置资源可达性: client/fonts/meiryo-bold.ttf 是已知缺失项 (9.3MB, 没随工具
     * 分发)。面板必须把它标出来, 否则用户以为它会进包, 实际导出时被静默跳过,
     * 游戏端回退默认字体、日文块字形突变, 而工具全程不吭声。
     * 这条断言同时也是对「探针没跑完就采快照」的防御: 若 assetProbe 是空的,
     * 说明上面那句 await 没等住。 */
    const missingKeys = Object.keys(dump.assetProbe.checked || {}).filter(k => dump.assetProbe.checked[k] === false);
    if (!missingKeys.length)
      throw new Error("内置资源探针没跑出任何结果 (assetsReady 未落定?), 快照会采到半成品状态");
    if (!missingKeys.includes("fonts/meiryo-bold"))
      throw new Error("内置资源探针结果与已知事实矛盾: 应缺失的 fonts/meiryo-bold.ttf 反而可达");
    const markedMissing = await page.evaluate(
      () => Array.from(document.querySelectorAll(".res-row .r-name.missing")).map(el => el.id));
    if (!markedMissing.length)
      throw new Error("有内置资源缺失, 但面板上一行都没标 (missing 类没落地): " + missingKeys.join(", "));
    if (!markedMissing.includes("rn_fonts_meiryo-bold"))
      throw new Error("meiryo-bold.ttf 未被标记为缺失, 实际标记了: " + markedMissing.join(", "));

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

    /* ---- 撤销栈探针 (行为门禁, 不是快照) ----
     * 快照里不含撤销栈, 于是「改完能不能撤回来」在门禁上是完全失明的: 把
     * pushHistory 调用删掉、把回放闭包写错、把基准变量忘了刷新, compare 全绿。
     * 这里走用户真实路径 (派发 input/change), 只读 __bocchi 做断言, 因此它不是
     * 「把当前行为拍下来」而是「要求行为正确」—— 坏掉就 exit 1, 与基线无关。
     * 跑在所有快照采集之后: 它会改模型, 不能污染外壳快照。 */
    const historyProbe = await page.evaluate(() => {
      const B = globalThis.__bocchi;
      const fails = [];
      const ok = (cond, msg) => { if (!cond) fails.push(msg); };
      const depth = () => B.historyStats().undo;
      /* 先把栈撤空, 再取基准。上面 sliderProbe 改过面板宽度并留了一条记录, 若不先
         撤掉, 后面「撤到底应当回到本次探针开始时的值」就会因为它而对不上; 同键
         700ms 的合并窗也会把两次入栈并成一条。undo() 顺手清掉合并标记。 */
      for (let i = 0; i < 200 && B.historyStats().undo > 0; i++) B.undo();
      const setVal = (el, v) => {
        const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
        Object.getOwnPropertyDescriptor(proto, "value").set.call(el, v);
        el.dispatchEvent(new Event("input", { bubbles: true }));
      };
      const blur = (el) => el.dispatchEvent(new Event("change", { bubbles: true }));
      const cssVar = (k) => document.documentElement.style.getPropertyValue(k).trim();

      /* 1) 滑杆: input 事件应当恰好入栈一条, undo 回原值, redo 复原 */
      const sliderRow = [...document.querySelectorAll(".slider-row")]
        .find(r => r.querySelector("label") && r.querySelector("label").textContent.includes("面板宽度"));
      const slider = sliderRow && sliderRow.querySelector("input[type=range]");
      const sOrig = slider ? slider.value : null;
      const d0 = depth();
      if (!slider) fails.push("滑杆探针: 找不到「面板宽度」行");
      else {
        setVal(slider, slider.value === slider.max ? slider.min : slider.max);
        ok(depth() === d0 + 1, `滑杆: 拖动应入栈 1 条, 实际 ${depth() - d0} 条`);
        B.undo();
        ok(slider.value === sOrig, `滑杆: undo 未回到原值 (${slider.value} != ${sOrig})`);
        B.redo();
        ok(slider.value !== sOrig, "滑杆: redo 未复原");
        B.undo();
      }

      /* 2) 文本: input 只改模型不入栈, change 才入栈; 且回放后撤销基准必须跟着走 */
      const tInput = document.querySelector("#sec-texts .text-row input[type=text]");
      const tOrig = tInput ? tInput.value : null;
      const td0 = depth();
      if (!tInput) fails.push("文本探针: #sec-texts 下找不到文本输入框");
      else {
        setVal(tInput, tOrig + "-探针");
        ok(depth() === td0, `文本: input 事件不应入栈, 实际多了 ${depth() - td0} 条`);
        blur(tInput);
        ok(depth() === td0 + 1, `文本: change 应入栈 1 条, 实际多了 ${depth() - td0} 条`);
        B.undo();
        ok(tInput.value === tOrig, `文本: undo 未回退 (${tInput.value} != ${tOrig})`);
        // 基准回归的核心断言: 撤销后基准若仍停在被撤掉的值上, 用户把同一个值再输
        // 一遍就会凭空多出一条撤销条目 —— 撤销次数开始凭空增长。
        const after = depth();
        blur(tInput);
        ok(depth() === after, `文本: 撤销后再失焦生成了 ${depth() - after} 条伪造历史条目`);
      }

      /* 3) 预览配色: 改颜色此前零入栈, 撤不回来; 且 CSS 变量与状态表必须同步回退 */
      const cInput = document.querySelector("#sec-colors-preview input[type=color]");
      const cBefore = JSON.parse(JSON.stringify(B.state.PREVIEW_COLORS));
      const cd0 = depth();
      /* 先探一下撤销栈本身还活着没有: 拖拽手势靠一个全局静音区成对开关, 一旦
         beginGesture 没等到 endGesture, 之后所有 push 都被无声吞掉, 而症状是
         「Ctrl+Z 忽然什么都不干」—— 没有任何报错。 */
      B.pushHistory({ label: "探针存活检查", undo: () => {}, redo: () => {} });
      if (depth() !== cd0 + 1) {
        fails.push("撤销栈已失效: 直接 push 也被吞, 怀疑手势静音区泄漏");
        B.redo();
      } else {
        B.undo();
      }
      if (!cInput) fails.push("预览配色探针: 找不到取色器");
      else {
        setVal(cInput, cInput.value === "#ff0000" ? "#00ff00" : "#ff0000");
        ok(depth() === cd0 + 1, `预览配色: 应入栈 1 条, 实际多了 ${depth() - cd0} 条 (控件值 ${cInput.value}, 栈 ${cd0}->${depth()})`);
        const key = Object.keys(B.state.PREVIEW_COLORS).find(k => B.state.PREVIEW_COLORS[k] !== cBefore[k]);
        ok(!!key, "预览配色: 状态表没有跟着变");
        if (key) {
          ok(cssVar(key) === B.state.PREVIEW_COLORS[key],
            `预览配色: CSS 变量 ${key} 未同步 (${cssVar(key)} != ${B.state.PREVIEW_COLORS[key]})`);
          B.undo();
          ok(B.state.PREVIEW_COLORS[key] === cBefore[key], `预览配色: undo 未还原状态表 (${key})`);
          ok(cssVar(key) === cBefore[key], `预览配色: undo 未还原 CSS 变量 (${key})`);
          B.redo();
          B.undo();
        }
      }

      /* 4) 撤到底: 必须能撤空, 且撤空后三个模型都回到初始值 */
      let guard = 200;
      while (B.historyStats().undo > 0 && guard-- > 0) B.undo();
      ok(depth() === 0, `撤销栈: 撤到底后仍有 ${depth()} 条`);
      ok(slider && slider.value === sOrig, "撤销栈: 撤空后滑杆未回到初始值");
      ok(tInput && tInput.value === tOrig, "撤销栈: 撤空后文本未回到初始值");
      for (const k of Object.keys(cBefore)) {
        ok(B.state.PREVIEW_COLORS[k] === cBefore[k], `撤销栈: 撤空后预览色 ${k} 未回到初始值`);
      }
      return { ok: fails.length === 0, fails, finalDepth: B.historyStats() };
    });
    if (!historyProbe || !historyProbe.ok) {
      const fails = historyProbe ? historyProbe.fails : ["探针未执行 (缺少 __bocchi 调试出口)"];
      console.error("\n撤销栈探针失败:");
      for (const f of fails) console.error("  - " + f);
      throw new Error("撤销栈探针未通过");
    }

    dump.console = consoleFiltered.map(m => `[${m.type}] ${m.text}`);
    fs.mkdirSync(path.dirname(OUT), { recursive: true });
    fs.writeFileSync(OUT, JSON.stringify(dump, null, 1));
    console.log("dumped:", OUT);
    console.log("console warn/error:", consoleFiltered.length ? consoleFiltered.map(m => `[${m.type}] ${m.text}`).join(" | ") : "(none)");
    console.log("smoke:", JSON.stringify(dump.smoke));
    console.log("uiProbe: ok");
    console.log("sliderProbe:", JSON.stringify(sliderProbe));
    console.log("historyProbe: ok");
  } finally {
    if (browser) await browser.close();
    if (srv) srv.server.close();
  }
})().catch(e => { console.error(e); process.exit(1); });








