package me.baier.client.ui.mainmenu.component.api;

import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.interfaces.IComponent;
import me.baier.client.ui.model.FrameContext;
import me.baier.client.ui.model.IFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBaseComponent<T extends IFrameContext> implements IComponent<T> {
  protected List<BezierAnimation<Float>> animations = new ArrayList<>();

  @Override
  public void update(T frameContext) {
    for (var anim : animations) {
      if (anim.isAnimating() && !anim.isPaused()) anim.update();
    }
  }
}
