package me.baier.client.ui.model;

import lombok.Getter;

public class MainMenuPoulsenFrameContext extends FrameContext {
  @Getter private float rect1Width;
  @Getter private float rect1PosX;
  @Getter private float bgFontSize;

  public MainMenuPoulsenFrameContext() {
    super();
    rect1Width = scaledWidth * 0.412f;
    rect1PosX = (scaledWidth - rect1Width) / 2;

    bgFontSize = scaledHeight * 0.205f / 0.305f;
  }
}
