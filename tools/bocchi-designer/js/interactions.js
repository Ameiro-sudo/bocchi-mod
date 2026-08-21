/* ============================================================================
 * interactions.js - 舞台切换 / 入场动画 / 加载页演示 / 选中拖拽 / 键盘微调 / 缩放
 *
 * 可选中对象统一走 SEL 配置表 (移动轴 / 缩放语义 / 显示名), 拖拽与键盘微调共用
 * 一份增量逻辑, 替代旧版三处 switch 的重复分发。
 * ==========================================================================*/
import { $, set } from "./core.js";
import { state } from "./core.js";
import { saveState } from "./design.js";
import { W, H, value as factValue } from "./facts.js";
import { setStatus } from "./status.js";
import { setOV, snapshotOV } from "./ov.js";

/** 舞台双击文本时定位输入框的回调, 由 main.js 接线为 panels.focusTextInput */
export const hooks = {};

/* ---------- 舞台切换 ---------- */
let current = "misayos";
function showStage(name) {
  current = name;
  selKey = null;
  drag = null;
  $("selBox").style.display = "none";
  $("splashStage").style.display = name === "splash" ? "block" : "none";
  $("misayosStage").style.display = name === "misayos" ? "block" : "none";
  $("poulsenStage").style.display = name === "poulsen" ? "block" : "none";
  $("swSplash").classList.toggle("active", name === "splash");
  $("swMisayos").classList.toggle("active", name === "misayos");
  $("swPoulsen").classList.toggle("active", name === "poulsen");
  const names = {
    splash: "加载页（点击 TAP TO START）",
    misayos: "主菜单 misayos",
    poulsen: "主菜单 poulsen",
  };
  setStatus("<b>" + names[name] + "</b> · 1:1 预览 · 1280×720 设计分辨率" + (name === "misayos" ? " · 点击元素可选中拖拽" : ""));
  if (name === "splash") startSplashDemo();
  else { clearInterval(splashTimer); replay(name); } // L6: 离开加载页清理演示定时器
  if (name === "misayos") startAmbient();
  else stopAmbient(); // M4: 离开 misayos 停止常驻动画, 不再空转
}

/* ---------- 入场动画重播 ---------- */
function replay(name) {
  const map = {
    misayos: ["mBlock1", "mStroke1", "mBlock3", "mTachie", "mRecord", "mInfo", "mPhobia", "mBocchi", "mRock", "mBoxGotoh", "mBoxGirl", "mLogo", "mPanel"],
    poulsen: ["pGoto1", "pGoto2", "pHitoriTop", "pHitoriBottom", "pSq1", "pSq2", "pSq3", "pBar", "pBoxA", "pBoxB", "pJName", "pJKana", "pAddInfo", "pAlias", "pTachie", "pDots", "pDot1", "pDot2", "pDot3", "pLogo", "pBtnSingle", "pBtnMulti", "pBtnOptions", "pBtnQuitP", "pBtnMisayos"],
  };
  const ids = map[name] || [];
  ids.forEach(id => {
    const el = $(id);
    if (el) { el.style.transition = "none"; el.style.opacity = "0"; }
  });
  requestAnimationFrame(() => requestAnimationFrame(() => {
    const ease = "cubic-bezier(.33,1,.68,1)";
    ids.forEach((id, i) => {
      const el = $(id);
      if (el) { el.style.transition = `opacity 700ms ${ease} ${i * 40}ms`; el.style.opacity = "1"; }
    });
  }));
}

/* ---------- 加载页演示 ---------- */
let splashTimer = null;
function startSplashDemo() {
  clearInterval(splashTimer);
  $("progressFill").style.width = "0%";
  let p = 0;
  splashTimer = setInterval(() => {
    p = Math.min(1, p + 0.012);
    $("progressFill").style.width = (p * 100) + "%";
    const frame = Math.floor(p * 20) % 20;
    $("loadingIcon").style.backgroundPosition = `-${frame * factValue("splash.loadingFrameW")}px 0`;
    if (p >= 1) clearInterval(splashTimer);
  }, 50);
}

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
let selKey = null;
let drag = null;

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
function updateSelBox() {
  const box = $("selBox");
  if (!drag) box.style.display = current === "misayos" && selKey ? "block" : "none";
  if (current !== "misayos" || !selKey) return;
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
  selKey = null;
  updateSelBox();
  setStatus("");
}

$("stageScale").addEventListener("mousedown", e => {
  if (current !== "misayos") return;
  const handleEl = e.target.closest("#selBox i");
  const mover = e.target.closest(".sel-move");
  if ((handleEl || mover) && selKey) {
    e.preventDefault();
    const handle = handleEl && handleEl.dataset.h;
    drag = { mode: handle ? "resize" : "move", handle, start: stagePos(e), startOV: snapshotOV() };
    return;
  }
  const t = e.target.closest("[id]");
  const key = t && SEL_HIT[t.id];
  if (key && key === selKey) {
    // 已选中: 直接开始拖拽
    e.preventDefault();
    drag = { mode: "move", handle: null, start: stagePos(e), startOV: snapshotOV() };
    return;
  }
  selKey = key || null;
  updateSelBox();
});

window.addEventListener("mousemove", e => {
  if (!drag || !selKey) return;
  const p = stagePos(e);
  applyDelta(SEL[selKey], drag.startOV, p.x - drag.start.x, p.y - drag.start.y, drag.handle || "");
});
window.addEventListener("mouseup", () => { drag = null; updateSelBox(); });

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
/** 当前 OV 值视图 (缺省 0), 供键盘微调当"绝对起点"使用 */
function currentOV() {
  const out = {};
  for (const k of Object.keys(state.OV)) out[k] = +state.OV[k] || 0;
  return out;
}

// 常驻动画: 立绘呼吸 + 唱片旋转 (M4: 仅在 misayos 舞台运行, 切换后清理)
let ambientTimer = null;
function startAmbient() {
  stopAmbient();
  ambientTimer = setInterval(() => {
    const t = Date.now() / 1000;
    const breath = Math.sin(t * 2 * Math.PI / 3) * 3;
    const base = +($("mTachie").dataset.baseRot || 0);
    $("mTachie").style.transform = `rotate(${base - (breath + 1.5) / 3}deg)`;
    $("mRecord").style.transform = `rotate(${(t * 20) % 360}deg)`;
  }, 33);
}
function stopAmbient() {
  if (ambientTimer) { clearInterval(ambientTimer); ambientTimer = null; }
}

// L3: 阻止舞台内图片被浏览器原生拖拽
$("stage").addEventListener("dragstart", e => { if (e.target.closest("[data-sel]")) e.preventDefault(); });

/* ---------- 缩放控制 ---------- */
function fitStage() {
  const wrap = $("stageScale").closest(".preview-wrap");
  const scale = Math.min((wrap.clientWidth - 44) / W, (wrap.clientHeight - 56) / H);
  applyScale(scale, true);
}
function applyScale(scale, fromFit) {
  $("stageScale").style.transform = `scale(${scale})`;
  const buttons = document.querySelectorAll(".seg button[data-zoom]");
  for (const b of buttons) b.classList.toggle("on", b.dataset.zoom == (fromFit ? "0" : state.zoom));
}
function setZoom(z) {
  state.zoom = z;
  saveState();
  if (z === 0) fitStage();
  else applyScale(z);
}
document.querySelectorAll(".seg button[data-zoom]").forEach(b => {
  b.addEventListener("click", () => setZoom(+b.dataset.zoom));
});
window.addEventListener("resize", () => {
  if (state.zoom === 0) fitStage();
});

export {
  showStage, replay, startSplashDemo, updateSelBox, fitStage, startAmbient, stopAmbient,
};
/** 当前舞台名 / 当前选中键 (main.js 组装期使用) */
export const currentStage = () => current;
export const activeSelKey = () => selKey;
export function setSel(k) { selKey = k; updateSelBox(); }


