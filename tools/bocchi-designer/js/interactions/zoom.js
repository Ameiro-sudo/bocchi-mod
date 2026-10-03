/* ============================================================================
 * zoom.js - 预览缩放: 适应窗口 / 100% / 200%
 *
 * fitStage 从 DOM 实测可用空间而不是抄常量, 所以这个模块与页面上「画框外面还占了多少
 * 高度」强绑定 —— 改 preview-wrap 的结构就会改变它的结果, 别把它当成纯数学看。
 * ==========================================================================*/
import { $, state } from "../core.js";
import { saveState } from "../design.js";
import { W, H } from "../facts.js";
import { setScale } from "../bench.js";

/* 画框自身的 padding + border, 两个方向。它同时决定「可用空间」和「画框自身尺寸」,
 * 两边必须用同一个数 —— 早先 fitStage 只扣 padding、applyScale 算尺寸时却把 border
 * 也算进去, 于是画框永远比可用空间高出一个边框, 预览区恒定多出 2px 滚动。 */
function frameChrome(frame) {
  const cs = getComputedStyle(frame);
  const v = (side) => parseFloat(cs[side]) || 0;
  return {
    bx: v("paddingLeft") + v("paddingRight") + v("borderLeftWidth") + v("borderRightWidth"),
    by: v("paddingTop") + v("paddingBottom") + v("borderTopWidth") + v("borderBottomWidth"),
  };
}
/* 舞台之外还有多少像素可用, 此前是手抄常量 (横 44 / 竖 56)。实测那些 chrome 约
 * 128px 竖向 (wrap 内边距 40 + 画框 padding+border 46 + 状态栏 20 + 两个 gap 24),
 * 少扣了 70 多 —— 于是「适应」算出的缩放偏大, 舞台超出可视区, 外层 overflow:auto
 * 直接出滚动条, 恰好是「适应」本该消除的那件事。CSS 一改还会继续漂。
 * 改成从 DOM 实测: 扣掉 wrap 内边距 + 画框 chrome + 状态栏等非画框子元素的实际高度
 * + gap 数×gap。以后动样式不会再让它算错。 */
export function fitStage() {
  const scaleEl = $("stageScale");
  const wrap = scaleEl.closest(".preview-wrap");
  const frame = scaleEl.closest("#stageFrame");
  if (!wrap || !frame) return;
  const px = (el, side) => parseFloat(getComputedStyle(el)[side]) || 0;
  const siblings = [...wrap.children].filter(c => c !== frame);
  const cs = getComputedStyle(wrap);
  const { bx, by } = frameChrome(frame);
  const availW = wrap.clientWidth - px(wrap, "paddingLeft") - px(wrap, "paddingRight") - bx;
  const availH = wrap.clientHeight - px(wrap, "paddingTop") - px(wrap, "paddingBottom") - by
    - siblings.reduce((h, c) => h + c.getBoundingClientRect().height, 0)
    - (parseFloat(cs.rowGap) || 0) * siblings.length;
  /* 夹一下下界: 可用空间可能被同级元素吃光 (窄屏下读数条换行会占掉大半屏高),
   * 这时 Math.min 会算出 0 或负数 —— scale(负数) 会把画布左右翻转, 比画不下更难
   * 解释。留一个 2% 的下界: 画布小到看不清, 但方向和位置仍然是对的。 */
  applyScale(Math.max(0.02, Math.min(availW / W, availH / H)), true);
}
function applyScale(scale, fromFit) {
  $("stageScale").style.transform = `scale(${scale})`;
  // transform 不改变布局盒: 舞台在布局上仍然占满 1280×720, 外层 overflow:auto 于是
  // 照样出滚动条 —— 「适应」把舞台缩小了, 却没把占位也缩小, 于是它消除滚动条的承诺
  // 落空, 而且在窄屏下 (可用宽 < 1280) 必然触发。给画框显式写缩放后的尺寸, 让
  // 视觉大小与布局占位一致。
  const frame = $("stageFrame");
  if (frame) {
    const { bx, by } = frameChrome(frame);
    frame.style.width = (W * scale + bx) + "px";
    frame.style.height = (H * scale + by) + "px";
    // 标尺轨是 frame 的直接子元素, 不随 stageScale 缩放; 把真实缩放系数以无单位
    // 自定义属性交给 CSS, 刻度间距 = 舞台坐标间距 × 缩放, 于是在任何缩放级别下
    // 刻度都对得上画布像素。这是 fitStage 与样式之间唯一的耦合点。
    frame.style.setProperty("--bench-scale", scale);
  }
  setScale(scale, !!fromFit);
  const buttons = document.querySelectorAll(".seg button[data-zoom]");
  for (const b of buttons) b.classList.toggle("on", b.dataset.zoom == (fromFit ? "0" : state.zoom));
}
export function setZoom(z) {
  state.zoom = z;
  saveState();
  if (z === 0) fitStage();
  else applyScale(z);
}
export function bindZoom() {
  document.querySelectorAll(".seg button[data-zoom]").forEach(b => {
    b.addEventListener("click", () => setZoom(+b.dataset.zoom));
  });
  window.addEventListener("resize", () => {
    if (state.zoom === 0) fitStage();
  });
}