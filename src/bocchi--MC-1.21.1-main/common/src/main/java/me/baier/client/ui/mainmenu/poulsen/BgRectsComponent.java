package me.baier.client.ui.mainmenu.poulsen;

import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;

public class BgRectsComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    return lastAnimation;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    float rect1Width = frame.getRect1Width();
    ctx.canvas().clear(0xFFFFFFFF);
    var rect1PosX = frame.getRect1PosX();

    SkiaRenderEngine.drawRect(rect1PosX, 0, rect1Width, frame.getScaledHeight(), 0xFFF567B4);
    float rect3PosX = rect1PosX + rect1Width;
    SkiaRenderEngine.drawRectLTRB(
        rect3PosX, 0, frame.getScaledWidth(), frame.getScaledHeight(), 0xFFF198D3);
  }
}
