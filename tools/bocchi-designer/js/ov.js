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
import { push as pushHistory } from "./history.js";
import { createField } from "./store.js";

const OV = state.OV;

/** key -> { input, val, def } ; panels.addSlider 注册 */
export const SLIDERS = {};

/** key -> 可撤销字段 (与面板/画布拖拽共用同一份撤销基准), 见 setOV */
const OV_FIELDS = {};

export function registerSlider(key, def, input, val) {
  SLIDERS[key] = { input, val, def };
  if (!(key in OV)) OV[key] = def;
  OV_FIELDS[key] = createField({
    label: "布局微调",
    read: () => OV[key],
    apply: (v) => applyOV(key, v),
    coalesce: "ov:" + key,
  });
}

function setFill(input) {
  const min = +input.min, max = +input.max, v = +input.value;
  input.style.setProperty("--fill", ((v - min) / (max - min) * 100).toFixed(1) + "%");
}
export { setFill };

/** 落地函数 (入栈与撤销/重做回放共用): 只改值+UI+调度, 不做历史记录 */
function applyOV(key, v) {
  OV[key] = v;
  const s = SLIDERS[key];
  if (s) {
    s.input.value = v;
    s.val.textContent = v;
    s.val.classList.toggle("is-default", v == s.def);
    setFill(s.input);
  }
  scheduleRelayout();
  scheduleSave();
}

/** 滑杆行输入变化: 统一走 setOV (带历史记录, 同键连续拖动自动合并为一步) */
export function onSliderInput(key, v) {
  setOV(key, +v);
}

/** 编程设值 (拖拽/方向键/双击复位): 收敛到滑块范围, 两位小数, 同步滑杆 UI */
export function setOV(key, v) {
  if (typeof v !== "number" || Number.isNaN(v)) return;
  const s = SLIDERS[key];
  v = s ? Math.min(Math.max(v, +s.input.min), +s.input.max) : v;
  v = Math.round(v * 100) / 100;
  // 滑杆与拖拽共用一个 field: 拖拽期间 setOV 的逐帧 push 被手势静音区吞掉, 收尾
  // 由 pushOVGesture 用 snapshotOV() 起的另一条命令封口, 但「基准」必须是同一个,
  // 否则滑杆拖完后紧接着的画布拖拽会以一个已经作废的值为 from。
  OV_FIELDS[key].set(v);
}

/** 拖拽起始快照: 全部 OV 当前值 (无持久化值时取滑杆现值), 供 move/resize 增量计算 */
export function snapshotOV() {
  const snap = {};
  for (const k of Object.keys(SLIDERS)) {
    snap[k] = OV[k] != null ? OV[k] : +SLIDERS[k].input.value;
  }
  return snap;
}

/** 画布拖拽手势收尾: 用起始快照与当前值合成一条完整命令 (多轴位移不丢轴)。
 * 手势期间 setOV 的逐帧 push 已被静音区吞掉, 这里是唯一入栈点。 */
export function pushOVGesture(label, startMap) {
  let changed = false;
  const before = {}, after = {};
  for (const k of Object.keys(SLIDERS)) {
    const start = startMap && startMap[k] != null ? +startMap[k] : +SLIDERS[k].def;
    const cur = OV[k] != null ? +OV[k] : +SLIDERS[k].def;
    if (start !== cur) changed = true;
    before[k] = start;
    after[k] = cur;
  }
  if (!changed) return;
  const restore = (map) => {
    for (const k of Object.keys(SLIDERS)) applyOV(k, map[k]);
    applyNow();          // 重排 + 选中框跟随
    saveState();
  };
  pushHistory({ label, undo: () => restore(before), redo: () => restore(after) });
}

/** 全部复位到默认值 (面板"全部复位"按钮); 整体作为一条可撤销命令 */
export function resetAll() {
  const before = {};
  for (const k of Object.keys(SLIDERS)) {
    if (+OV[k] !== +SLIDERS[k].def) before[k] = +OV[k];
  }
  const applyDefaults = () => {
    for (const k of Object.keys(SLIDERS)) applyOV(k, SLIDERS[k].def);
    applyNow();          // 重排 + 选中框跟随
    saveState();
  };
  applyDefaults();
  if (Object.keys(before).length) {
    pushHistory({
      label: "复位全部布局",
      undo: () => { for (const [k, v] of Object.entries(before)) applyOV(k, v); applyNow(); saveState(); },
      redo: applyDefaults,
    });
  }
  toast("misayos 布局已全部复位");
}

/** 载入时把持久化值收敛回滑块范围 (L8), 由 panels.addSlider 使用。
 *  收敛是改写了 state.OV 的 —— 旧实现改完就搁着, 既不入栈也不落盘, 于是
 *  localStorage 里一直躺着一个越界值, 每次打开页面都被静默夹一次, 而磁盘态与
 *  内存态从此长期不一致。改完要落盘, 让下次启动不再重复夹。载入收敛不是用户
 *  的编辑, 所以不入撤销栈。 */
let clampedAny = false;
export function clampSaved(key, min, max, def) {
  const saved = OV[key] != null ? Math.min(Math.max(+OV[key], +min), +max) : +def;
  if (OV[key] != null && +OV[key] !== saved) { OV[key] = saved; clampedAny = true; }
  return saved;
}

/** 面板构建完成后调用: 若期间发生过收敛, 把结果写回 localStorage */
export function flushClamped() {
  if (!clampedAny) return false;
  clampedAny = false;
  saveState();
  return true;
}

/** 模型被导入 (design.json/zip 里的 layout 段) 后, 把滑杆 UI 拉回 state.OV。
 *  applyLayout() 只改数据不改界面, 不调这个的话导入完面板还显示导入前的值,
 *  用户会以为导入没生效 —— 而实际生效了, 只是看不见。 */
export function syncSlidersFromModel() {
  for (const k of Object.keys(SLIDERS)) applyOV(k, OV[k] != null ? +OV[k] : +SLIDERS[k].def);
  // applyLayout 整体替换了模型值, 每个滑杆 field 的撤销基准必须一并搬过来,
  // 否则导入后第一次拖动会以「导入前的旧值」为 from, 一撤销就整屏跳回导入前。
  for (const f of Object.values(OV_FIELDS)) f.resync();
  applyNow();
}
