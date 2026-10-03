/* ============================================================================
 * main.js - 组装根: 模块接线 + 启动序列
 *
 * 依赖注入点 (替代旧 window.BD 全局定位器):
 *   - interactions.hooks.focusText <- panels.focusTextInput
 *   - render.setAfterRelayout      <- interactions.updateSelBox
 *   - io.onModelImported           <- 导入后的 UI 全量同步 (本文件定义)
 * type=module 天然 defer, 执行到这里时 DOM 已就绪。
 * ==========================================================================*/
import { $, state, toast } from "./core.js";
import { loadState } from "./design.js";
import { syncSlidersFromModel } from "./ov.js";
import { initPreview, refreshPreviews } from "./preview.js";
import { relayout } from "./render.js";
import { setAfterRelayout } from "./render.js";
import { build, applyAllTexts, updateResNames, focusTextInput, resyncBaselines, probeBundledAssets, refreshDirtyMarks } from "./panels.js";
import { bind as bindIO, onModelImported } from "./io.js";
import { showStage, replay, currentStage, fitStage, updateSelBox, hooks } from "./interactions.js";
import { FONT_SET_NAME, loadUploadedFonts } from "./fonts.js";
import { S } from "./design.js";
import { undo as undoHistory, redo as redoHistory, stats as historyStats, push as pushHistory, onStackChange, canUndo, canRedo } from "./history.js";

/* 撤销/重做按钮的可用态。此前 canUndo/canRedo 全工程零调用点, 两个按钮恒亮 ——
   恒亮的按钮等于承诺「按了有反应」, 而按下去只会弹一句「没有可撤销的操作」。
   状态由 history 的栈变化通知驱动, 不靠各写入点自己记得刷新。 */
function syncUndoButtons(canU, canR) {
  $("btnUndo").disabled = !canU;
  $("btnRedo").disabled = !canR;
}

/** 撤销/重做入口: 空栈给提示, 成功回放报 label */
function doUndo() {
  const label = undoHistory();
  if (label == null) toast("没有可撤销的操作");
  else toast("已撤销：" + label);
}
function doRedo() {
  const label = redoHistory();
  if (label == null) toast("没有可重做的操作");
  else toast("已重做：" + label);
}

/** 模型被导入 (zip/design.json) 变更后的 UI 全量同步 */
function syncAllFromModel() {
  applyAllTexts();
  updateResNames();
  resyncBaselines();
  syncSlidersFromModel();
  refreshDirtyMarks();
  const sel = $("themeSel");
  if (sel) sel.value = S.menu.theme;
  refreshPreviews();
  // zip 导入携带的字体 blob 与上传同效: 先注册 FontFace 再重排 (度量才准确)
  loadUploadedFonts()
    .then(() => relayout())
    .catch(() => relayout());
}

function boot() {
  loadState();

  // 预览配色 -> CSS 变量
  for (const [k, v] of Object.entries(state.PREVIEW_COLORS)) {
    document.documentElement.style.setProperty(k, v);
    if (k === "--btn-bg") document.querySelectorAll(".btn, .btn-icon").forEach(el => el.style.background = v);
  }

  build();
  applyAllTexts();
  updateResNames();
  bindIO();
  onModelImported(syncAllFromModel);

  // 接线 (消除旧 BD 全局与模块环)
  hooks.focusText = focusTextInput;
  setAfterRelayout(updateSelBox);

  // 舞台切换
  $("swSplash").addEventListener("click", () => showStage("splash"));
  $("swMisayos").addEventListener("click", () => showStage("misayos"));
  $("swPoulsen").addEventListener("click", () => showStage("poulsen"));
  $("btnReplay").addEventListener("click", () => replay(currentStage()));

  // 撤销/重做: 头部按钮 + 快捷键 (输入框聚焦期间交给浏览器原生撤销, 失焦后走模型栈)
  onStackChange(syncUndoButtons);
  syncUndoButtons(canUndo(), canRedo());
  $("btnUndo").addEventListener("click", doUndo);
  $("btnRedo").addEventListener("click", doRedo);
  window.addEventListener("keydown", (e) => {
    if (!(e.ctrlKey || e.metaKey)) return;
    const k = String(e.key || "").toLowerCase();
    if (k !== "z" && k !== "y") return;
    const tag = document.activeElement ? document.activeElement.tagName.toLowerCase() : "";
    if (/input|textarea|select/.test(tag)) return;
    e.preventDefault();
    if (k === "y" || e.shiftKey) doRedo();   // Ctrl+Y / Ctrl+Shift+Z
    else doUndo();                           // Ctrl+Z
  });

  // 加载页 TAP TO START
  $("tapText").addEventListener("click", () => {
    if (currentStage() !== "splash") return;
    $("splashStage").style.transition = "opacity 1s ease";
    $("splashStage").style.opacity = "0";
    setTimeout(() => { $("splashStage").style.opacity = "1"; showStage("misayos"); }, 1000);
  });

  // 面板 hover 遮罩
  $("mPanel").addEventListener("mouseenter", () => { $("mDim").style.opacity = "1"; });
  $("mPanel").addEventListener("mouseleave", () => { $("mDim").style.opacity = "0"; });

  // 首次渲染
  initPreview();
  refreshPreviews();
  relayout();
  if (state.zoom === 0) fitStage();
  showStage("misayos");

  // 字体就绪后重排 (度量更准)
  document.fonts.ready
    .then(() => loadUploadedFonts())
    .then(() => Promise.all(Object.values(FONT_SET_NAME).map(f => document.fonts.load('20px "' + f + '"').catch(() => null))))
    .then(() => relayout())
    .catch(() => relayout());

  // 内置资源挨个探可达性 (meiryo-bold.ttf 之类的「写着路径但没随工具分发」)。
  // 挂到 __bocchi 上是因为它是异步的: 快照门禁必须等它落定, 否则采到的是
  // 半成品状态, 差异会随机出现又随机消失。
  globalThis.__bocchi.assetsReady = probeBundledAssets();
}

/* 门禁调试出口。挂在 boot() 之前、且只给读取和驱动能力, 不构成一条新的状态
   写入入口 —— 探针走的仍是用户真实路径 (派发 input/change/click)。存在的理由:
   撤销栈从来没有被任何门禁看见过, 于是「改完能不能撤回来」只能靠人肉点。
   见 dev/verify.mjs 的 historyProbe。 */
globalThis.__bocchi = {
  state, S,
  historyStats, pushHistory,
  undo: undoHistory, redo: redoHistory,
};

boot();
