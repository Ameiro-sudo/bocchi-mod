package me.baier.client.ui.mainmenu.poulsen;

import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.utils.ColorUtil;

import java.util.List;

public class CirclesComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {

  private BezierAnimation<Float> circleAnimationChain;
  private BezierAnimation<Float> circle2Animation;
  private BezierAnimation<Float> circle3Animation;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    long circleAnimDuration = 2000 / 3;
    circleAnimationChain =
        BezierAnimation.createFloat(0f, 255f, circleAnimDuration, BezierControlPoints.EASE_IN_OUT);
    circle2Animation =
        BezierAnimation.createFloat(0f, 255f, circleAnimDuration, BezierControlPoints.EASE_IN_OUT);
    circle3Animation =
        BezierAnimation.createFloat(0f, 255f, circleAnimDuration, BezierControlPoints.EASE_IN_OUT);
    lastAnimation
        .then(circleAnimationChain, 0.4f)
        .then(circle2Animation, 0.4f)
        .then(circle3Animation, 0.4f);
    animations.addAll(List.of(circleAnimationChain, circle2Animation, circle3Animation));
    return lastAnimation;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {

    var circleX = frame.getScaledWidth() * 0.03f;
    var circleY = frame.getScaledHeight() * 0.05f;
    SkiaRenderEngine.drawCircle(
        circleX,
        circleY,
        3.5f,
        ColorUtil.replaceAlpha(
            0XFFF24949,
            circleAnimationChain == null
                ? 255
                : Math.round(circleAnimationChain.getCurrentValue())));
    SkiaRenderEngine.drawCircle(
        circleX + circleX,
        circleY,
        3.5f,
        ColorUtil.replaceAlpha(
            0XFFE5439B,
            circle2Animation == null ? 255 : Math.round(circle2Animation.getCurrentValue())));
    SkiaRenderEngine.drawCircle(
        circleX * 3,
        circleY,
        3.5f,
        ColorUtil.replaceAlpha(
            0xFFFF90C5,
            circle3Animation == null ? 255 : Math.round(circle3Animation.getCurrentValue())));
  }
}
