/* ============================================================================
 * design.js - design.json 数据模型 + 状态持久化
 *
 * S = 当前编辑态: { textures/svgs/fonts: {key: {path, blob}}, colors: {key: path},
 *                   menu: {theme}, texts: {key: 文案}, extra: 未知键保留区 }
 * 上传的资源 (blob) 仅存在内存; 布局/文本/配色等偏好存 localStorage。
 * ==========================================================================*/
import { state } from "./core.js";
import * as facts from "./facts.js";

export const DEFAULT_DESIGN = {
  textures: {
    bocchi: "client/textures/bocchi.png", gotoh: "client/textures/gotoh.png",
    gotoh_image_1: "client/textures/gotoh_image_1.png", gotoh_image_2: "client/textures/gotoh_image_2.png",
    bocchi_loading: "client/textures/bocchi_loading.png", logo: "client/textures/logo.png",
  },
  svgs: {
    single: "client/svgs/single.svg", multi: "client/svgs/multi.svg", option: "client/svgs/option.svg",
    lang: "client/svgs/lang.svg", quit: "client/svgs/quit.svg", theme: "client/svgs/theme.svg",
  },
  fonts: {
    "Radikal-Black": "client/fonts/radikal-black.ttf", "Radikal-Regular": "client/fonts/radikal-regular.ttf",
    "meiryo-bold": "client/fonts/meiryo-bold.ttf",
    "SourceHanSansSC-Light": "client/fonts/sourcehansanssc-light.ttf",
    "SourceHanSansSC-Regular": "client/fonts/sourcehansanssc-regular.ttf",
    "SourceHanSansSC-Heavy": "client/fonts/sourcehansanssc-heavy.ttf",
    "SourceHanSansSC-Normal": "client/fonts/sourcehansanssc-normal.ttf",
    "SourceHanSansSC-Bold": "client/fonts/sourcehansanssc-bold.ttf",
  },
  colors: {
    vinyl_edge: "#050505", vinyl_base: "#1A1A1A", vinyl_shine_1: "#33FFFFFF", vinyl_shine_2: "#1AFFFFFF",
    vinyl_shine_3: "#001A1A1A", vinyl_groove: "#1FFFFFFF", vinyl_label: "#981A1A1A",
  },
  menu: { theme: "misayos" },
};

const S = { textures: {}, svgs: {}, fonts: {}, colors: {}, menu: { theme: "misayos" } };
for (const sec of ["textures", "svgs", "fonts"]) {
  for (const [k, v] of Object.entries(DEFAULT_DESIGN[sec])) S[sec][k] = { path: v, blob: null };
}
S.colors = { ...DEFAULT_DESIGN.colors };
// H2: 导入时未知键/未知 section 原样保留, 导出时合并回去 (与 Java 端纯累加覆盖语义一致)
// H3: extra 用 null 原型容器, 杜绝 __proto__/constructor 键原型污染
S.extra = Object.create(null);

/* ---------- H3: 原型污染防护 ----------
 * design.json / zip / localStorage 都是外部输入, 统一经 cleanCopy 过滤危险键后再入模 */
export const UNSAFE_KEYS = new Set(["__proto__", "constructor", "prototype"]);
export function cleanCopy(src) {
  const out = Object.create(null);
  if (!src || typeof src !== "object") return out;
  for (const [k, v] of Object.entries(src)) {
    if (!UNSAFE_KEYS.has(k)) out[k] = v;
  }
  return out;
}

const localAsset = (p) => "assets/" + p.replace(/^[a-z0-9_.-]+:/, "").replace(/^client\//, "");
const usedPath = (sec, k) => (S[sec][k].blob ? blobUrl(S[sec][k]) : localAsset(S[sec][k].path));

/* ---------- blob ObjectURL 缓存 (M1: 不再每次调用新建, 替换时 revoke) ---------- */
const blobUrl = (entry) => {
  if (entry._url) return entry._url;
  return (entry._url = URL.createObjectURL(entry.blob));
};
function revokeBlobUrl(entry) {
  if (entry._url) { URL.revokeObjectURL(entry._url); entry._url = null; }
}
/** 替换/清空资源 blob: 先 revoke 旧 URL, 再写新值 */
function setBlob(sec, k, blob) {
  const entry = S[sec][k];
  if (entry && entry._url) revokeBlobUrl(entry);
  if (entry) entry.blob = blob || null;
}

/* ---------- design.json 路径解析 (H1: 保留命名空间) ---------- */
const NS_RE = /^([a-z0-9_.-]+):(.+)$/;
function splitPath(p) {
  const m = NS_RE.exec(p || "");
  return m ? { ns: m[1], rest: m[2] } : { ns: "minecraft", rest: p };
}
const zipEntry = (p) => "assets/" + splitPath(p).ns + "/" + splitPath(p).rest;

/* ---------- 颜色 ---------- */
function hexToCss(hex) {
  const m = /^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.exec((hex || "").trim());
  if (!m) return null;
  let h = m[1];
  if (h.length === 6) return "#" + h;
  const a = parseInt(h.slice(0, 2), 16) / 255, rgb = h.slice(2);
  return `rgba(${parseInt(rgb.slice(0, 2), 16)},${parseInt(rgb.slice(2, 4), 16)},${parseInt(rgb.slice(4, 6), 16)},${a.toFixed(3)})`;
}

/* ---------- 可编辑文本 (导出 design.json 的 texts 段) ----------
 * 注: 不换行空格用 \u00A0 字符而非 &nbsp; 实体 - applyText 会对文本做 HTML 转义,
 * 实体写法会被原样显示成 "&nbsp;" 字面量。导出时经 textsForExport 转纯文本 */
export const DEFAULT_TEXTS = {
  mBocchi: "BOCCHI", mRock: "THE ROCK!",
  mBoxGotoh: "Gotoh Hitori", mBoxGirl: "A reclusive girl",
  mPhobia: "SOCIAL\u00A0\u00A0PHOBIA",
  mInfo: 'Goto, nicknamed "Little Solitude",<br>is a girl who always starts her speech with "Ah..."<br>and is extremely accepting and introverted',
  pTitle: "BOCCHI", pVer: "1.0", pBranch: '"ALPHA"',
  pCopy1: "Bocchi Client\u00A0\u00A0\u00A0\u00A0Version - 1.0", pCopy2: "@COPYRIGHT MISAYO",
  pHitoriTop: "HITORI", pHitoriBottom: "HITORI", pGoto1: "GOTO", pGoto2: "GOTO",
  pJName: "後藤 ひとり", pJKana: "ご\u00A0\u00A0\u00A0とう",
  pAliasText: "ギターヒーロー",
  pAdd1: "FEBRUARY 21", pAdd2: "50 kg & 156 cm", pAdd3: "Aqua eye",
  // 设置界面 (SettingsPanel.java:135 / :277) 消费, 但舞台上没有对应元素 —— 无法预览
  sTitle: "CFGS", sDone: "DONE",
};
// 单一数据源: state.TEXTS 与导出用的 S.texts 指向同一对象
S.texts = { ...DEFAULT_TEXTS };
state.TEXTS = S.texts;

const htmlToPlain = (s) => String(s).replace(/\u00A0/g, " ");
/** 预览文案 -> design.json texts 段纯文本 (\u00A0 转空格, mInfo 按 <br> 拆三行) */
export function textsForExport() {
  const o = {};
  for (const [k, v] of Object.entries(S.texts)) {
    if (UNSAFE_KEYS.has(k)) continue;
    if (k === "mInfo") {
      const lines = String(v).split(/\s*<br\s*\/?>\s*/i);
      for (let i = 0; i < 3; i++) o["mInfoLine" + (i + 1)] = htmlToPlain(lines[i] != null ? lines[i] : "");
    } else o[k] = htmlToPlain(v);
  }
  return o;
}

/* ---------- design.json 生成 (导出/预览) ---------- */
/* ---------- layout 段 (misayos 布局微调 -> design.json) ----------
 * 面板上 16 个滑杆里有 7 个是纯预览 (Java 端无对应公式: tachieRot/tachieOp/
 * titleSize/titleX/titleY/panelW/panelX), 其余 9 个在 Java 端有确切公式, 这里写进
 * design.json 的 layout 段。7 个 webOnly 滑杆刻意不导出 —— 导出一个游戏端不会读的
 * 键比不导出更糟: 用户会以为改了生效了。
 *
 * 存的是**归一化比例**而非像素: Java 的 FrameContext 固定 scaledWidth=480 /
 * scaledHeight=270, 预览是 1280x720, 同一组公式在两帧下成比例, 所以 px/轴长 就是
 * Java 那边的系数本身。
 *
 * 两类键, 语义不同:
 *   - 尺寸类 (无 offset 标记): 绝对比例, 缺省值 = mod 内置常量。
 *   - 偏移类 (offset: true): 相对**内置位置**的偏移比例, 缺省 0。
 * 偏移类用 0 做缺省是有意的 —— Java 那边不必知道基准值, 少一次我把常量抄错的
 * 机会, 也让 check-layout.py 的原有表达式原样保留。
 *
 * 键名直接就是 Design.num("layout.<键>", d) 的查找路径, 与 Design.merge() 写入
 * VALUES 的格式一致, 所以 Java 端不需要任何遍历改动, 只需要一个取值函数。
 */
export const LAYOUT_SPEC = [
  { ov: "block1", base: "misayos.block1Size", json: { "misayos.block1SizeW": "w", "misayos.block1SizeH": "h" } },
  { ov: "blockX", json: { "misayos.block1XOffset": "w" }, offset: true },
  { ov: "blockY", json: { "misayos.block1YOffset": "h" }, offset: true },
  { ov: "tachieH", base: "misayos.tachieH", json: { "misayos.tachieH": "h" } },
  { ov: "tachieX", json: { "misayos.tachieXOffset": "w" }, offset: true },
  { ov: "tachieY", json: { "misayos.tachieYOffset": "h" }, offset: true },
  { ov: "recordSize", base: "misayos.recordSize", json: { "misayos.recordSize": "h" } },
  { ov: "recordX", json: { "misayos.recordXOffset": "w" }, offset: true },
  { ov: "recordY", json: { "misayos.recordYOffset": "h" }, offset: true },
];
const r6 = (n) => Math.round(n * 1e6) / 1e6;   // 比例取 6 位: 够精确, 又不会让快照因浮点尾数抖动
const r2 = (n) => Math.round(n * 100) / 100;   // 滑杆回到像素, 取 2 位
const axisLen = (axis) => (axis === "w" ? facts.W : facts.H);

/** 滑杆现状 -> design.json layout 段 (键与 LAYOUT_SPEC 一一对应) */
export function layoutForExport() {
  const o = {};
  for (const s of LAYOUT_SPEC) {
    const ov = Number(state.OV[s.ov]);
    const px = s.offset
      ? (Number.isFinite(ov) ? ov : 0)
      : (Number.isFinite(ov) && ov !== 0 ? ov : facts.value(s.base));
    for (const [key, axis] of Object.entries(s.json)) o[key] = r6(px / axisLen(axis));
  }
  return o;
}

/** design.json layout 段 -> 滑杆 px。不认识的键走 extra 保留, 导入不会丢东西。 */
export function applyLayout(obj) {
  for (const s of LAYOUT_SPEC) {
    for (const [key, axis] of Object.entries(s.json)) {
      const v = Number(obj?.[key]);
      if (!Number.isFinite(v)) continue;
      state.OV[s.ov] = r2(v * axisLen(axis));
    }
  }
}

function objOf(sec) {
  const o = {};
  // textures/svgs/fonts 值为 {path, blob}, colors 值为字符串 - 兼容两种形态
  for (const [k, v] of Object.entries(S[sec])) o[k] = v && typeof v === "object" ? v.path : v;
  return o;
}
function mergeExtras(sec, base) {
  const ex = Object.hasOwn(S.extra, sec) ? S.extra[sec] : null;
  if (!ex || typeof ex !== "object") return base;
  const out = { ...base };
  for (const [k, v] of Object.entries(ex)) {
    if (!UNSAFE_KEYS.has(k) && !Object.hasOwn(out, k)) out[k] = v;
  }
  return out;
}
function buildDesignJSON() {
  const o = {
    _readme: "bocchi 设计模板 (Design Template). 复制本文件到材质包 assets/minecraft/client/design.json 即可替换整个设计. 想换哪项就改哪项, 其余自动回退到 mod 内置默认. 所有以 _ 开头的键是注释/说明, 加载时会忽略. 路径格式: namespace:path, 省略命名空间则默认 minecraft. 颜色格式: #AARRGGBB 或 #RRGGBB.",
    textures: mergeExtras("textures", { _comment: "位图资源: 立绘/Logo/加载图", ...objOf("textures") }),
    svgs: mergeExtras("svgs", { _comment: "主菜单按钮图标 (SVG), 与按钮 icon 参数对应: lang/multi/option/quit/single/theme", ...objOf("svgs") }),
    fonts: mergeExtras("fonts", { _comment: "Skia 字体. 键 = FontSet 中的字体名 (区分大小写), 值 = ttf/otf 文件路径. 换同名字体直接换文件, 换路径改这里.", ...objOf("fonts") }),
    colors: mergeExtras("colors", { _comment: "设计色板, 代码内硬编码颜色已接入此表. 格式 #RRGGBB 或 #AARRGGBB", ...objOf("colors") }),
    texts: mergeExtras("texts", { _comment: "界面文案. mInfoLine1~3 为 misayos 介绍三行; 其余键与 Bocchi Designer 文本面板一致", ...textsForExport() }),
    menu: { _comment: "主菜单主题: misayos (默认) / poulsen. 材质包覆盖此项即可切换主题, 资源重载后生效", theme: S.menu.theme },
    layout: mergeExtras("layout", {
      _comment: "misayos 菜单布局。值是相对宽/高的归一化比例, 不是像素 —— 所以不同分辨率下观感一致。尺寸类键写绝对比例 (缺省 = mod 内置值); Offset 类键写相对内置位置的偏移比例 (缺省 0)。键就是游戏端的查找路径 (layout.<键>)。整段删掉等于全部回退到 mod 内置布局。用 Bocchi Designer 的「misayos 布局微调」段生成。",
      ...layoutForExport(),
    }),
  };
  for (const [sec, val] of Object.entries(S.extra)) {
    if (sec === "textures" || sec === "svgs" || sec === "fonts" || sec === "colors" || sec === "texts" || sec === "layout") continue;
    if (sec === "menu") { for (const [k, v] of Object.entries(val)) if (!UNSAFE_KEYS.has(k) && !Object.hasOwn(o.menu, k)) o.menu[k] = v; continue; }
    o[sec] = typeof val === "object" && val !== null ? { ...val } : val;
  }
  return o;
}

/* ---------- 状态持久化 (localStorage) ----------
 * 持久化: OV 布局微调 / 文本内容 / colors 段 / menu.theme / 预览配色 / 面板开合 / 缩放
 * 不持久化: 上传的 blob (体积大, 刷新即还原; 有上传资源时 beforeunload 会提示先导出)
 * Node (单测) 下无 localStorage/window: save/load 静默跳过, beforeunload 不注册。
 */
const LS_KEY = "bocchi-designer:v1";
function saveState() {
  try {
    const st = {
      ov: state.OV,
      texts: state.TEXTS,
      colors: S.colors,
      theme: S.menu.theme,
      previewColors: state.PREVIEW_COLORS,
      openSections: state.openSections,
      zoom: state.zoom,
    };
    localStorage.setItem(LS_KEY, JSON.stringify(st));
  } catch (e) { /* 存储不可用时静默 */ }
}
function loadState() {
  try {
    const st = JSON.parse(localStorage.getItem(LS_KEY) || "null");
    if (!st) return false;
    // H3: localStorage 同样按外部输入处理, 过滤危险键
    if (st.ov && typeof st.ov === "object") Object.assign(state.OV, cleanCopy(st.ov));
    if (st.texts && typeof st.texts === "object") Object.assign(state.TEXTS, cleanCopy(st.texts));
    if (st.colors && typeof st.colors === "object") Object.assign(S.colors, cleanCopy(st.colors));
    if (st.theme && ["misayos", "poulsen"].includes(st.theme)) S.menu.theme = st.theme;
    if (st.previewColors && typeof st.previewColors === "object") Object.assign(state.PREVIEW_COLORS, cleanCopy(st.previewColors));
    if (Array.isArray(st.openSections)) state.openSections = st.openSections;
    if (typeof st.zoom === "number") state.zoom = st.zoom;
    return true;
  } catch (e) { return false; /* 损坏状态忽略 */ }
}

/** 当前有多少个资源只活在内存里 (上传或从 zip 导入的 blob)。
 *  它们不落 localStorage —— 刷新一次就没了, 且没有任何撤销机会。 */
export function uploadedCount() {
  let n = 0;
  for (const sec of ["textures", "svgs", "fonts"])
    for (const v of Object.values(S[sec])) if (v && v.blob) n++;
  return n;
}

/* M3: 落盘节流 (滑杆拖动画布拖拽高频触发); 页面卸载前冲刷 */
import { debounce } from "./core.js";
export const scheduleSave = debounce(saveState, 250);
if (typeof window !== "undefined") {
  window.addEventListener("beforeunload", (e) => {
    scheduleSave.flush();
    // 只在「真的有东西会丢」时才拦。没有上传资源时每次离开都弹确认, 等于把提示
    // 训练成背景噪音, 用户下次就一路点掉 —— 那才是提示真正失效的时刻。
    const n = uploadedCount();
    if (!n) return;
    e.preventDefault();
    e.returnValue = `有 ${n} 个资源只存在于内存中, 刷新/关闭会丢失。请先导出材质包。`;
  });
}

export {
  S, localAsset, usedPath, blobUrl, revokeBlobUrl, setBlob,
  splitPath, zipEntry, hexToCss, buildDesignJSON, saveState, loadState,
};
