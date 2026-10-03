/* ============================================================================
 * ui/section.js - 折叠分区骨架
 *
 * 面板每一段的壳 (标题 / 展开箭头 / 右上角工具按钮 / 徽标) 与格子内的小标题。
 * 折叠状态存在 core.state.openSections 里, 切换即落盘 —— 关页再回来仍是你上次的样子。
 * ==========================================================================*/
import { $ } from "../core.js";
import { state } from "../core.js";
import { saveState } from "../design.js";

/** #controls 在 build() 被调用时才取 —— 此前是模块顶层就 $("controls"),
 *  于是「import 这个模块」本身带上了「DOM 必须已经存在」的副作用。 */
function controls() { return $("controls"); }

export function addSection(title, id, opts) {
  opts = opts || {};
  const sec = document.createElement("div");
  sec.className = "sec" + (state.openSections.includes(id) ? " open" : "");
  sec.id = "sec-" + id;
  const head = document.createElement("div");
  head.className = "sec-head";
  const caret = document.createElement("span");
  caret.className = "caret";
  const h = document.createElement("h2");
  h.textContent = title;
  head.append(caret, h);
  if (opts.tool) {
    const t = document.createElement("button");
    t.className = "sec-tool";
    t.textContent = opts.tool.label;
    t.addEventListener("click", opts.tool.onClick);
    head.appendChild(t);
  }
  const badge = document.createElement("span");
  badge.className = "badge";
  badge.textContent = opts.badge || "";
  head.appendChild(badge);
  const body = document.createElement("div");
  body.className = "sec-body";
  head.addEventListener("click", (e) => {
    if (e.target.closest(".sec-tool")) return;
    sec.classList.toggle("open");
    const i = state.openSections.indexOf(id);
    if (sec.classList.contains("open")) { if (i < 0) state.openSections.push(id); }
    else if (i >= 0) state.openSections.splice(i, 1);
    saveState();
  });
  sec.append(head, body);
  controls().appendChild(sec);
  return body;
}

/** 网格内的分组小标题 (占满一行, 把不同界面的字段隔开) */
export function addGroupLabel(body, text) {
  const d = document.createElement("div");
  d.className = "grid-group";
  d.textContent = text;
  body.appendChild(d);
}