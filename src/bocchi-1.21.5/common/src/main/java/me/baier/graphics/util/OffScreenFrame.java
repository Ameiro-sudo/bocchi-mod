package me.baier.graphics.util;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import io.github.humbleui.skija.*;
import java.util.Objects;
import lombok.Getter;
import me.baier.client.ClientInstance;
import me.baier.graphics.SkiaContext;
import me.baier.utils.GLUtils;

public class OffScreenFrame implements ClientInstance {

  @Getter private RenderTarget framebuffer;
  @Getter private Surface surface;
  @Getter private Canvas canvas;

  public static OffScreenFrame create() {
    return new OffScreenFrame();
  }

  public OffScreenFrame() {
    var ctx = SkiaContext.get();
    if (FramebufferFactory.INSTANCE.factory == null) {
      FramebufferFactory.INSTANCE.factory =
          new RenderTargetDescriptor(
              mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, 0);
    }

    this.framebuffer = FramebufferFactory.INSTANCE.factory.allocate();
    FramebufferFactory.INSTANCE.factory.prepare(this.framebuffer);
    this.surface =
        Surface.wrapBackendRenderTarget(
            ctx.getContext(),
            BackendRenderTarget.makeGL(
                this.framebuffer.width,
                this.framebuffer.height,
                0, // samples
                8, // stencil
                GLUtils.getFBO(this.framebuffer),
                FramebufferFormat.GR_GL_RGBA8),
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB(),
            new SurfaceProps(PixelGeometry.RGB_H));
    this.canvas = surface.getCanvas();
  }

  public void close() {
    surface.close();
    this.framebuffer.destroyBuffers();
  }
}
