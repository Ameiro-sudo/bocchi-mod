/* ============================================================================
 * render.js - 渲染编排: 整体重排 + design.json 预览刷新
 *
 * relayout()      三个舞台一次重排 (layout.js) + 唱片配色回填 + json 预览
 * scheduleRelayout()  rAF 合并版, 高频变更 (滑杆/拖拽) 用
 * setAfterRelayout(fn) 注册重排后回调 (main.js 接线为选中框跟随), 保持原
 *                 "relayout 后必跟 updateSelBox" 的行为而不引入模块环。
 * ==========================================================================*/
import { $ } from "./core.js";
import { rafThrottle } from "./core.js";
import { buildDesignJSON } from "./design.js";
import { splash, misayos, poulsen } from "./layout.js";
import { refreshVinyl } from "./preview.js";

export function relayout() {
  splash();
  misayos();
  poulsen();
  refreshVinyl();   // layout 会用 cssText 覆盖 mRecord, 需重设唱片配色
  const pre = $("jsonPreview");
  if (pre) pre.textContent = JSON.stringify(buildDesignJSON(), null, 2);
}

let afterRelayout = null;
/** main.js 在组装期调用一次; fn 在每次重排后同步执行 */
export function setAfterRelayout(fn) { afterRelayout = fn; }
/** 立即执行一次"重排 + 后置回调" (复位等需要同步生效的场景) */
export function applyNow() {
  relayout();
  if (afterRelayout) afterRelayout();
}

// M3: 高频变更 (滑杆拖动/画布拖拽) 聚合到单帧重排
export const scheduleRelayout = rafThrottle(() => {
  relayout();
  if (afterRelayout) afterRelayout();
});
