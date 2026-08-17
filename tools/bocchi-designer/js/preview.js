/* ============================================================================
 * preview.js — 预览资源刷新 (纹理/SVG/字体/唱片配色)
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});
  const { $, set } = BD.core;
  const { hexToCss } = BD.design;
  const S = BD.design.S;

  function refreshPreviews() {
    const used = BD.design.usedPath;
    $("mTachieImg").src = used("textures", "bocchi");
    $("mWatermark").src = used("textures", "bocchi");
    $("mRecordCover").src = used("textures", "bocchi");
    for (const id of ["mLogo", "pLogo", "splashLogo"]) $(id).querySelector("img").src = used("textures", "logo");
    $("loadingIcon").style.backgroundImage = `url("${used("textures", "bocchi_loading")}")`;
    $("pBgImg").src = used("textures", "gotoh");
    $("pTachie").querySelector("img").src = used("textures", "gotoh");
    $("pBoxAImg").src = used("textures", "gotoh_image_1");
    $("pBoxBImg").src = used("textures", "gotoh_image_2");
    const btnIcons = [["pBtnS", "single"], ["pBtnM", "multi"], ["pBtnO", "option"], ["pBtnLang", "lang"], ["pBtnQuit", "quit"], ["pBtnTheme", "theme"]];
    for (const [id, key] of btnIcons) $(id).querySelector("img").src = used("svgs", key);
    refreshVinyl();
  }

  function refreshVinyl() {
    const rec = $("mRecord");
    if (!rec) return;
    const c = S.colors;
    rec.style.setProperty("--vinyl-base", hexToCss(c.vinyl_base) || "#1A1A1A");
    rec.style.setProperty("--vinyl-edge", hexToCss(c.vinyl_edge) || "#050505");
    rec.style.setProperty("--vinyl-groove", hexToCss(c.vinyl_groove) || "rgba(255,255,255,.12)");
    rec.style.setProperty("--vinyl-shine-1", hexToCss(c.vinyl_shine_1) || "rgba(255,255,255,.2)");
    rec.style.setProperty("--vinyl-shine-2", hexToCss(c.vinyl_shine_2) || "rgba(255,255,255,.1)");
    rec.style.setProperty("--vinyl-shine-3", hexToCss(c.vinyl_shine_3) || "rgba(26,26,26,0)");
    rec.style.setProperty("--vinyl-label", hexToCss(c.vinyl_label) || "rgba(24,26,26,.6)");
  }

  function updateCover() {
    const img = $("mRecordCover");
    if (!img || !img.naturalWidth || !img.naturalHeight) return;
    const OV = BD.state.OV;
    const rs = (+OV.recordSize || BD.facts.value("misayos.recordSize")) - 10;
    const R = rs * 0.48 * 0.65;
    const nw = img.naturalWidth, nh = img.naturalHeight;
    const sw = nw >= nh ? 2 * R * nw / nh : 2 * R;
    const sh = nw >= nh ? 2 * R : 2 * R * nh / nw;
    img.style.width = (1.6 * sw) + "px";
    img.style.height = (1.6 * sh) + "px";
    img.style.transform = `translate(calc(-50% + ${0.3 * sw}px), calc(-50% + ${-(rs - 10) / 4.5 + 0.3 * sh}px))`;
  }
  $("mRecordCover").addEventListener("load", updateCover);

  BD.preview = { refreshPreviews, refreshVinyl, updateCover };
})();
