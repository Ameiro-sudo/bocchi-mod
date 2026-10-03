/* ============================================================================
 * ui/color.js - design.json colors 段编辑行 (文本框 + 取色器 + 还原内置)
 * ==========================================================================*/
import { toast } from "../core.js";
import { S, DEFAULT_DESIGN, scheduleSave } from "../design.js";
import { refreshVinyl } from "../preview.js";
import { scheduleRelayout } from "../render.js";
import { push as pushHistory } from "../history.js";
import { markDirty, scheduleDirty } from "./dirty.js";
import { registerBaseline } from "./baseline.js";

export function addColorRow(body, label, key) {
  const row = document.createElement("div");
  row.className = "color-row";
  const lab = document.createElement("div");
  lab.className = "c-label"; lab.textContent = label;
  const text = document.createElement("input");
  text.type = "text"; text.value = S.colors[key];
  const pick = document.createElement("input");
  pick.type = "color";
  const m8 = /^#?([0-9a-fA-F]{8})$/.exec(S.colors[key]);
  const m6 = /^#?([0-9a-fA-F]{6})$/.exec(S.colors[key]);
  if (m8) pick.value = "#" + m8[1].slice(2);
  else if (m6) pick.value = "#" + m6[1];
  /** 落地函数 (入栈与回放共用) */
  const applyColor = (value) => {
    S.colors[key] = value;
    text.value = value;
    refreshVinyl();
    // 取色器 input 是连续事件 (拖动时每像素一个), 与文本框同理由走节流
    scheduleRelayout();
    scheduleSave();
    scheduleDirty();
  };
  let committedColor = S.colors[key];   // 最近一次入栈的值 (时间窗合并的基准)
  /** 值变化后调用: 与 committedColor 比对入栈; 同键连续拖取色器自动合并 */
  const commitColor = () => {
    const cur = S.colors[key];
    if (cur === committedColor) return;
    const from = committedColor, to = cur;
    pushHistory({
      label: `配色 ${key}`,
      undo: () => applyColor(from),
      redo: () => applyColor(to),
    }, "color:" + key);
    committedColor = cur;
  };
  registerBaseline(() => { committedColor = S.colors[key]; });
  text.addEventListener("change", () => {
    applyColor(text.value.trim() || DEFAULT_DESIGN.colors[key]);
    commitColor();
  });
  pick.addEventListener("input", () => {
    const old = S.colors[key];
    const a = /^#?([0-9a-fA-F]{2})([0-9a-fA-F]{6})$/.exec(old);
    applyColor(a ? "#" + a[1] + pick.value.slice(1) : pick.value);
    commitColor();
  });
  pick.addEventListener("change", commitColor);   // 松手收尾 (合并窗已覆盖, 兜底)
  // 「内置」: 还原 design.json 默认色板 (可撤销)
  const rst = document.createElement("button");
  rst.className = "row-reset"; rst.textContent = "内置";
  rst.title = "恢复默认色值";
  rst.addEventListener("click", () => {
    const def = DEFAULT_DESIGN.colors[key];
    if (S.colors[key] === def) { toast(`配色 ${key} 已是默认值`); return; }
    const prev = S.colors[key];
    applyColor(def);
    committedColor = def;
    pushHistory({
      label: `还原配色 ${key}`,
      undo: () => { applyColor(prev); committedColor = prev; },
      redo: () => { applyColor(def); committedColor = def; },
    });
  });
  row.append(lab, text, pick, rst);
  body.appendChild(row);
  markDirty(row, () => S.colors[key] !== DEFAULT_DESIGN.colors[key]);
}