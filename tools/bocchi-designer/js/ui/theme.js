/* ============================================================================
 * ui/theme.js - menu.theme 主菜单主题选择
 * ==========================================================================*/
import { toast } from "../core.js";
import { S, DEFAULT_DESIGN, saveState } from "../design.js";
import { relayout } from "../render.js";
import { createField } from "../store.js";
import { markDirty, scheduleDirty } from "./dirty.js";
import { registerBaseline } from "./baseline.js";

export function addThemeRow(body) {
  const row = document.createElement("div");
  row.className = "slider-row";
  const lab = document.createElement("label");
  lab.textContent = "theme";
  const sel = document.createElement("select");
  sel.id = "themeSel";
  for (const [v, l] of [["misayos", "misayos（默认）"], ["poulsen", "poulsen"]]) {
    const o = document.createElement("option");
    o.value = v; o.textContent = l;
    sel.appendChild(o);
  }
  sel.value = S.menu.theme;
  /** 主题落地函数 (入栈与回放共用) */
  const applyTheme = (v) => {
    S.menu.theme = v;
    sel.value = v;
    relayout();
    saveState();
    scheduleDirty();
    toast("主题已改为 " + v + "（design.json menu.theme）");
  };
  const field = createField({
    label: "主题",
    read: () => S.menu.theme,
    apply: applyTheme,
  });
  registerBaseline(() => field.resync());
  sel.addEventListener("change", () => field.set(sel.value));
  row.append(lab, sel);
  markDirty(row, () => S.menu.theme !== DEFAULT_DESIGN.menu.theme);
  body.appendChild(row);
  const hint = document.createElement("div");
  hint.className = "hint";
  hint.textContent = "游戏内优先级: ~/.bocchi/theme.json（游戏内切换）> 材质包 design.json > mod 内置默认。";
  body.appendChild(hint);
}