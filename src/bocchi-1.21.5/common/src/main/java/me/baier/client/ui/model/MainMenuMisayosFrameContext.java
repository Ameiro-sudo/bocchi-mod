package me.baier.client.ui.model;

import io.github.humbleui.types.Point;
import lombok.Getter;

@Getter
public class MainMenuMisayosFrameContext extends FrameContext {
  protected final Point block1Pos;
  protected final float block1Size;
  protected final Point block3Pos;
  protected final float block3Size;

  public MainMenuMisayosFrameContext() {
    super();
    block1Pos = new Point(scaledWidth * 0.095f, scaledHeight * 0.4f);
    block1Size = Math.min(scaledWidth * 0.2265625f, scaledHeight * 0.4027777777777778f);
    block3Pos = new Point(scaledWidth * 0.263125f, scaledHeight * 0.0972222222222222f);
    block3Size = Math.min(scaledWidth * 0.4166666666666667f, scaledHeight * 0.7407407407407407f);
  }
}
