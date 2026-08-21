/* ============================================================================
 * status.js - 舞台下方状态栏写入器 (interactions 与 panels 共用)
 * ==========================================================================*/
import { $ } from "./core.js";

/** 写入状态栏 HTML (内容全部来自本工具内部的静态文案/键名, 无外部输入) */
export function setStatus(html) {
  const el = $("status");
  if (el) el.innerHTML = html;
}
