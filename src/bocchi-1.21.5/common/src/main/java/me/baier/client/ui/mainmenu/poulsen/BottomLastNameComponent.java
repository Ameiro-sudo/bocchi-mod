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

public class BottomLastNameComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {

  private BezierAnimation<Float> bottomHitoriTextAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    bottomHitoriTextAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(bottomHitoriTextAnim, 0.1f);
    animations.add(bottomHitoriTextAnim);
    return bottomHitoriTextAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();
    float bgFontSize = frame.getBgFontSize();

    var canvas = ctx.canvas();
    var sharedFontForGoto = FontSet.RADIKAL_BLACK.getFont(bgFontSize);
    String bottomHitoriTextStr = Design.value("texts.pHitoriBottom", "HITORI");
    float bottomHitoriFinalX = rect1PosX - sharedFontForGoto.getHalfHeight() / 4.5f;
    float rect3PosY_val = screenHeight * 0.586f;
    float rect3Height_val_for_hitori = screenHeight * 0.086f;
    float bottomHitoriFinalY =
        rect3PosY_val + rect3Height_val_for_hitori + sharedFontForGoto.getHalfHeight() / 2.35f;
    float bottomHitoriWidth = sharedFontForGoto.getStringWidth(bottomHitoriTextStr);
    float bottomHitoriHeight = sharedFontForGoto.getHeight();
    Point bottomHitoriStartPos =
        ScreenUtils.calculateStartPosition(
            bottomHitoriFinalX,
            bottomHitoriFinalY - bottomHitoriHeight,
            bottomHitoriWidth,
            bottomHitoriHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (bottomHitoriTextAnim != null)
      sharedFontForGoto.drawString(
          bottomHitoriTextStr,
          Mth.lerp(
              bottomHitoriTextAnim.getCurrentValue(),
              bottomHitoriStartPos.getX(),
              bottomHitoriFinalX),
          Mth.lerp(
              bottomHitoriTextAnim.getCurrentValue(),
              bottomHitoriStartPos.getY() + bottomHitoriHeight,
              bottomHitoriFinalY),
          ColorUtil.replaceAlpha(
              0xFFFF86C0, Math.round(bottomHitoriTextAnim.getCurrentValue() * 255.f)));
  }
}
