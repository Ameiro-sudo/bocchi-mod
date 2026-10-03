/* ============================================================================
 * history.js - 撤销/重做栈 (纯逻辑, 无 DOM 依赖, Node 单测可直接导入)
 *
 * 命令 = { label, undo(), redo() }。三类入栈形态:
 *   push(cmd, coalesceKey?)      普通命令; 同 key 在 COALESCE_MS 时间窗内合并
 *                                (保留最早 undo, 用最新 redo -- 滑杆/取色器连续
 *                                 拖动合并为一步)
 *   beginGesture()/endGesture()  手势静音区 (画布拖拽): 区间内的 push 一律吞掉,
 *                                由手势所有者在结束前用起止快照自行合成一条完整
 *                                命令再入栈 (多轴位移才不会丢轴)
 *
 * applying 守卫: undo/redo 回放期间的一切 push 静默丢弃, 杜绝回放入栈。
 * 任何新 push 清空 redo 栈 (分叉历史)。上限 MAX_HISTORY, 溢出丢最旧。
 * ==========================================================================*/

export const MAX_HISTORY = 100;
export const COALESCE_MS = 700;

const undoStack = [];
const redoStack = [];
let applying = false;
let muted = false;           // 手势静音区开关
let lastCoalesce = null;     // { key, at }

let nowFn = () => Date.now();
/** 测试注入时钟 */
export function _setNow(fn) { nowFn = fn; }

function trim() {
  while (undoStack.length > MAX_HISTORY) undoStack.shift();
}

/** 入栈一条命令; applying/静音/时间窗三种分流见文件头 */
export function push(cmd, coalesceKey) {
  if (applying || muted || !cmd || typeof cmd.undo !== "function" || typeof cmd.redo !== "function") return;
  const t = nowFn();
  // 同键时间窗合并: 保留最早 undo, 用最新 redo, 刷新时间戳
  if (coalesceKey && lastCoalesce && lastCoalesce.key === coalesceKey && t - lastCoalesce.at < COALESCE_MS) {
    undoStack[undoStack.length - 1].redo = cmd.redo;
    lastCoalesce.at = t;
    redoStack.length = 0;
    return;
  }
  undoStack.push({ label: String(cmd.label || ""), undo: cmd.undo, redo: cmd.redo });
  trim();
  redoStack.length = 0;
  lastCoalesce = coalesceKey ? { key: coalesceKey, at: t } : null;
  notify();
}

/** 开启手势静音区: 区间内 push 全部吞掉, 命令由所有者在结束时合成入栈 */
export function beginGesture() {
  muted = true;
  lastCoalesce = null;
}
/** 关闭手势静音区 */
export function endGesture() {
  muted = false;
}

/** 撤销, 返回命令 label; 空栈返回 null */
export function undo() {
  const cmd = undoStack.pop();
  if (!cmd) return null;
  applying = true;
  try { cmd.undo(); } finally { applying = false; }
  redoStack.push(cmd);
  lastCoalesce = null;
  notify();
  return cmd.label;
}

/** 重做, 返回命令 label; 空栈返回 null */
export function redo() {
  const cmd = redoStack.pop();
  if (!cmd) return null;
  applying = true;
  try { cmd.redo(); } finally { applying = false; }
  undoStack.push(cmd);
  trim();
  lastCoalesce = null;
  notify();
  return cmd.label;
}

/** 导入新模型时清空全部历史。
 *  muted 一并清掉: 未闭合的静音区会把后续所有 push 吞掉, clearHistory 是会话级
 *  的重置, 留着它等于让「导入之后这个页面再也不入栈」(test/history.test.mjs
 *  「clearHistory 解除」一条锁的就是这个)。拖拽进行中导入导致的收尾错位, 真正的
 *  修法是让清栈先于任何模型写入 —— 见 io.applyDesignJSON 开头。 */
export function clearHistory() {
  undoStack.length = 0;
  redoStack.length = 0;
  muted = false;
  lastCoalesce = null;
  notify();
}

export const canUndo = () => undoStack.length > 0;
export const canRedo = () => redoStack.length > 0;
/** 测试/状态栏用: {undo, redo} 深度 */
export function stats() { return { undo: undoStack.length, redo: redoStack.length }; }

/* 栈变化通知。撤销按钮的可用态只能由这里驱动: 让每个写入点各自记得刷新, 漏一处
   就留下一个恒亮的按钮, 而恒亮的按钮看起来是「可按」, 按下去只弹一句提示。 */
const listeners = new Set();
/** 注册栈变化回调, 返回注销函数 */
export function onStackChange(fn) { listeners.add(fn); return () => listeners.delete(fn); }
function notify() {
  if (!listeners.size) return;
  const u = canUndo(), r = canRedo();
  for (const fn of listeners) { try { fn(u, r); } catch (e) { console.error("[history] onStackChange 回调抛错", e); } }
}