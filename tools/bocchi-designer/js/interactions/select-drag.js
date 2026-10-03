/* ============================================================================
 * select-drag.js - 舞台元素的选中 / 拖拽 / 手柄缩放 / 方向键微调 / 双击定位
 *
 * 可选中对象统一走 SEL 配置表 (移动轴 / 缩放语义 / 显示名), 拖拽与键盘微调共用
 * 一份增量逻辑, 替代旧版三处 switch 的重复分发。
 * ==========================================================================*/
import { $ } from "../core.js";
import { state } from "../core.js";
import { beginGesture, endGesture } from "../history.js";
import { setOV, snapshotOV, pushOVGesture } from "../ov.js";
import { setStatus } from "../status.js";
import { setSelection, clearSelection } from "../bench.js";
import { current, selKey, drag, setSelKey, setDrag, stageStatus } from "./stage-state.js";

/** 舞台双击文本时定位输入框的回调, 由 main.js 接线为 panels.focusTextInput */
export const hooks = {};

/* ---------- 选中配置表 ----------
 * move:  OV键 -> 受影响轴 ; resize.mode:
 *   axisY    仅纵向手柄生效, 增量 = dy*yDir          (立绘高度)
 *   growLeft 仅西向手柄生效, 新值 = start - dx        (面板宽度)
 *   max      取 dx*xDir 与 dy*yDir 较大者            (尺寸类)
 */
const SEL = {
  mTachie: { label: "立绘", move: { tachieX: "x", tachieY: "y" }, resize: { key: "tachieH", mode: "axisY" } },
  mRecord: { label: "唱片", move: { recordX: "x", recordY: "y" }, resize: { key: "recordSize", mode: "max" } },
  title:   { label: "标题组", rectIds: ["mBocchi", "mRock", "mBoxGotoh", "mBoxGirl"], move: { titleX: "x", titleY: "y" }, resize: { key: "titleSize", mode: "max" } },
  mPanel:  { label: "侧栏面板", move: { panelX: "x" }, resize: { key: "panelW", mode: "growLeft" } },
  mBlock1: { label: "背景方块", move: { blockX: "x", blockY: "y" }, resize: { key: "block1", mode: "max" } },
};
// 舞台元素 id -> 选中键 (点击命中映射)
const SEL_HIT = {
  mTachie: "mTachie", mTachieImg: "mTachie", mRecord: "mRecord", mRecordCover: "mRecord",
  mBocchi: "title", mRock: "title", mBoxGotoh: "title", mBoxGirl: "title",
  mPanel: "mPanel", mBlock1: "mBlock1", mStroke1: "mBlock1", mDash1: "mBlock1",
  mDash2: "mBlock1", mRectW: "mBlock1",
};
// 文本元素 -> 可编辑文本 key (dblclick 定位到输入框)
const SEL_TEXT = {
  mBocchi: "mBocchi", mRock: "mRock", mBoxGotoh: "mBoxGotoh", mBoxGirl: "mBoxGirl",
  mPhobia: "mPhobia", mInfo: "mInfo", pTitle: "pTitle", pVer: "pVer", pBranch: "pBranch",
  pCopy1: "pCopy1", pCopy2: "pCopy2",
};

function stageScale() {
  const m = /scale\(([\d.]+)\)/.exec($("stageScale").style.transform || "");
  return m ? parseFloat(m[1]) : 1;
}
function stagePos(e) {
  const r = $("stage").getBoundingClientRect();
  const s = stageScale();
  return { x: (e.clientX - r.left) / s, y: (e.clientY - r.top) / s };
}
function cssRect(el) {
  const r = el.getBoundingClientRect();
  const sr = $("stage").getBoundingClientRect();
  const s = stageScale();
  return { left: (r.left - sr.left) / s, top: (r.top - sr.top) / s, width: r.width / s, height: r.height / s };
}
export function updateSelBox() {
  const box = $("selBox");
  if (!drag) box.style.display = current === "misayos" && selKey ? "block" : "none";
  if (current !== "misayos" || !selKey) {
    clearSelection();
    return;
  }
  let r;
  const cfg = SEL[selKey];
  if (cfg.rectIds) {
    const rects = cfg.rectIds.map(id => cssRect($(id)));
    r = {
      left: Math.min(...rects.map(x => x.left)), top: Math.min(...rects.map(x => x.top)),
      right: Math.max(...rects.map(x => x.left + x.width)), bottom: Math.max(...rects.map(x => x.top + x.height)),
    };
    r = { left: r.left, top: r.top, width: r.right - r.left, height: r.bottom - r.top };
  } else {
    r = cssRect($(selKey));
  }
  box.style.cssText = `display:block;left:${r.left - 2}px;top:${r.top - 2}px;width:${r.width + 4}px;height:${r.height + 4}px;`;
  /* 校准台读数: 面板滑杆写的是意图, 这里报的是实测 —— 拖动/缩放过程中实时跟着变。 */
  setSelection(cfg.label, r.width, r.height, r.left, r.top);
  setStatus(
    `<span class="sel-chip">${cfg.label}</span> 已选中 · 拖动移动 · 拖角缩放 · 方向键微调 (Shift×10) · Esc 取消`);
}

/** 按 SEL 表把位移/缩放增量落到 OV (拖拽用绝对起点, 键盘用当前值+步长) */
function applyDelta(cfg, base, dx, dy, handle) {
  for (const [key, axis] of Object.entries(cfg.move)) {
    setOV(key, base[key] + (axis === "x" ? dx : dy));
  }
  if (!cfg.resize) return;
  const rz = cfg.resize;
  if (rz.mode === "axisY") {
    const yDir = handle.includes("s") ? 1 : handle.includes("n") ? -1 : 0;
    if (yDir) setOV(rz.key, base[rz.key] + dy * yDir);
  } else if (rz.mode === "growLeft") {
    const xDir = handle.includes("e") ? 1 : handle.includes("w") ? -1 : 0;
    if (xDir < 0) setOV(rz.key, base[rz.key] - dx);
  } else { // max
    const xDir = handle.includes("e") ? 1 : handle.includes("w") ? -1 : 0;
    const yDir = handle.includes("s") ? 1 : handle.includes("n") ? -1 : 0;
    setOV(rz.key, base[rz.key] + Math.max(dx * xDir, dy * yDir));
  }
}

function clearSel() {
  if (!selKey) return;
  setSelKey(null);
  updateSelBox();
  setStatus(stageStatus());
}

/** 切舞台时清空选中与拖拽态。放在这里而不是让 stage-view 去写 stage-state 的字段,
 *  是因为「拖到一半切了舞台」这件事只有选中模块知道该怎么收尾 (含 endGesture)。 */
export function resetSelection() {
  setSelKey(null);
  setDrag(null);
  $("selBox").style.display = "none";
  clearSelection();
}

/** 当前 OV 值视图 (缺省 0), 供键盘微调当"绝对起点"使用 */
function currentOV() {
  const out = {};
  for (const k of Object.keys(state.OV)) out[k] = +state.OV[k] || 0;
  return out;
}

export function bindSelectDrag() {
  $("stageScale").addEventListener("mousedown", e => {
    if (current !== "misayos") return;
    const handleEl = e.target.closest("#selBox i");
    const mover = e.target.closest(".sel-move");
    if ((handleEl || mover) && selKey) {
      e.preventDefault();
      const handle = handleEl && handleEl.dataset.h;
      setDrag({ mode: handle ? "resize" : "move", handle, start: stagePos(e), startOV: snapshotOV() });
      beginGesture();   // 静音区: 逐帧 setOV 不入栈, 收尾时合成一条完整命令
      return;
    }
    const t = e.target.closest("[id]");
    const key = t && SEL_HIT[t.id];
    if (key && key === selKey) {
      // 已选中: 直接开始拖拽
      e.preventDefault();
      setDrag({ mode: "move", handle: null, start: stagePos(e), startOV: snapshotOV() });
      beginGesture();
      return;
    }
    setSelKey(key || null);
    updateSelBox();
  });

  window.addEventListener("mousemove", e => {
    if (!drag || !selKey) return;
    const p = stagePos(e);
    applyDelta(SEL[selKey], drag.startOV, p.x - drag.start.x, p.y - drag.start.y, drag.handle || "");
  });
  window.addEventListener("mouseup", () => {
    if (!drag) return;
    const label = (drag.handle ? "缩放 " : "拖拽 ") + SEL[selKey].label;
    endGesture();                              // 先关静音区
    pushOVGesture(label, drag.startOV);        // 起止快照合成一条命令 (无位移则不留痕)
    setDrag(null);
    updateSelBox();
  });
  /* 兜底: 按下后在窗口外松开 (切到别的程序、拖到别的窗口上), mouseup 收不到,
   * 手势就永远开着 —— 而静音区是全局开关, 一旦泄漏, 之后整页再不入撤销栈, 表面
   * 上毫无征兆。所以再加一道: 窗口失焦即视为手势结束。 */
  window.addEventListener("blur", () => { if (drag) { setDrag(null); endGesture(); updateSelBox(); } });

  // 双击文本元素 -> 定位到编辑框
  $("stage").addEventListener("dblclick", e => {
    const t = e.target.closest("[id]");
    if (!t || current !== "misayos") return;
    const textKey = t && SEL_TEXT[t.id];
    if (textKey && hooks.focusText && hooks.focusText(textKey)) {
      e.stopPropagation();
    }
  });

  // 键盘微调 / Esc
  window.addEventListener("keydown", e => {
    if (!selKey || current !== "misayos") return;
    if (document.activeElement && /input|textarea|select/.test(document.activeElement.tagName.toLowerCase())) return;
    const step = e.shiftKey ? 10 : 1;
    const k = e.key;
    if (k === "ArrowLeft") { applyDelta(SEL[selKey], currentOV(), -step, 0, ""); e.preventDefault(); }
    else if (k === "ArrowRight") { applyDelta(SEL[selKey], currentOV(), step, 0, ""); e.preventDefault(); }
    else if (k === "ArrowUp") { applyDelta(SEL[selKey], currentOV(), 0, -step, ""); e.preventDefault(); }
    else if (k === "ArrowDown") { applyDelta(SEL[selKey], currentOV(), 0, step, ""); e.preventDefault(); }
    else if (k === "Escape") { clearSel(); }
  });

  // L3: 阻止舞台内图片被浏览器原生拖拽
  $("stage").addEventListener("dragstart", e => { if (e.target.closest("[data-sel]")) e.preventDefault(); });
}