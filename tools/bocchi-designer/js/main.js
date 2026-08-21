/* ============================================================================
 * main.js - 组装根: 模块接线 + 启动序列
 *
 * 依赖注入点 (替代旧 window.BD 全局定位器):
 *   - interactions.hooks.focusText <- panels.focusTextInput
 *   - render.setAfterRelayout      <- interactions.updateSelBox
 *   - io.onModelImported           <- 导入后的 UI 全量同步 (本文件定义)
 * type=module 天然 defer, 执行到这里时 DOM 已就绪。
 * ==========================================================================*/
import { $, state } from "./core.js";
import { loadState } from "./design.js";
import { initPreview, refreshPreviews } from "./preview.js";
import { relayout } from "./render.js";
import { setAfterRelayout } from "./render.js";
import { build, applyAllTexts, updateResNames, focusTextInput } from "./panels.js";
import { bind as bindIO, onModelImported } from "./io.js";
import { showStage, replay, currentStage, fitStage, updateSelBox, hooks } from "./interactions.js";
import { FONT_SET_NAME, loadUploadedFonts } from "./fonts.js";
import { S } from "./design.js";

/** 模型被导入 (zip/design.json) 变更后的 UI 全量同步 */
function syncAllFromModel() {
  applyAllTexts();
  updateResNames();
  const sel = $("themeSel");
  if (sel) sel.value = S.menu.theme;
  refreshPreviews();
  relayout();
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
}

boot();
