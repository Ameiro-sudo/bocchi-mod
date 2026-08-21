package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.design.Design;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ColorUtil;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

public class BigFirstNameComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> hitoriTextAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    hitoriTextAnim = BezierAnimation.createFloat(0, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(hitoriTextAnim, 0.1f);
    animations.add(hitoriTextAnim);
    return hitoriTextAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    float bgFontSize = screenHeight * 0.205f / 0.305f;
    float hitoriFontSize = bgFontSize * 0.552f;
    var hitoriFont = FontSet.RADIKAL_BLACK.getFont(hitoriFontSize);
    float hitoriTextWidth = hitoriFont.getStringWidth(getName());
    float hitoriTextHeight = hitoriFont.getFont().getMetrics().getCapHeight();
    float finalHitoriTextDrawX_centered = rect1PosX * 0.5f;
    float finalHitoriTextDrawY_baseline = (screenHeight * 0.2f + hitoriFont.getHalfHeight() / 2);
    Point hitoriStartPos =
        ScreenUtils.calculateStartPosition(
            finalHitoriTextDrawX_centered - hitoriTextWidth / 2,
            finalHitoriTextDrawY_baseline - hitoriTextHeight,
            hitoriTextWidth,
            hitoriTextHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (hitoriTextAnim != null) {
      var hitoriFontToUse = FontSet.RADIKAL_BLACK.getFont(bgFontSize * 0.552f);
      hitoriFontToUse.drawString(
          getName(),
          Mth.lerp(
              hitoriTextAnim.getCurrentValue(),
              hitoriStartPos.getX(),
              finalHitoriTextDrawX_centered),
          Mth.lerp(
              hitoriTextAnim.getCurrentValue(),
              hitoriStartPos.getY(),
              finalHitoriTextDrawY_baseline),
          ColorUtil.replaceAlpha(0xEEE95A9F, Math.round(hitoriTextAnim.getCurrentValue() * 255f)));
    }
  }

  private static String getName() {
    return Design.value("texts.pHitoriTop", "HITORI");
  }
}
