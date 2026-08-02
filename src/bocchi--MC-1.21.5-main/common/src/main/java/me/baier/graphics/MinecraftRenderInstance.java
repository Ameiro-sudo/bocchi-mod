package me.baier.graphics;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import me.baier.client.ClientInstance;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;

public interface MinecraftRenderInstance extends ClientInstance {

  default void defaultBlendFunc() {
    GlStateManager._blendFuncSeparate(
        GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
  }

  default GlDevice getDevice() {
    return (GlDevice) RenderSystem.getDevice();
  }

  default GlCommandEncoder getCommandEncoder() {
    return (GlCommandEncoder) getDevice().createCommandEncoder();
  }

  default int getFBO(RenderTarget target) {
    if (target.getColorTexture() != null) {
      return ((GlTexture) target.getColorTexture())
              .getFbo(getDevice().directStateAccess(), target.getDepthTexture());
    }
    throw new IllegalStateException("No FBO available");
  }

  default int getMinecraftFBO() {
    return getFBO(mc.getMainRenderTarget());
  }

  default GLCapabilities gl_caps() {
    try {
      return GL.getCapabilities();
    } catch (IllegalStateException e) {
      return GL.createCapabilities();
    }
  }

  default boolean dsa() {
    return gl_caps().GL_ARB_direct_state_access;
  }
}
