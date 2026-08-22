/* ============================================================================
 * layout.js - 三个舞台的布局计算
 *
 * 所有"魔法数字"均取自 js/facts.js (factValue("组.名")), 修改 Java 端布局后运行
 * sync/check-layout.py 即可发现漂移。带 webOnly 注记的为预览端推导,
 * Java 无直接对应公式 (通常来自美术稿/字形度量)。
 * ==========================================================================*/
import { $, set, state } from "./core.js";
import { gh, cssTop, tw } from "./fonts.js";
import * as facts from "./facts.js";
import { updateCover } from "./preview.js";

const factValue = facts.value;
const OV = state.OV;

const FM = { heavy: "SH Heavy", light: "SH Light", regular: "SH Regular", normal: "SH Normal", bold: "SH Bold", rb: "Radikal Black" };

/* ================= 加载页 (SplashUI.java) ================= */
export function splash() {
  const W = facts.W, H = facts.H;
  const spacing = factValue("splash.spacing");            // SplashFrameContext.java:18
  const barW = factValue("splash.barW"), barH = factValue("splash.barFontH"); // :162 / SplashUI.java:58
  const barX = factValue("splash.barX");                  // SplashUI.java:160
  // 加载页常驻显示的是 LOGO 初始态 (居中, 进度条上方); tLogo 左上角小标是淡出过渡终态
  const logoW = factValue("splash.iLogoW"), logoH = factValue("splash.iLogoH");
  const logoX = factValue("splash.iLogoX"), logoY = factValue("splash.iLogoY");

  set($("splashLogo"), `left:${logoX}px;top:${logoY}px;width:${logoW}px;height:${logoH}px;`);
  set($("progressTrack"), `left:${barX}px;top:${H / 2 + spacing + barH / 2}px;width:${barW}px;`);
  set($("loadingIcon"),
    `left:${barX + barW - barH * 1.5}px;top:${H / 2 + spacing + barH / 2 - barH * 1.5}px;` +
    `width:${barH * 2.5}px;height:${barH * 2.5}px;background-size:${barH * 2.5 * 20}px ${barH * 2.5}px;`);
  set($("tapText"),
    `left:${W / 2 - tw("TAP TO START", factValue("splash.tapFont") / 2, "Radikal Regular") / 2}px;` +
    `top:${H / 2 + spacing + barH + 12}px;font-size:${factValue("splash.tapFont") / 2}px;`);
}

/* ================= misayos ================= */
export function misayos() {
  const W = facts.W, H = facts.H;
  // 背景方块 (MainMenuMisayosFrameContext)
  const b1s = +OV.block1 || factValue("misayos.block1Size");
  const b3s = factValue("misayos.block3Size");
  const b1x = factValue("misayos.block1X") + (+OV.blockX || 0);
  const b1y = factValue("misayos.block1Y") + (+OV.blockY || 0);
  const b3x = factValue("misayos.block3X"), b3y = factValue("misayos.block3Y");
  // 面板比例 (webOnly: 按美术稿 197.1 基准缩放)
  const sc = b3s / 197.1;
  const ts = +OV.titleSize || 53;                          // 标题字号 (预览默认 53, webOnly)
  const tF = ts / 2;

  set($("mBlock1"), `left:${b1x}px;top:${b1y}px;width:${b1s}px;height:${b1s}px;`);
  set($("mStroke1"), `left:${b1x}px;top:${b1y - 1}px;width:${b1s}px;`);  // BgRectsComponent:69-74
  set($("mBlock3"), `left:${b3x}px;top:${b3y}px;width:${b3s}px;height:${b3s}px;`);
  set($("mWatermark"), `left:${b3x}px;top:${b3y}px;width:${b3s}px;height:${b3s}px;`);
  set($("mDash1"), `left:${factValue("misayos.dash1X")}px;top:${factValue("misayos.dash1Y")}px;width:${factValue("misayos.dash1EndX") - factValue("misayos.dash1X")}px;`);
  set($("mDash2"), `left:0;top:${factValue("misayos.dash2Y")}px;width:${factValue("misayos.dash2EndX")}px;`); // TextElementsComponent:80,86
  set($("mRectW"), `left:${factValue("misayos.rectWX")}px;top:${b1y - 5}px;width:${factValue("misayos.rectWW")}px;height:5px;`); // TextElementsComponent:89-96

  // 立绘 (MainTachieComponent:68-71)
  const th = +OV.tachieH || factValue("misayos.tachieH");
  const twd = th * (factValue("misayos.tachieW") / factValue("misayos.tachieH"));  // 宽高比 1.035483870967742
  set($("mTachie"),
    `left:${factValue("misayos.tachieX") + (+OV.tachieX || 0)}px;top:${factValue("misayos.tachieY") + (+OV.tachieY || 0)}px;` +
    `width:${twd}px;height:${th}px;opacity:${(+OV.tachieOp ?? 100) / 100};`);
  $("mTachie").dataset.baseRot = +OV.tachieRot || 0;

  // 唱片 (MainTachieComponent:94)
  const rs = +OV.recordSize || factValue("misayos.recordSize");
  set($("mRecord"),
    `left:${factValue("misayos.recordX") + 5 + (+OV.recordX || 0)}px;top:${factValue("misayos.recordY") + 5 + (+OV.recordY || 0)}px;` +
    `width:${rs - 10}px;height:${rs - 10}px;`);
  updateCover();

  // 简介文字 (TextElementsComponent:61-74)
  const infoY1 = factValue("misayos.infoY") - gh(6.5, FM.light) / 2;
  set($("mInfo"),
    `left:${factValue("misayos.infoX")}px;top:${cssTop(infoY1, 6.5, FM.light)}px;font-size:3.25px;` +
    `line-height:${gh(6.5, FM.light) + 1}px;`);
  set($("mPhobia"),
    `left:${factValue("misayos.phobiaX")}px;top:${cssTop(factValue("misayos.phobiaY"), 8.5, FM.regular)}px;` +
    `font-size:4.25px;letter-spacing:${factValue("misayos.decorateSpacing")}px;`);

  // 大标题 (TextElementsComponent, 字号/字距 webOnly)
  const wB = tw("B", tF, FM.heavy), wT = tw("T", tF, FM.heavy);
  const tx = +OV.titleX || 0;
  const bocchiX = b1x - wB * 1.2 + tx, rockX = b1x - wT / 2 + tx;
  const ty = +OV.titleY || 0;
  const bocchiY = b1y + gh(ts, FM.heavy) - 5, rockY = b1y + gh(ts, FM.heavy) * 2;
  set($("mBocchi"), `left:${bocchiX}px;top:${cssTop(bocchiY, ts, FM.heavy) + ty}px;font-size:${tF}px;letter-spacing:-1.5px;`);
  set($("mRock"), `left:${rockX}px;top:${cssTop(rockY, ts, FM.heavy) + ty}px;font-size:${tF}px;letter-spacing:-1.5px;`);

  const gotohX = rockX + (tw("THE RO", tF, FM.heavy, -1.5) + tw("THE ROC", tF, FM.heavy, -1.5)) / 2 - tw("O", tF, FM.heavy) / 2;
  const gotohY = rockY + gh(ts, FM.heavy) * 1.25;
  const gotohCW = tw("Gotoh Hitori", 6.5, FM.regular, -0.35);
  set($("mBoxGotoh"),
    `left:${gotohX - gotohCW * 0.3 / 2}px;top:${gotohY - gh(13, FM.regular) * 0.4 / 2}px;` +
    `width:${gotohCW * 1.3}px;height:${gh(13, FM.regular) * 1.4}px;font-size:6.5px;` +
    `line-height:${gh(13, FM.regular) * 1.4}px;text-align:center;`);
  const girlX = rockX + tw("THE ROC", tF, FM.heavy, -1.5);
  const girlY = rockY - gh(ts, FM.heavy) / 4;
  const girlCW = tw("A reclusive girl", 4.75, FM.normal, -0.35);
  set($("mBoxGirl"),
    `left:${girlX - girlCW * 0.1 / 2}px;top:${girlY - gh(9.5, FM.normal) * 0.8 / 2}px;` +
    `width:${girlCW * 1.1}px;height:${gh(9.5, FM.normal) * 1.8}px;font-size:4.75px;` +
    `line-height:${gh(9.5, FM.normal) * 1.8}px;text-align:center;`);

  set($("mLogo"), `left:${factValue("splash.logoX")}px;top:${H * 0.1}px;width:${factValue("splash.logoW")}px;height:${factValue("splash.logoH")}px;`);

  // 侧栏面板 (GuiComponent)
  const pw = (+OV.panelW || 95) * sc, ph = 167.6 * sc;     // 95/167.6 webOnly (美术稿比例)
  const px = W - pw + (+OV.panelX || 0), py = (H - ph) / 2;
  set($("mPanel"), `left:${px}px;top:${py}px;width:${pw}px;height:${ph}px;`);
  const s = sc;
  const t21 = factValue("misayos.titleFontSize");          // GuiComponent.java:241
  const bw = tw("BOCCHI", t21 / 2, FM.heavy, -0.1);
  const tY = gh(t21, FM.heavy) + 3;
  set($("pTitle"), `left:${bw / 2}px;top:${cssTop(tY, t21, FM.heavy)}px;font-size:${t21 / 2}px;letter-spacing:-.1px;`);
  const vY = tY + gh(t21, FM.heavy) / 2 + gh(10, FM.regular) / 2;
  set($("pVer"), `left:${bw / 2 + bw + 5}px;top:${cssTop(vY, 10, FM.regular) - 1.5}px;font-size:5px;padding:1px 2px;`);
  set($("pLine"), `left:${bw / 2 - 2.5}px;top:${tY + gh(t21, FM.heavy) + 2.5}px;width:${bw + 5}px;`);
  const aY = tY + gh(t21, FM.heavy) + gh(11, FM.heavy) / 2 + 4;
  set($("pBranch"), `left:${bw / 2}px;top:${cssTop(aY, 11, FM.heavy)}px;font-size:5.5px;letter-spacing:-.1px;`);

  const btnW = 75 * s, btnH = 15 * s, btnX = 5 * s + (pw - 5 * s - btnW) / 2;
  const btnGap = factValue("misayos.btnGap") * s;          // GuiComponent.java:126
  const y1 = (factValue("misayos.btnStartY") + factValue("misayos.btnYOffset")) * s; // 30+10 -> :120,:123
  const y2 = y1 + btnGap, y3 = y2 + btnGap;
  const iconS = 15 * s, iconSp = 1 * s;
  const aw = btnW - iconSp - iconS * 2 - iconSp;
  const btnFont = `${9.5 * s / 2}px`;
  set($("pBtnS"), `left:${btnX}px;top:${y1}px;width:${btnW}px;height:${btnH}px;font-size:${btnFont};`);
  set($("pBtnM"), `left:${btnX}px;top:${y2}px;width:${btnW}px;height:${btnH}px;font-size:${btnFont};`);
  set($("pBtnO"), `left:${btnX}px;top:${y3}px;width:${aw}px;height:${btnH}px;font-size:${btnFont};`);
  set($("pBtnLang"), `left:${btnX + aw + iconSp}px;top:${y3}px;width:${iconS}px;height:${iconS}px;`);
  set($("pBtnQuit"), `left:${btnX + aw + iconSp + iconS + iconSp}px;top:${y3}px;width:${iconS}px;height:${iconS}px;`);
  set($("pBtnTheme"), `left:${btnX}px;top:${ph - 28 * s}px;width:${btnW}px;height:${btnH}px;font-size:${btnFont};`); // 28 webOnly (GuiComponent:164 推导)
  set($("pFooter"), `left:${5 + 3}px;top:${ph * factValue("misayos.panelFooterY")}px;width:${pw - 10 - 6}px;`); // GuiComponent:272
  const c1Y = ph - gh(7, FM.normal) * 3 - 5, c2Y = ph - gh(7, FM.normal) * 2 - 2.5;
  const c1W = tw("Bocchi Client    Version - 1.0", 3.5, FM.normal), c2W = tw("@COPYRIGHT MISAYO", 3.5, FM.normal);
  set($("pCopy1"), `left:${5 + (pw - 5 - c1W) / 2}px;top:${cssTop(c1Y, 7, FM.normal)}px;font-size:3.5px;`);
  set($("pCopy2"), `left:${5 + (pw - 5 - c2W) / 2}px;top:${cssTop(c2Y, 7, FM.normal)}px;font-size:3.5px;`);
}

/* ================= poulsen ================= */
export function poulsen() {
  const W = facts.W, H = facts.H;
  const S0 = factValue("poulsen.bgFontSize");              // MainMenuPoulsenFrameContext:15
  const F0 = S0 / 2, gh0 = gh(S0, FM.rb), half0 = gh0 / 2;
  const r1x = factValue("poulsen.rect1X"), r1w = factValue("poulsen.rect1W"); // MainMenuPoulsenFrameContext:12-13
  const spacing = factValue("poulsen.gotoSpacing");        // ImagesBlockComponent:41

  set($("pRectPink"), `left:${r1x}px;top:0;width:${r1w}px;height:${H}px;`);
  set($("pRectLpink"), `left:${r1x + r1w}px;top:0;width:${W - r1x - r1w}px;height:${H}px;`);
  const bgW = factValue("poulsen.bgImgW"), bgH = bgW * 3.51;       // BgImageComponent:40; *3.51 源图比例 webOnly
  set($("pBgImg"), `left:${factValue("poulsen.bgImgX")}px;top:${-bgH / 30}px;width:${bgW}px;height:${bgH}px;`);
  set($("pStrip"), `left:${r1x + r1w}px;top:${factValue("poulsen.stripY")}px;width:${W - r1x - r1w}px;height:${factValue("poulsen.stripH")}px;`);

  const gW = tw("GOTO", F0, FM.rb);
  set($("pGoto1"), `left:${r1x - half0 / 15}px;top:${cssTop(half0 / 2 + spacing, S0, FM.rb)}px;font-size:${F0}px;color:#FF86C0;`);
  set($("pGoto2"), `left:${r1x - half0 / 15}px;top:${cssTop(half0 / 2 + spacing + gh0 * 1.25 + spacing, S0, FM.rb)}px;font-size:${F0}px;color:#E95A9F;`);

  const hF = factValue("poulsen.hitoriFont"), hh = gh(hF, FM.rb) / 2; // BigFirstNameComponent:32,37
  set($("pHitoriTop"),
    `left:${r1x * 0.5 - tw("HITORI", hF / 2, FM.rb) / 2}px;top:${cssTop(factValue("poulsen.hitoriY") + hh, hF, FM.rb)}px;` +
    `font-size:${hF / 2}px;color:#E95A9F;opacity:.93;`);
  set($("pHitoriBottom"),
    `left:${r1x - half0 / 4.5}px;top:${cssTop(factValue("poulsen.stripY") + factValue("poulsen.stripH") + half0 / 2.35, S0, FM.rb)}px;` +
    `font-size:${F0}px;color:#FF86C0;`);

  const sqY = H * 0.65 - 7.5, sqCX = r1x * 0.75 - 7.5;     // webOnly (美术稿点位)
  set($("pSq1"), `left:${sqCX - 20}px;top:${sqY}px;width:15px;height:15px;`);
  set($("pSq2"), `left:${sqCX}px;top:${sqY}px;width:15px;height:15px;`);
  set($("pSq3"), `left:${sqCX + 20}px;top:${sqY}px;width:15px;height:15px;`);

  const fontPosY = half0 / 2 + spacing;
  const barW = gh0 * 1.25, barX = r1x + gW * 0.86 - barW / 2, barY = fontPosY + gh0; // ImagesBlockComponent:47
  set($("pBar"), `left:${barX}px;top:${barY}px;width:${barW}px;height:${H * 0.043}px;`);
  const bs = factValue("poulsen.square"), bImg = factValue("poulsen.squareImg"); // ImagesBlockComponent:50,:101
  set($("pBoxA"), `left:${barX + barW}px;top:${fontPosY}px;width:${bs}px;height:${bs}px;`);
  set($("pBoxAImg"), `left:${(bs - bImg) / 2}px;top:${(bs - bImg) / 2}px;width:${bImg}px;height:${bImg}px;`);
  set($("pBoxB"), `left:${barX + barW}px;top:${barY + H * 0.043 - bs}px;width:${bs}px;height:${bs}px;`);
  set($("pBoxBImg"), `left:${(bs - bImg) / 2}px;top:${(bs - bImg) / 2}px;width:${bImg}px;height:${bImg}px;`);

  const nS = factValue("poulsen.nameFont"), nF = nS / 2;            // JapaneseNamesComponent:35
  const nW = tw("後藤 ひとり", nF, "Meiryo");
  const nTopY = factValue("poulsen.nameY") - gh(nS, "Meiryo") / 2; // :42
  set($("pJName"), `left:${r1x * 0.5 - nW / 2}px;top:${cssTop(nTopY, nS, "Meiryo")}px;font-size:${nF}px;color:#E95A9F;`);
  const kS = factValue("poulsen.kanaFont");                        // :36
  set($("pJKana"),
    `left:${r1x * 0.5 - nW / 2 + nW * 0.07}px;top:${cssTop(nTopY - gh(nS, "Meiryo") - gh(kS, "Meiryo") * 1.25, kS, "Meiryo")}px;` +
    `font-size:${kS / 2}px;color:#000;`);

  const aS = factValue("poulsen.aliasFont"), aF = aS / 2, aX = r1x + r1w - half0; // AliasTextComponent:35
  set($("pAlias"),
    `left:${aX}px;top:${cssTop(factValue("poulsen.aliasY"), aS, "Meiryo")}px;font-size:${aF}px;` +
    `transform:scaleX(${factValue("poulsen.aliasScaleX")});transform-origin:center;`);
  const qS = factValue("poulsen.quoteFont");                       // :36
  set($("pAliasQ1"), `position:relative;display:inline;font-size:${qS / 2}px;top:${-(gh(aS, "Meiryo") + gh(qS, "Meiryo") / 2)}px;`);
  set($("pAliasText"), `position:relative;display:inline;`);
  set($("pAliasQ2"), `position:relative;display:inline;font-size:${qS / 2}px;left:${-gh(aS, "Meiryo") / 2}px;top:${gh(aS, "Meiryo") * 2 - gh(qS, "Meiryo") / 2}px;`);

  const iS = factValue("poulsen.addFont"), iF = iS / 2;             // AdditionInfoComponent:38
  const iX = r1x + r1w, iY = factValue("poulsen.addY"), iH = factValue("poulsen.addH"); // :37,:42
  const iW = Math.max(factValue("poulsen.addW"), tw("FEBRUARY 21", iF, FM.rb)); // :36
  set($("pAddInfo"), `left:${iX}px;top:${iY}px;width:${iW}px;height:${iH * 3}px;`);
  const iY1 = iY + iH / 2 - gh(iS, FM.rb) / 2;
  set($("pAdd1"), `left:${(iW - tw("FEBRUARY 21", iF, FM.rb)) / 2}px;top:${cssTop(iY1, iS, FM.rb)}px;font-size:${iF}px;color:#fff;`);
  set($("pAdd2"), `left:${(iW - tw("50 kg & 156 cm", iF, FM.rb)) / 2}px;top:${cssTop(iY1 + iH, iS, FM.rb)}px;font-size:${iF}px;color:#424242;`);
  set($("pAdd3"), `left:${(iW - tw("Aqua eye", iF, FM.rb)) / 2}px;top:${cssTop(iY1 + iH * 2, iS, FM.rb)}px;font-size:${iF}px;color:#424242;`);

  const tW = r1w * 0.644, tH = tW * 3.51;                  // webOnly (与 bgImg 同源图比例)
  set($("pTachie"), `left:${r1x + tW / 2.5}px;top:${H * 0.05}px;width:${tW}px;height:${tH}px;`);

  const dots = $("pDots");
  if (!dots.dataset.built) {
    dots.dataset.built = "1";
    for (let r = 0; r < 5; r++) for (let c = 0; c < 8; c++) {
      const d = document.createElement("i");
      d.style.left = c * 10 + "px"; d.style.top = r * 10 + "px";
      dots.appendChild(d);
    }
  }
  set(dots, `left:${W - 75}px;top:${H - 45}px;width:75px;height:45px;`);
  set($("pDot1"), `left:${factValue("poulsen.circleX") - 3.5}px;top:${factValue("poulsen.circleY") - 3.5}px;background:#F24949;`); // CirclesComponent:40-41
  set($("pDot2"), `left:${factValue("poulsen.circleX") * 2 - 3.5}px;top:${factValue("poulsen.circleY") - 3.5}px;background:#E5439B;`);
  set($("pDot3"), `left:${factValue("poulsen.circleX") * 3 - 3.5}px;top:${factValue("poulsen.circleY") - 3.5}px;background:#FF90C5;`);

  set($("pLogo"), `left:${factValue("splash.logoX")}px;top:${H * 0.1}px;width:${factValue("splash.logoW")}px;height:${factValue("splash.logoH")}px;`);

  const bS = factValue("poulsen.btnFont"), bF = bS / 2;             // MainMenuScreen:84
  const bX = factValue("poulsen.btnX"), bY0 = factValue("poulsen.btnY0"), bGap = factValue("poulsen.btnGap"); // :86-87
  const bTops = [0, 1, 2, 3, 4].map(i => cssTop(bY0 + i * bGap, bS, FM.rb));
  const bIds = ["pBtnSingle", "pBtnMulti", "pBtnOptions", "pBtnQuitP", "pBtnMisayos"];
  bIds.forEach((id, i) => set($(id), `left:${bX}px;top:${bTops[i]}px;font-size:${bF}px;`));
}
