/* ============================================================================
 * app.js — 启动: 恢复持久化状态 → 构建面板 → 绑定事件 → 首次渲染
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});
  const { $ } = BD.core;
  const state = BD.state;

  function boot() {
    BD.design.loadState();

    // 预览配色 → CSS 变量
    for (const [k, v] of Object.entries(state.PREVIEW_COLORS)) {
      document.documentElement.style.setProperty(k, v);
      if (k === "--btn-bg") document.querySelectorAll(".btn, .btn-icon").forEach(el => el.style.background = v);
    }

    BD.panels.build();
    BD.panels.applyAllTexts();
    BD.panels.updateResNames();
    BD.io.bind();

    // 舞台切换
    $("swSplash").addEventListener("click", () => BD.interactions.showStage("splash"));
    $("swMisayos").addEventListener("click", () => BD.interactions.showStage("misayos"));
    $("swPoulsen").addEventListener("click", () => BD.interactions.showStage("poulsen"));
    $("btnReplay").addEventListener("click", () => BD.interactions.replay(BD.interactions.current()));

    // 加载页 TAP TO START
    $("tapText").addEventListener("click", () => {
      if (BD.interactions.current() !== "splash") return;
      $("splashStage").style.transition = "opacity 1s ease";
      $("splashStage").style.opacity = "0";
      setTimeout(() => { $("splashStage").style.opacity = "1"; BD.interactions.showStage("misayos"); }, 1000);
    });

    // 面板 hover 遮罩
    $("mPanel").addEventListener("mouseenter", () => { $("mDim").style.opacity = "1"; });
    $("mPanel").addEventListener("mouseleave", () => { $("mDim").style.opacity = "0"; });

    // 首次渲染
    BD.preview.refreshPreviews();
    BD.panels.relayout();
    if (state.zoom === 0) BD.interactions.fitStage();
    BD.interactions.showStage("misayos");

    // 字体就绪后重排 (度量更准)
    document.fonts.ready
      .then(() => BD.fonts.loadUploadedFonts())
      .then(() => Promise.all(Object.values(BD.fonts.FONT_SET_NAME).map(f => document.fonts.load('20px "' + f + '"').catch(() => null))))
      .then(() => BD.panels.relayout())
      .catch(() => BD.panels.relayout());
  }

  BD.app = { boot };
  document.addEventListener("DOMContentLoaded", boot);
})();
