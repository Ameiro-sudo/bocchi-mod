package me.baier.client.ui.model;

import com.mojang.blaze3d.platform.Window;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

@Getter
@Setter
public class SplashFrameContext extends FrameContext {
  int midX, midY;
  int spacing;

  public SplashFrameContext() {
    super();
    midY = (scaledHeight / 2);
    midX = ((scaledWidth) / 2);
    var d = Math.min(scaledWidth * 0.75, scaledHeight) * 0.25;
    spacing = (int) (d * 0.5);
  }
}
