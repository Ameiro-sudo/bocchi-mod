package me.baier.skui.impl;

import io.github.humbleui.skija.Color;
import lombok.Getter;
import lombok.Setter;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.graphics.SkiaEnvironment;
import me.baier.skui.SkComponent;

public class SkContainer extends SkComponent {

  protected static final int SHADOW_COLOR = Color.makeARGB(35, 0, 0, 0);

  @Getter @Setter private float layoutHeight;

  @Setter @Getter private float scrollValue = 0.0F;

  @Getter
  private final BezierAnimation<Float> scroller =
      BezierAnimation.createFloat(0.0F, 300, BezierControlPoints.CUBIC_OUT);

  protected final SkScrollbar scrollbar = new SkScrollbar(this);

  public SkContainer() {
    this.addChild(scrollbar);
  }

  public SkContainer(float x, float y, float width, float height) {
    super(x, y, width, height);
    this.addChild(scrollbar);
  }

  public void ensureLimit() {
    float absHeight = getHeight();
    float maxScroll = absHeight - layoutHeight;
    this.scrollValue = Math.min(Math.max(scrollValue, maxScroll), 0);
    this.scroller.set(scrollValue);
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    final BezierAnimation<Float> scroller = getScroller();
    scroller.update();
    if (!scroller.isComplete()) {
      invalidateLayout();
    }
  }

  @Override
  protected void onRenderChildren(SkiaEnvironment env, int mouseX, int mouseY) {
    scrollbar.render(env, mouseX, mouseY);
  }

  @Override
  protected boolean onMouseScroll(int mouseX, int mouseY, int scroll) {
    float absHeight = getHeight();
    if (absHeight >= layoutHeight) {
      return false;
    }

    boolean scrollDown = scroll < 0;
    this.scrollValue += scrollDown ? -60.0F : 60.0F;
    ensureLimit();
    return true;
  }
}
