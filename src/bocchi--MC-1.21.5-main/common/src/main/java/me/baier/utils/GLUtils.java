package me.baier.utils;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Objects;

public class GLUtils {

  public static int getFBO(RenderTarget frameBuffer) {
    return ((GlTexture) Objects.requireNonNull(frameBuffer.getColorTexture()))
        .getFbo(
            ((GlDevice) RenderSystem.getDevice()).directStateAccess(),
            frameBuffer.getDepthTexture());
  }
}
