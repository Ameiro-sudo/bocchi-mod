/* ============================================================================
 * ov.js - misayos 布局微调 (OV) 值域: 滑杆注册表 + 设值/复位
 *
 * panels.js 负责 DOM 行创建并调用 registerSlider; interactions.js (拖拽/键盘)
 * 只经 setOV/snapshotOV 读写, 不再反向依赖面板模块。
 * ==========================================================================*/
import { $, toast } from "./core.js";
import { state } from "./core.js";
import { scheduleSave, saveState } from "./design.js";
import { applyNow, scheduleRelayout } from "./render.js";

const OV = state.OV;

/** key -> { input, val, def } ; panels.addSlider 注册 */
export const SLIDERS = {};

export function registerSlider(key, def, input, val) {
  SLIDERS[key] = { input, val, def };
  if (!(key in OV)) OV[key] = def;
}

function setFill(input) {
  const min = +input.min, max = +input.max, v = +input.value;
  input.style.setProperty("--fill", ((v - min) / (max - min) * 100).toFixed(1) + "%");
}
export { setFill };

/** 滑杆行输入变化: 更新值显示 + 调度重排与落盘 */
export function onSliderInput(key, v, val) {
  OV[key] = v;
  const s = SLIDERS[key];
  if (val) { val.textContent = v; val.classList.toggle("is-default", s && v == s.def); }
  if (s && s.input) setFill(s.input);
  scheduleRelayout();
  scheduleSave();
}

/** 编程设值 (拖拽/方向键): 收敛到滑块范围, 两位小数, 同步滑杆 UI */
export function setOV(key, v) {
  if (typeof v !== "number" || Number.isNaN(v)) return;
  const s = SLIDERS[key];
  v = s ? Math.min(Math.max(v, +s.input.min), +s.input.max) : v;
  v = Math.round(v * 100) / 100;
  OV[key] = v;
  if (s) {
    s.input.value = v; s.val.textContent = v;
    s.val.classList.toggle("is-default", v == s.def);
    setFill(s.input);
  }
  scheduleRelayout();
  scheduleSave();
}

/** 拖拽起始快照: 全部 OV 当前值 (无持久化值时取滑杆现值), 供 move/resize 增量计算 */
export function snapshotOV() {
  const snap = {};
  for (const k of Object.keys(SLIDERS)) {
    snap[k] = OV[k] != null ? OV[k] : +SLIDERS[k].input.value;
  }
  return snap;
}

/** 全部复位到默认值 (面板"全部复位"按钮) */
export function resetAll() {
  for (const k of Object.keys(SLIDERS)) {
    const s = SLIDERS[k];
    OV[k] = s.def;
    s.input.value = s.def;
    s.val.textContent = s.def;
    s.val.classList.add("is-default");
    setFill(s.input);
  }
  applyNow();          // 重排 + 选中框跟随
  saveState();
  toast("misayos 布局已全部复位");
}

/** 载入时把持久化值收敛回滑块范围 (L8), 由 panels.addSlider 使用 */
export function clampSaved(key, min, max, def) {
  const saved = OV[key] != null ? Math.min(Math.max(+OV[key], +min), +max) : +def;
  if (OV[key] != null && +OV[key] !== saved) OV[key] = saved;
  return saved;
}
