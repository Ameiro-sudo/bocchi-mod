package me.baier.client.ui.mainmenu.component.interfaces;

import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.model.FrameContext;
import me.baier.client.ui.model.IFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;

public interface IComponent<T extends IFrameContext> {
  float animOffset = 50f;
  long entranceDuration = 800;
  BezierControlPoints easeFunc = BezierControlPoints.CUBIC_OUT;

  BezierAnimation<Float> initAnimations(T frame, BezierAnimation<Float> lastAnimation);

  void update(T frame);

  void render(SkiaContext ctx, T frame);
}
