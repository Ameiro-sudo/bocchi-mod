package me.baier.client.ui.mainmenu.common;

import me.baier.animation.BezierAnimation;
import me.baier.client.ui.common.components.LogoRenderer;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.FrameContext;
import me.baier.client.ui.model.IFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.utils.TimerUtil;

public class LogoComponent extends AbstractBaseComponent<FrameContext> {
  private final TimerUtil timer = new TimerUtil();

  @Override
  public BezierAnimation<Float> initAnimations(
      FrameContext frame, BezierAnimation<Float> lastAnimation) {
    timer.reset();
    return lastAnimation;
  }

  @Override
  public void render(SkiaContext ctx, FrameContext frame) {
    if (timer.hasTimeElapsed(1000)) {
      var scaledWidth = frame.getScaledWidth();
      var rect1Width = scaledWidth * 0.412f;
      var rect1PosX = (scaledWidth - rect1Width) / 2;
      float logoDrawPosX = rect1PosX * 0.3f;
      float logoDrawPosY = frame.getScaledHeight() * 0.1f;
      float logoCurrentWidth = 60.f;
      float logoCurrentHeight = logoCurrentWidth * 0.317f;
      LogoRenderer.renderLogoWithColor(
          ctx.canvas(),
          logoDrawPosX,
          logoDrawPosY,
          logoCurrentWidth,
          logoCurrentHeight,
          0XFFFFFFFF);
    }
  }
}
