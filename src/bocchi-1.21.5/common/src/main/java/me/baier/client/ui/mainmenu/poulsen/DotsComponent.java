package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;

import java.util.List;

public class DotsComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> dotAlphaComp1Animation;
  private BezierAnimation<Float> dotAlphaComp2Animation;
  private BezierAnimation<Float> dotAlphaComp3Animation;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    long dotAnimDuration = 600;
    float maxAlphaPerComponent = 255f / 3f;
    dotAlphaComp1Animation =
        BezierAnimation.createFloat(
            0f, maxAlphaPerComponent, dotAnimDuration, BezierControlPoints.EASE_IN_OUT);
    dotAlphaComp2Animation =
        BezierAnimation.createFloat(
            0f, maxAlphaPerComponent, dotAnimDuration, BezierControlPoints.EASE_IN_OUT);
    dotAlphaComp3Animation =
        BezierAnimation.createFloat(
            0f,
            255f - (2 * maxAlphaPerComponent),
            dotAnimDuration,
            BezierControlPoints.EASE_IN_OUT);

    animations = List.of(dotAlphaComp1Animation, dotAlphaComp2Animation, dotAlphaComp3Animation);
    lastAnimation.then(dotAlphaComp1Animation, 0.2f);
    dotAlphaComp1Animation.then(dotAlphaComp2Animation, 0.4f);
    dotAlphaComp2Animation.then(dotAlphaComp3Animation, 0.4f);
    return dotAlphaComp3Animation;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var canvas = ctx.canvas();
    Path clipPathForDots = null;
    Shader gradientShaderForDots = null;
    Paint gradientPaintForDots = null;
    try { // Dots rendering
      int numRowsDots = 5;
      int numColsDots = 8;
      float circleRadiusDots = 2.5f;
      float spacingDots = 5f;
      float dotsGridWidth = numColsDots * (2 * circleRadiusDots) + (numColsDots - 1) * spacingDots;
      float dotsGridHeight = numRowsDots * (2 * circleRadiusDots) + (numRowsDots - 1) * spacingDots;
      float dotsPaddingFromEdge = 0;
      float dotsStartX = frame.getScaledWidth() - dotsGridWidth - dotsPaddingFromEdge;
      float dotsStartY = frame.getScaledHeight() - dotsGridHeight - dotsPaddingFromEdge;
      clipPathForDots = new Path();
      for (int row = 0; row < numRowsDots; row++) {
        for (int col = 0; col < numColsDots; col++) {
          float dotCenterX =
              dotsStartX + circleRadiusDots + col * (2 * circleRadiusDots + spacingDots);
          float dotCenterY =
              dotsStartY + circleRadiusDots + row * (2 * circleRadiusDots + spacingDots);
          clipPathForDots.addCircle(dotCenterX, dotCenterY, circleRadiusDots);
        }
      }
      clipPathForDots.setFillMode(PathFillMode.EVEN_ODD);
      Rect gradientRectDots =
          Rect.makeLTRB(
              dotsStartX - spacingDots,
              dotsStartY - spacingDots,
              dotsStartX + dotsGridWidth + spacingDots,
              dotsStartY + dotsGridHeight + spacingDots);
      Point gradientStartPointDots =
          new Point(gradientRectDots.getLeft(), gradientRectDots.getTop());
      Point gradientEndPointDots =
          new Point(gradientRectDots.getRight(), gradientRectDots.getBottom());
      float currentTotalDotAlpha = 0f;
      if (dotAlphaComp1Animation != null)
        currentTotalDotAlpha += dotAlphaComp1Animation.getCurrentValue();
      if (dotAlphaComp2Animation != null)
        currentTotalDotAlpha += dotAlphaComp2Animation.getCurrentValue();
      if (dotAlphaComp3Animation != null)
        currentTotalDotAlpha += dotAlphaComp3Animation.getCurrentValue();
      currentTotalDotAlpha = Math.max(0f, Math.min(255f, currentTotalDotAlpha));
      int animatedDotAlphaInt = Math.round(currentTotalDotAlpha);
      int finalAlpha1 = animatedDotAlphaInt;
      int finalAlpha2 = Math.round((animatedDotAlphaInt / 255.0f) * 0x66);
      int[] gradientColorsDots =
          new int[] {
            (finalAlpha1 << 24) | (0xFFFFFFFF & 0x00FFFFFF),
            (finalAlpha2 << 24) | (0xFFFFFFFF & 0x00FFFFFF)
          };
      gradientShaderForDots =
          Shader.makeLinearGradient(
              gradientStartPointDots, gradientEndPointDots, gradientColorsDots);
      gradientPaintForDots = new Paint().setShader(gradientShaderForDots).setAntiAlias(true);
      canvas.save();
      canvas.clipPath(clipPathForDots, ClipMode.INTERSECT, true);
      canvas.drawRect(gradientRectDots, gradientPaintForDots);
      canvas.restore();
    } finally {
      if (clipPathForDots != null) clipPathForDots.close();
      if (gradientShaderForDots != null) gradientShaderForDots.close();
      if (gradientPaintForDots != null) gradientPaintForDots.close();
    }
  }
}
