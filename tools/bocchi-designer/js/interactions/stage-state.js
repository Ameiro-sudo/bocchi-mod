/* ============================================================================
 * stage-state.js - 舞台交互的共享状态 (当前舞台 / 当前选中 / 当前拖拽)
 *
 * 为什么要单独一个模块: 舞台切换 (stage-view) 与选中拖拽 (select-drag) 互相要用对方的
 * 状态 —— showStage 要清掉选中态, Esc 取消选中要写回舞台的状态栏基线文案。两者各自持有
 * 变量的话, 一拆就成环, 不拆就只能继续挤在同一个文件里。这个模块零依赖, 于是两个上层
 * 模块都单向依赖它, 环自然断开。
 * ==========================================================================*/

export let current = "misayos";
export let selKey = null;
export let drag = null;

export function setCurrent(v) { current = v; }
/** accessor form: callers read it as a call, not as a bare binding */
export const currentStage = () => current;
export function setSelKey(v) { selKey = v; }
export function setDrag(v) { drag = v; }

const NAMES = {
  splash: "加载页（点击 TAP TO START）",
  misayos: "主菜单 misayos",
  poulsen: "主菜单 poulsen",
};

/** 当前舞台的状态栏基线文案。
 *  切舞台和取消选中都要写这段文案 —— 此前只有切舞台写, clearSel 写空串,
 *  于是「选中→Esc」之后状态栏就永久变空, 用户失去「现在看的是哪个舞台/能点什么」
 *  这条唯一常驻提示。构造收在这里, 一是让基线只有一份, 二是它纯由 current 决定,
 *  放进任一上层模块都会变成反向依赖。 */
export function stageStatus() {
  return "<b>" + NAMES[current] + "</b> · 1:1 预览 · 1280×720 设计分辨率"
    + (current === "misayos" ? " · 点击元素可选中拖拽" : "");
}