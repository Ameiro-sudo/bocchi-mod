/* ============================================================================
 * ui/dirty.js - 「已改 N 项」标记
 *
 * 面板有 60 多个可编辑项, 而「我到底改了什么、还差什么」这件事此前只能靠逐行肉眼比对
 * 默认值 —— 于是最常见的两种结局: 漏改一项没发现, 或者改回默认了还留在产物里。
 * 每一项注册一个比较函数, 统一刷成行上的 is-modified 标记 + 顶栏计数。
 * 只覆盖模型字段 (配色/文案/资源路径); 布局滑杆另有数值读数与「全部复位」, 不重复标记。
 * ==========================================================================*/
import { $, rafThrottle } from "../core.js";

const DIRTY = [];

/** 注册一行: isModified() 返回当前是否与默认值不同 */
export function markDirty(row, isModified) { DIRTY.push({ row, isModified }); }

export function refreshDirtyMarks() {
  let n = 0;
  for (const d of DIRTY) {
    const on = !!d.isModified();
    d.row.classList.toggle("is-modified", on);
    if (on) n++;
  }
  const chip = $("dirtyChip");
  if (!chip) return;
  chip.hidden = n === 0;
  const c = $("dirtyCount");
  if (c) c.textContent = String(n);
}

/** 高频路径 (拖色/逐字输入) 走 rAF 节流: 一帧最多刷一次, 不必每事件都重排 DOM */
export const scheduleDirty = rafThrottle(refreshDirtyMarks);