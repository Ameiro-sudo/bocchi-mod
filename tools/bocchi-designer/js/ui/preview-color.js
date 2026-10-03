/* ============================================================================
 * ui/preview-color.js - 预览配色 (只调设计器自己的界面主题, 不进产物)
 * ==========================================================================*/
import { state } from "../core.js";
import { scheduleSave } from "../design.js";
import { createField } from "../store.js";

/* 预览配色的落地函数 (拖色/撤销/重做共用)。它写的是三处地方, 必须一次写全:
   状态表 -> documentElement CSS 变量 -> --btn-bg 的按钮内联背景。少写任何一处,
   撤销就只撤了一半, 屏幕上留着一个撤不掉的色块。取色器也要跟着回写, 否则用户
   在撤销后看到的色块和控件里的色块对不上。 */
function applyPreviewColor(key, v, input) {
  document.documentElement.style.setProperty(key, v);
  state.PREVIEW_COLORS[key] = v;
  if (key === "--btn-bg") document.querySelectorAll(".btn, .btn-icon").forEach(el => el.style.background = v);
  if (input && document.activeElement !== input) input.value = v;
  scheduleSave();
}

export function addPreviewColor(body, label, key) {
  const wrap = document.createElement("div");
  wrap.className = "field";
  const lab = document.createElement("label");
  lab.textContent = label;
  const input = document.createElement("input");
  input.type = "color";
  input.value = state.PREVIEW_COLORS[key];
  // 预览配色此前只落盘不入栈 —— 等于改了就撤不回来。这里补上; 取色器是连续
  // input 事件, 所以按 key 合并, 免得拖一次色板留下几十条历史。
  const field = createField({
    label: `预览色 ${label}`,
    read: () => state.PREVIEW_COLORS[key],
    apply: (v) => applyPreviewColor(key, v, input),
    coalesce: "pcolor:" + key,
  });
  input.addEventListener("input", () => field.set(input.value));
  wrap.append(lab, input);
  body.appendChild(wrap);
}