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
    // 固定 1920x1080 逻辑画布: 任何窗口/界面尺寸下画面占比一致
    scaledWidth = 480;
    scaledHeight = 270;
  }
}
