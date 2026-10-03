/* ============================================================================
 * ui/text.js - 文案编辑行 (与舞台元素双向关联)
 *
 * input 与舞台元素双向关联: 点舞台元素可定位到对应输入框
 * ==========================================================================*/
import { $, escapeHtml } from "../core.js";
import { state } from "../core.js";
import { DEFAULT_TEXTS, scheduleSave } from "../design.js";
import { scheduleRelayout } from "../render.js";
import { push as pushHistory } from "../history.js";
import { markDirty, scheduleDirty } from "./dirty.js";
import { registerBaseline } from "./baseline.js";

const TEXT_INPUTS = {};

/** 文本模型落地函数 (输入/入栈/回放共用); 输入框聚焦时不回写 value 防光标跳动 */
function setTextModel(elId, v, input) {
  state.TEXTS[elId] = v;
  if (document.activeElement !== input) input.value = v;
  applyText(elId, v);
  scheduleRelayout();
  scheduleSave();
  scheduleDirty();
}

export function addTextRow(body, label, elId) {
  const row = document.createElement("div");
  row.className = "text-row";
  const lab = document.createElement("div");
  lab.className = "t-label"; lab.textContent = label;
  const dot = document.createElement("span");
  dot.className = "t-dot"; dot.title = "在舞台上双击对应元素可定位到此处";
  // 舞台上没有对应元素的键 (如设置界面的 sTitle/sDone) 不显示定位点 ——
  // 画一个点却永远双击不到, 比不画更让人以为是自己没找对
  if (!$(elId)) dot.style.display = "none";
  const input = document.createElement("input");
  input.type = "text";
  input.value = state.TEXTS[elId] != null ? state.TEXTS[elId] : DEFAULT_TEXTS[elId];
  // 撤销基准: 最近入栈值 -> change (失焦/回车) 时与最新值比对入栈
  const rec = { input, row, committed: input.value };
  input.addEventListener("change", () => {
    const to = input.value;
    if (to === rec.committed) return;
    const from = rec.committed;
    // 回放时必须同步搬动基准, 否则基准停在被撤销掉的那个值上: 下一次失焦会拿
    // 它当 from, 生成一条「从没发生过」的撤销条目, 撤销次数凭空多一次。
    const undo = () => { rec.committed = from; setTextModel(elId, from, input); };
    const redo = () => { rec.committed = to; setTextModel(elId, to, input); };
    pushHistory({ label: `文本 ${label}`, undo, redo });
    rec.committed = to;
  });
  input.addEventListener("input", () => {
    state.TEXTS[elId] = input.value;
    applyText(elId, input.value);
    // 这里此前是裸 relayout() + 裸 saveState(): 每敲一键就同步重排三个舞台 (含字体
    // 度量) 并同步写一遍 localStorage。文字越长的字段卡得越明显。
    // scheduleRelayout 是 rAF 节流 (一帧一次), scheduleSave 是 250ms 防抖; 关页前
    // design.js 的 beforeunload 会 flush, 所以不丢数据。
    scheduleRelayout();
    scheduleSave();
    scheduleDirty();
  });
  row.append(dot, lab, input);
  body.appendChild(row);
  TEXT_INPUTS[elId] = rec;
  registerBaseline(() => { rec.committed = state.TEXTS[elId]; });
  markDirty(row, () => (state.TEXTS[elId] != null ? state.TEXTS[elId] : DEFAULT_TEXTS[elId]) !== DEFAULT_TEXTS[elId]);
}

const INNER_HTML_IDS = new Set(["mPhobia", "mInfo", "pJKana", "pCopy1", "pCopy2"]);

// L7: 仅放行 <br>, 其余标签/脚本转义 (escapeHtml 见 core.js), 消除自我 XSS 面
function applyText(elId, html) {
  const el = $(elId);
  if (!el) return;
  if (INNER_HTML_IDS.has(elId)) {
    el.innerHTML = String(html).split(/\s*<br\s*\/?>\s*/i).map(escapeHtml).join("<br>");
  } else el.textContent = html;
}

/** 从舞台元素反查文本输入框并高亮 (双击舞台文本时调用) */
export function focusTextInput(elId) {
  const rec = TEXT_INPUTS[elId];
  if (!rec) return false;
  rec.row.scrollIntoView({ behavior: "smooth", block: "center" });
  rec.input.classList.add("flash");
  setTimeout(() => rec.input.classList.remove("flash"), 1200);
  rec.input.focus();
  return true;
}

export function applyAllTexts() {
  for (const elId of Object.keys(DEFAULT_TEXTS)) {
    const v = state.TEXTS[elId] != null ? state.TEXTS[elId] : DEFAULT_TEXTS[elId];
    applyText(elId, v);
    const rec = TEXT_INPUTS[elId];
    if (rec) {
      if (document.activeElement !== rec.input) rec.input.value = v;
      rec.committed = v;   // 模型被导入整体替换后, 撤销基准一并重置
    }
  }
}