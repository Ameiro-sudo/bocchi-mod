/* ============================================================================
 * preview.js - 预览资源刷新 (纹理/SVG/字体/唱片配色)
 * ==========================================================================*/
import { $, state } from "./core.js";
import { hexToCss, usedPath, S } from "./design.js";
import { value as factValue } from "./facts.js";

export function refreshPreviews() {
  $("mTachieImg").src = usedPath("textures", "bocchi");
  $("mWatermark").src = usedPath("textures", "bocchi");
  $("mRecordCover").src = usedPath("textures", "bocchi");
  for (const id of ["mLogo", "pLogo", "splashLogo"]) $(id).querySelector("img").src = usedPath("textures", "logo");
  $("loadingIcon").style.backgroundImage = `url("${usedPath("textures", "bocchi_loading")}")`;
  $("pBgImg").src = usedPath("textures", "gotoh");
  $("pTachie").querySelector("img").src = usedPath("textures", "gotoh");
  $("pBoxAImg").src = usedPath("textures", "gotoh_image_1");
  $("pBoxBImg").src = usedPath("textures", "gotoh_image_2");
  const btnIcons = [["pBtnS", "single"], ["pBtnM", "multi"], ["pBtnO", "option"], ["pBtnLang", "lang"], ["pBtnQuit", "quit"], ["pBtnTheme", "theme"]];
  for (const [id, key] of btnIcons) $(id).querySelector("img").src = usedPath("svgs", key);
  refreshVinyl();
}

export function refreshVinyl() {
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

export function updateCover() {
  const img = $("mRecordCover");
  if (!img || !img.naturalWidth || !img.naturalHeight) return;
  const OV = state.OV;
  const rs = (+OV.recordSize || factValue("misayos.recordSize")) - 10;
  const R = rs * 0.48 * 0.65;
  const nw = img.naturalWidth, nh = img.naturalHeight;
  const sw = nw >= nh ? 2 * R * nw / nh : 2 * R;
  const sh = nw >= nh ? 2 * R : 2 * R * nh / nw;
  img.style.width = (1.6 * sw) + "px";
  img.style.height = (1.6 * sh) + "px";
  img.style.transform = `translate(calc(-50% + ${0.3 * sw}px), calc(-50% + ${-(rs - 10) / 4.5 + 0.3 * sh}px))`;
}

/** 唱片封面图加载完成后按比例重铺 (原为模块顶层绑定, 收敛到显式初始化) */
export function initPreview() {
  $("mRecordCover").addEventListener("load", updateCover);
}
