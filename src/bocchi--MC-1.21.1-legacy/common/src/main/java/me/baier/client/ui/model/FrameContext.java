package me.baier.client.ui.model;

import com.mojang.blaze3d.platform.Window;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

@Getter
@Setter
public class FrameContext extends IFrameContext {
  protected int scaledWidth, scaledHeight;

  public FrameContext() {

    Minecraft mc = Minecraft.getInstance();
    Window window = mc.getWindow();

    scaledWidth = window.getGuiScaledWidth();
    scaledHeight = window.getGuiScaledHeight();
  }
}
