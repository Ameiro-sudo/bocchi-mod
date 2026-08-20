package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.utils.ColorUtil;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

import java.util.List;

public class ThreeSquaresComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {

  private BezierAnimation<Float> leftSquareAnim;
  private BezierAnimation<Float> middleSquareAnim;
  private BezierAnimation<Float> rightSquareAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    leftSquareAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(leftSquareAnim, 0.1f);
    middleSquareAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    leftSquareAnim.then(middleSquareAnim, 0.05f);

    rightSquareAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    middleSquareAnim.then(rightSquareAnim, 0.05f);
    animations.addAll(List.of(leftSquareAnim, middleSquareAnim, rightSquareAnim));
    return rightSquareAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();

    float squaresPixelSize = 15f;
    float squareSpacing = squaresPixelSize / 3f;
    float squaresFinalPosY = screenHeight * 0.65f - squaresPixelSize / 2;
    float middleSquareFinalX = (rect1PosX * 0.75f) - squaresPixelSize / 2;
    float leftSquareFinalX = middleSquareFinalX - squareSpacing - squaresPixelSize;
    float rightSquareFinalX = middleSquareFinalX + squaresPixelSize + squareSpacing;
    Point leftSqStartPos =
        ScreenUtils.calculateStartPosition(
            leftSquareFinalX,
            squaresFinalPosY,
            squaresPixelSize,
            squaresPixelSize,
            screenWidth,
            screenHeight,
            animOffset);

    Point midSqStartPos =
        ScreenUtils.calculateStartPosition(
            middleSquareFinalX,
            squaresFinalPosY,
            squaresPixelSize,
            squaresPixelSize,
            screenWidth,
            screenHeight,
            animOffset);

    Point rightSqStartPos =
        ScreenUtils.calculateStartPosition(
            rightSquareFinalX,
            squaresFinalPosY,
            squaresPixelSize,
            squaresPixelSize,
            screenWidth,
            screenHeight,
            animOffset);

    int squareBaseColor = 0xFFE95A9F;
    if (leftSquareAnim != null)
      SkiaRenderEngine.drawRect(
          Mth.lerp(leftSquareAnim.getCurrentValue(), leftSqStartPos.getX(), leftSquareFinalX),
          Mth.lerp(leftSquareAnim.getCurrentValue(), leftSqStartPos.getY(), squaresFinalPosY),
          squaresPixelSize,
          squaresPixelSize,
          ColorUtil.replaceAlpha(
              squareBaseColor, Math.round(leftSquareAnim.getCurrentValue() * 255.f)));
    if (middleSquareAnim != null)
      SkiaRenderEngine.drawRect(
          Mth.lerp(middleSquareAnim.getCurrentValue(), midSqStartPos.getX(), middleSquareFinalX),
          Mth.lerp(middleSquareAnim.getCurrentValue(), midSqStartPos.getY(), squaresFinalPosY),
          squaresPixelSize,
          squaresPixelSize,
          ColorUtil.replaceAlpha(
              squareBaseColor, Math.round(middleSquareAnim.getCurrentValue() * 255.f)));
    if (rightSquareAnim != null)
      SkiaRenderEngine.drawRect(
          Mth.lerp(rightSquareAnim.getCurrentValue(), rightSqStartPos.getX(), rightSquareFinalX),
          Mth.lerp(rightSquareAnim.getCurrentValue(), rightSqStartPos.getY(), squaresFinalPosY),
          squaresPixelSize,
          squaresPixelSize,
          ColorUtil.replaceAlpha(
              squareBaseColor, Math.round(rightSquareAnim.getCurrentValue() * 255.f)));
  }
}
