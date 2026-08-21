package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.design.Design;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

public class AdditionInfoComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> febInfoGroupAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    febInfoGroupAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(febInfoGroupAnim, 0.1f);
    animations.add(febInfoGroupAnim);
    return febInfoGroupAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();

    var canvas = ctx.canvas();

    float additionalInfoFinalWidth = screenWidth * 0.1076f;
    float additionalInfoFinalHeight = screenHeight * 0.0322f;
    String addTitleText = Design.value("texts.pAdd1", "FEBRUARY 21");
    var infoTitleFontForCalc = FontSet.RADIKAL_BLACK.getFont(additionalInfoFinalHeight / 0.8f);
    float feb21TextWidth = infoTitleFontForCalc.getStringWidth(addTitleText);
    if (feb21TextWidth > additionalInfoFinalWidth) additionalInfoFinalWidth = feb21TextWidth;
    float finalFebInfoX = rect1PosX + rect1Width;
    float finalFebInfoY = screenHeight * 0.734f;
    float febInfoBlockActualHeight = additionalInfoFinalHeight * 3;
    Point febInfoStartPos =
        ScreenUtils.calculateStartPosition(
            finalFebInfoX,
            finalFebInfoY,
            additionalInfoFinalWidth,
            febInfoBlockActualHeight,
            screenWidth,
            screenHeight,
            animOffset);
    if (febInfoGroupAnim != null) {
      float groupAlpha =
          (febInfoGroupAnim != null) ? Math.round(febInfoGroupAnim.getCurrentValue() * 255f) : 255f;
      canvas.save(); // Save before translate and layer
      canvas.translate(
          Mth.lerp(febInfoGroupAnim.getCurrentValue(), febInfoStartPos.getX(), finalFebInfoX)
              - (rect1PosX + rect1Width),
          Mth.lerp(febInfoGroupAnim.getCurrentValue(), febInfoStartPos.getY(), finalFebInfoY)
              - (frame.getScaledHeight() * 0.734f));
      canvas.saveLayerAlpha(null, (int) groupAlpha);

      float additionalInfoCurrentWidth = frame.getScaledWidth() * 0.1076f;
      float additionalInfoCurrentHeight = frame.getScaledHeight() * 0.0322f;
      var infoTitleFont = FontSet.RADIKAL_BLACK.getFont(additionalInfoCurrentHeight / 0.8f);
      var infoContentFont = FontSet.RADIKAL_REGULAR.getFont(additionalInfoCurrentHeight / 0.8f);
      float feb21TextWidthForRender = infoTitleFont.getStringWidth(addTitleText);
      if (feb21TextWidthForRender > additionalInfoCurrentWidth)
        additionalInfoCurrentWidth = feb21TextWidthForRender;

      float finalFebInfoX_orig = rect1PosX + rect1Width;
      float finalFebInfoY_orig = frame.getScaledHeight() * 0.734f;

      SkiaRenderEngine.drawRect(
          finalFebInfoX_orig,
          finalFebInfoY_orig,
          additionalInfoCurrentWidth,
          additionalInfoCurrentHeight,
          0XFF000000);
      float infoFontDrawPosX =
          finalFebInfoX_orig + (additionalInfoCurrentWidth - feb21TextWidthForRender) / 2;
      float infoFontDrawPosY =
          (finalFebInfoY_orig + additionalInfoCurrentHeight / 2)
              - infoTitleFont.getHalfHeight() / 2;
      infoTitleFont.drawString(addTitleText, infoFontDrawPosX, infoFontDrawPosY, 0XFFFFFFFF);
      infoContentFont.drawString(
          Design.value("texts.pAdd2", "50 kg & 156 cm"),
          infoFontDrawPosX,
          infoFontDrawPosY + additionalInfoCurrentHeight,
          0XFF424242);
      infoContentFont.drawString(
          Design.value("texts.pAdd3", "Aqua eye"),
          infoFontDrawPosX,
          infoFontDrawPosY + additionalInfoCurrentHeight * 2,
          0XFF424242);

      canvas.restore(); // Restore layer
      canvas.restore(); // Restore translate
    }
  }
}
