package me.baier.client.ui.mainmenu.misayos;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.FrameContext;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.utils.ColorUtil;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

import java.util.List;

public class StrokeElementsComponent extends AbstractBaseComponent<MainMenuMisayosFrameContext> {
  private BezierAnimation<Float> line1Animation;
  private BezierAnimation<Float> line2Animation;
  private BezierAnimation<Float> blindsAnimation;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuMisayosFrameContext frame, BezierAnimation<Float> lastAnimation) {
    line1Animation = BezierAnimation.createFloat(0.f, 1.f, 600, BezierControlPoints.CUBIC_IN);
    line2Animation = BezierAnimation.createFloat(0.f, 1.f, 760, BezierControlPoints.CUBIC_OUT);
    blindsAnimation = BezierAnimation.createFloat(0.f, 1.f, 1000, BezierControlPoints.EASE_OUT);
    lastAnimation.then(line1Animation, 0.1f);
    line1Animation.then(line2Animation, 0.1f);
    line2Animation.then(blindsAnimation, 0.1f);
    animations = List.of(line1Animation, line2Animation, blindsAnimation);
    return blindsAnimation;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuMisayosFrameContext frame) {
    var screenWidth = frame.getScaledWidth();
    var screenHeight = frame.getScaledHeight();
    var blockPos1 = frame.getBlock1Pos();
    var blockSize = frame.getBlock1Size();

    var canvas = ctx.canvas();
    var line1Start = new Point(blockPos1.getX() + blockSize * 0.1f, blockPos1.getY() * 0.1f);
    var line1End = new Point(blockPos1.getX() + blockSize * 0.72f, blockPos1.getY());
    var line2Start =
        new Point(blockPos1.getX() + blockSize * 0.1f, blockPos1.getY() + blockSize * 1.1f);
    var line2End = new Point(blockPos1.getX() + blockSize * 0.72f, blockPos1.getY() + 5.f);
    var currentLine1End =
        new Point(
            Mth.lerp(line1Animation.getCurrentValue(), line1Start.getX(), line1End.getX()),
            Mth.lerp(line1Animation.getCurrentValue(), line1Start.getY(), line1End.getY()));
    var currentLine2End =
        new Point(
            Mth.lerp(line2Animation.getCurrentValue(), line2Start.getX(), line2End.getX()),
            Mth.lerp(line2Animation.getCurrentValue(), line2Start.getY(), line2End.getY()));
    var rect2Pos = ScreenUtils.calculateCenterPosition(line2End, line1End);
    var currentRect2PosForLine1 =
        ScreenUtils.calculateCenterPosition(
            currentLine1End,
            new Point(
                Mth.lerp(line1Animation.getCurrentValue(), line2Start.getX(), line2End.getX()),
                Mth.lerp(line1Animation.getCurrentValue(), line2Start.getY(), line2End.getY())));
    var currentRect2PosForLine2 =
        ScreenUtils.calculateCenterPosition(
            new Point(
                Mth.lerp(line2Animation.getCurrentValue(), line1Start.getX(), line1End.getX()),
                Mth.lerp(line2Animation.getCurrentValue(), line1Start.getY(), line1End.getY())),
            currentLine2End);
    var rect2Size = 5.f;
    int line1Alpha = Math.round(Mth.lerp(line1Animation.getCurrentValue(), 0, 255));
    int line2Alpha = Math.round(Mth.lerp(line2Animation.getCurrentValue(), 0, 255));
    // drawDashLine(canvas, line2Start, line2End, 3.5f, 0XFFFFFFFF, 0.7f);
    drawDashLine(
        canvas,
        line1Start,
        new Point(currentRect2PosForLine1.getX() + 1.f, currentLine1End.getY()),
        3.5f,
        ColorUtil.replaceAlpha(0XFFFFFFFF, line1Alpha),
        0.5f);
    drawDashLine(
        canvas,
        line2Start,
        new Point(currentRect2PosForLine2.getX() + 1.f, currentLine2End.getY()),
        3.5f,
        ColorUtil.replaceAlpha(0XFFFFFFFF, line2Alpha),
        0.5f);
    var currentRect2Size = Mth.lerp(line2Animation.getCurrentValue(), 0.f, rect2Size);

    SkiaRenderEngine.drawRect(
        line1Start.getX() - 4.f,
        line1Start.getY() - 4.f,
        2.5f,
        2.5f,
        ColorUtil.replaceAlpha(0XFFFFFFFF, line1Alpha));

    SkiaRenderEngine.drawRect(
        rect2Pos.getX() + 1.f,
        rect2Pos.getY() - rect2Size / 2.f,
        rect2Size,
        currentRect2Size,
        ColorUtil.replaceAlpha(0XFFFFFFFF, line2Alpha));
    var rect3Size = 8.f;
    var currentRect3Size = Mth.lerp(line2Animation.getCurrentValue(), 0.f, rect3Size);

    SkiaRenderEngine.drawRect(
        line2Start.getX() - rect3Size,
        line2Start.getY() + 1.f,
        rect3Size,
        currentRect3Size,
        ColorUtil.replaceAlpha(0XFFFFFFFF, line2Alpha));
    var currentSpacing = 7 - Mth.lerp(blindsAnimation.getCurrentValue(), 0.f, 5.f);
    var blindAlpha = Math.round(Mth.lerp(blindsAnimation.getCurrentValue(), 0, 255));
    drawBlindsRect(
        ctx,
        Rect.makeXYWH(
            line2Start.getX() + rect3Size,
            line2Start.getY() + 1.f,
            rect3Size * 6.f,
            currentRect3Size),
        2.f,
        currentSpacing,
        48.f,
        ColorUtil.replaceAlpha(0XFFFFFFFF, blindAlpha));

    drawBlindsRect(
        ctx,
        Rect.makeXYWH(screenWidth * 0.8f, screenHeight * 0.08f, rect3Size * 8.f, rect3Size),
        2.f,
        currentSpacing,
        48.f,
        ColorUtil.replaceAlpha(0XFFFFFFFF, blindAlpha));
  }

  public void drawBlindsRect(
      SkiaContext ctx, Rect rect, float width, float spacing, float rotate, int color) {

    var canvas = ctx.canvas();
    canvas.saveLayer(rect, null);
    float centerX = rect.getLeft() + rect.getWidth() / 2.f;
    float centerY = rect.getTop() + rect.getHeight() / 2.f;
    ctx.startRotate(centerX, centerY, rotate);
    float diagonal =
        (float) Math.sqrt(Math.pow(rect.getWidth(), 2) + Math.pow(rect.getHeight(), 2));
    float startOffset = (diagonal - rect.getWidth()) / 2f;
    float currentX = rect.getLeft() - startOffset;

    float endX = rect.getRight() + startOffset;

    while (currentX < endX) {
      try (var paint = new Paint().setColor(color)) {
        var blindRect =
            Rect.makeLTRB(
                currentX,
                rect.getTop() - (diagonal - rect.getHeight()) / 2f,
                currentX + width,
                rect.getBottom() + (diagonal - rect.getHeight()) / 2f);
        canvas.drawRect(blindRect, paint);
        currentX += (width + spacing);
      }
    }

    ctx.stopRotate();
    canvas.restore();
  }

  private static void drawDashLine(
      Canvas canvas, Point start, Point end, float interval, int color, float width) {
    try (var linePaint =
        new Paint().setColor(color).setMode(PaintMode.STROKE).setStrokeWidth(width)) {

      float[] intervals = {interval, interval};
      float phase = 0f;

      try (PathEffect dashEffect = PathEffect.makeDash(intervals, phase)) {
        linePaint.setPathEffect(dashEffect);
        canvas.drawLine(start.getX(), start.getY(), end.getX(), end.getY(), linePaint);
      }
    }
  }
}
