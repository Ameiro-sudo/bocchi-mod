/* ============================================================================
 * bench.js - 校准台读数条: 画布上的实测值 (光标坐标 / 选中尺寸 / 当前缩放)
 *
 * 面板上的滑杆写的是「意图」: 你想让它在哪、想让它多大。
 * 画布上缺的是「实测」: 现在它真的在哪、真的多大, 以及你正盯着哪个像素。
 * 这个模块只负责把实测值写进 .bench-readout, 不参与任何编辑逻辑。
 *
 * 为什么几何由调用方算好再传进来: 舞台坐标换算的口径已经存在于
 * interactions/select-drag.js (stagePos / cssRect), 在这里重写一遍必然漂移;
 * 反过来让 select-drag 反向依赖本模块又会和 interactions/ 之间的方向打架。
 * 所以本模块只暴露纯写文本的出口 —— 算好了报个数, 怎么算的不归它管。
 *
 * 读数不进 #status: dev/verify.mjs 采 #status 的 textContent 进快照,
 * 塞进去会变成每次采样都不同的时序噪声。
 * ==========================================================================*/
import { $ } from "./core.js";
import { W, H } from "./facts.js";

const BLANK = "\u2014";

/* 只有 textContent 真的变了才写 DOM: mousemove 每秒能来上百次,
 * 无谓的写入会把读数条自己的重排也拖进来。dim 类同步切, 让「有值/没值」一眼可分。 */
function put(id, text, blank) {
  const el = $(id);
  if (!el) return;
  if (el.textContent !== text) el.textContent = text;
  el.classList.toggle("dim", !!blank);
}

/* ---------- 光标 ---------- */

/* x/y 为 null 表示「此刻没有有效坐标」(画布外 / 指针已移出)。 */
export function setCursor(x, y) {
  const blank = x === null || y === null;
  put("roCursorX", blank ? BLANK : String(Math.round(x)), blank);
  put("roCursorY", blank ? BLANK : String(Math.round(y)), blank);
}
export const clearCursor = () => setCursor(null, null);

/* ---------- 选中 ---------- */

export function setSelection(label, w, h, left, top) {
  put("roSelLabel", label || "\u65e0", !label);
  put("roSelSize", BLANK, true);
  put("roSelPos", BLANK, true);
  if (label) {
    put("roSelSize", Math.round(w) + " \u00d7 " + Math.round(h), false);
    put("roSelPos", Math.round(left) + ", " + Math.round(top), false);
  }
}
export const clearSelection = () => setSelection("", 0, 0, 0, 0);

/* ---------- 缩放 ---------- */

/* 适应窗口时按钮亮的是「适应」而不是某个百分比, 所以这里也把 fit 标出来,
 * 否则 62% 会被读成有人手动调到了 62%。scale 是 applyScale 里的真实系数。 */
export function setScale(scale, fitted) {
  const pct = Math.round(scale * 100) + "%";
  put("roScale", fitted ? "\u9002\u5e94 " + pct : pct, false);
}

/* ---------- 绑定 ---------- */

/* 监听挂在 #stageFrame 而不是 #stage: 指针移到标尺轨上时画布坐标依然有意义,
 * 而标尺轨是 frame 的直接子元素, 挂在 stage 上收不到。
 * 拖拽过程中 #selBox 会吃掉部分 mousemove, 但事件仍冒泡到 frame, 读数不断。 */
export function bindBench() {
  const frame = $("stageFrame");
  const stage = $("stage");
  if (!frame || !stage) return;
  frame.addEventListener("mousemove", (e) => {
    const r = stage.getBoundingClientRect();
    if (!r.width || !r.height) return;
    const x = (e.clientX - r.left) / (r.width / W);
    const y = (e.clientY - r.top) / (r.height / H);
    setCursor(x >= 0 && x <= W && y >= 0 && y <= H ? x : null,
              x >= 0 && x <= W && y >= 0 && y <= H ? y : null);
  });
  frame.addEventListener("mouseleave", clearCursor);
}
