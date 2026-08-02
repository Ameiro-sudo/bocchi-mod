package me.baier.client.ui.common.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import me.baier.client.ui.model.FrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.media.SKVideoDecoder;
import org.lwjgl.opengl.GL33C;

import java.io.IOException;

public class BackgroundRenderer {
  public static void renderBackground(SKVideoDecoder decoder) {

    var ctx = SkiaContext.get();
    ctx.begin();
    var frameContext = new FrameContext();

    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setBlendMode(BlendMode.SRC_OVER);
      try {

        var texture = decoder.computeFrame();

        if (texture == null) {
          return;
        }

        // Clip image
        float texWidth = texture.getWidth();
        float texHeight = texture.getHeight();
        float frameWidth = frameContext.getScaledWidth();
        float frameHeight = frameContext.getScaledHeight();

        float texAspect = texWidth / texHeight;
        float frameAspect = frameWidth / frameHeight;
        Rect dstRect = Rect.makeXYWH(0, 0, frameWidth, frameHeight);

        Rect srcRect;

        if (Math.abs(texAspect - frameAspect) < 1e-5) {
          srcRect = Rect.makeWH(texWidth, texHeight);
        } else if (texAspect > frameAspect) {
          float targetTexWidth = frameAspect * texHeight;

          float xOffset = (texWidth - targetTexWidth) / 2.0f;
          srcRect = Rect.makeXYWH(xOffset, 0, targetTexWidth, texHeight);
        } else {
          float targetTexHeight = texWidth / frameAspect;
          float yOffset = (texHeight - targetTexHeight) / 2.0f;
          srcRect = Rect.makeXYWH(0, yOffset, texWidth, targetTexHeight);
        }
        ctx.canvas().clear(0);
        ctx.canvas().drawImageRect(texture, srcRect, dstRect, SamplingMode.LINEAR, paint, true);

      } catch (IOException e) {
        System.err.println("Error computing frame for background:");
        e.printStackTrace();
      }
    }
    ctx.end();
    GlStateManager._blendFuncSeparate(
        GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
  }
}
