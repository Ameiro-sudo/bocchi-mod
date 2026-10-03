/* ============================================================================
 * core.js - 基础工具: 应用状态 / DOM 助手 / toast / 下载 / 节流 (零依赖)
 *
 * 本模块不依赖任何其他模块; 所有纯函数可在 Node 下直接单测。
 * zip 读写不在这里 —— 见 zip.js (纯计算, 可独立单测)。
 * ==========================================================================*/

/* ---------- 应用状态 (布局微调/文本/预览配色/面板开合/缩放) ----------
 * 持久化由 design.js 的 saveState/loadState 负责 */
export const state = {
  OV: {},                 // misayos 布局微调 (预览用)
  TEXTS: null,            // 可编辑文本 (design.js 初始化后指向 S.texts 同一对象)
  PREVIEW_COLORS: {       // 预览配色 (仅预览, 不导出)
    "--accent": "#FBA0BE", "--accent-deep": "#E88BA6",
    "--bg-top": "#07021C", "--bg-bottom": "#492F49",
    "--splash-bg": "#1F1F1F", "--btn-bg": "#353535",
  },
  /* 默认展开哪几段。原先是 ["layout-misayos","res-textures","export"]: 把「导出」放在
   首屏黄金位, 却把 21 行的 texts 收起来 —— 而 texts 是全页最大的可编辑面, 收起后
   面板看起来像缺了一块。改成 layout(画布拖拽, 头牌功能) + texts(最大编辑面)。
   只影响新会话: 已存过 localStorage 的用户 openSections 是从磁盘读的, 不受影响。 */
  openSections: ["layout-misayos", "texts"],  // 展开的面板 section id
  zoom: 0,                // 0=适应窗口 1=100% 2=200%
};

/* ---------- DOM 助手 ---------- */
export const $ = (id) => document.getElementById(id);
export const set = (el, css) => { if (el) el.style.cssText = css; };

/* ---------- toast ---------- */
let toastTimer = null;
export function toast(msg, warn) {
  let t = $("toast");
  if (!t) {
    t = document.createElement("div");
    t.id = "toast";
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.classList.toggle("warn", !!warn);
  t.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove("show"), 3200);
}

/* ---------- debounce (M3: 拖拽/滑杆高频变更聚合) ---------- */
export function debounce(fn, ms) {
  let t = null;
  const g = function (...args) { clearTimeout(t); t = setTimeout(() => { t = null; fn.apply(this, args); }, ms); };
  g.cancel = () => { if (t) { clearTimeout(t); t = null; } };
  g.flush = () => { if (t) { clearTimeout(t); t = null; fn(); } };
  return g;
}

/* rAF 节流: 合并同帧多次调用为一次 */
export function rafThrottle(fn) {
  let pending = false, rafId = 0, lastArgs = null;
  const g = function (...args) {
    lastArgs = args;
    if (pending) return;
    pending = true;
    rafId = requestAnimationFrame(() => { pending = false; fn.apply(this, lastArgs); });
  };
  g.flush = function () { if (pending) { cancelAnimationFrame(rafId); pending = false; fn.apply(this, lastArgs); } };
  return g;
}

/* HTML 转义 (L7: 文本编辑只放行 <br>, 其余标签转义, 消除自我 XSS 面) */
export function escapeHtml(s) {
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/* ---------- 下载 ---------- */
export function download(blob, name) {
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 30000);
}