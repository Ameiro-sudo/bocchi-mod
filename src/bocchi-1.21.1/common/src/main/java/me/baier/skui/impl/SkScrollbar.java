package me.baier.skui.impl;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Color;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import lombok.Setter;
import me.baier.graphics.SkiaEnvironment;
import me.baier.skui.SkComponent;

public class SkScrollbar extends SkComponent {

  private static final int BACKGROUND = Color.makeARGB(30, 0, 0, 0);
  private static final int SLIDER_COLOR = Color.makeARGB(51, 0, 0, 0);

  private final SkContainer container;
  private boolean isDragging = false;

  @Setter private float offsetTop;

  public SkScrollbar(SkContainer container) {
    this.container = container;
    this.setWidth(10);
    this.setZIndex(1);
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    if (container.getHeight() >= container.getLayoutHeight()) {
      setEnabled(false);
      return;
    }
    setEnabled(true);

    if (isDragging) {
      updateScrollPosition(mouseY);
    }

    final float visibleHeight = container.getHeight();
    final float totalHeight = container.getLayoutHeight();
    final float scrollPosition = container.getScroller().getCurrentValue();

    setX(container.getWidth() - getWidth());
    setY(offsetTop);
    setHeight(container.getHeight() - offsetTop);

    final float absX = getAbsoluteX();
    final float absY = getAbsoluteY();
    final float absWidth = getWidth();
    final float absHeight = getHeight();

    final float scrollButtonHeight = (visibleHeight / totalHeight) * absHeight;
    final float scrollButtonPosition =
        (scrollPosition / (totalHeight - visibleHeight)) * -(absHeight - scrollButtonHeight);

    Canvas canvas = env.getCanvas();
    Paint paint = env.borrowPaint();

    paint.setColor(BACKGROUND);
    canvas.drawRect(Rect.makeXYWH(absX, absY, absWidth, absHeight), paint);

    paint.setColor(SLIDER_COLOR);
    canvas.drawRRect(
        RRect.makeXYWH(absX, absY + scrollButtonPosition, absWidth, scrollButtonHeight, 10), paint);

    env.recyclePaint(paint);
  }

  @Override
  protected void onMouseClick(int mouseX, int mouseY, int button) {
    if (isHovered() && button == 0) {
      isDragging = true;
      updateScrollPosition(mouseY);
    }
  }

  @Override
  protected boolean onMouseRelease(int mouseX, int mouseY, int button) {
    if (isDragging) {
      isDragging = false;
    }
    return false;
  }

  private void updateScrollPosition(int mouseY) {
    float visibleHeight = container.getHeight();
    float totalHeight = container.getLayoutHeight();
    float scrollableHeight = totalHeight - visibleHeight;

    if (scrollableHeight <= 0) return;

    float absY = getAbsoluteY();
    float absHeight = getHeight();
    float scrollButtonHeight = (visibleHeight / totalHeight) * absHeight;

    float halfButtonHeight = scrollButtonHeight / 2;
    float relY = mouseY - absY - halfButtonHeight;
    float maxY = absHeight - scrollButtonHeight;
    relY = Math.max(-halfButtonHeight, Math.min(relY, maxY + halfButtonHeight));

    float newScrollPosition = -(relY / (absHeight - scrollButtonHeight)) * scrollableHeight;
    container.setScrollValue(newScrollPosition);
    container.ensureLimit();
  }
}
