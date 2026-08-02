package me.baier.client.ui.common.components;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import me.baier.design.Design;
import me.baier.graphics.SkiaCallback;
import me.baier.graphics.SkiaRenderEngine;
import net.minecraft.resources.ResourceLocation;

public class LogoRenderer {
  private static final String GLOW_RUNTIME_EFFECT =
      "uniform shader image;\n"
          + "    half4 main(float2 tex_coord) {\n"
          + "        float bloom_spread = 1.5;\n"
          + "        float bloom_intensity = 1;\n"
          + "        float uv_x = tex_coord.x;\n"
          + "        float uv_y = tex_coord.y;\n"
          + "        float4 sum = float4(0.0);\n"
          + "        for (int n = 0; n < 9; ++n) {\n"
          + "            float uv_y = (tex_coord.y) + (bloom_spread * float(n - 4));\n"
          + "            float4 h_sum = vec4(0.0);\n"
          + "            h_sum += image.eval(float2(uv_x - (4.0 * bloom_spread), uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x - (3.0 * bloom_spread), uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x - (2.0 * bloom_spread), uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x - bloom_spread, uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x, uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x + bloom_spread, uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x + (2.0 * bloom_spread), uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x + (3.0 * bloom_spread), uv_y));\n"
          + "            h_sum += image.eval(float2(uv_x + (4.0 * bloom_spread), uv_y));\n"
          + "            sum += h_sum / 9.0;\n"
          + "        }\n"
          + "        return image.eval(tex_coord) + ((sum / 9.0) * bloom_intensity);\n"
          + "    }";

  private static RuntimeEffect GLOW_EFFECT;

  static {
    GLOW_EFFECT = RuntimeEffect.makeForShader(GLOW_RUNTIME_EFFECT);
  }

  private static final ResourceLocation LOGO_TEXTURE = Design.resource("textures.logo");

  public static void renderLogoWithColor(
      Canvas canvas, float x, float y, float w, float h, int color) {
    renderLogoWithColor(canvas, x, y, w, h, color, SkiaCallback.DEFAULT);
  }

  public static void renderLogoWithColor(
      Canvas canvas, float x, float y, float w, float h, int color, SkiaCallback callback) {
    Image logoImage = null;
    try {
      logoImage = SkiaRenderEngine.getImageFromResource(LOGO_TEXTURE);
      if (logoImage == null) {
        System.err.println("Logo image not found: " + LOGO_TEXTURE);
        return;
      }

      Rect sourceRect = Rect.makeWH(logoImage.getWidth(), logoImage.getHeight());
      Rect destinationRect = Rect.makeXYWH(x, y, w, h);

      try (Paint imagePaint = new Paint()) {
        imagePaint.setColorFilter(ColorFilter.makeBlend(color, BlendMode.SRC_IN));
        imagePaint.setAntiAlias(true);

        if (callback != null) {
          callback.apply(imagePaint);
        }

        canvas.drawImageRect(
            logoImage, sourceRect, destinationRect, SamplingMode.LINEAR, imagePaint, true);
      }

    } catch (Exception e) {
      System.err.println("Error rendering logo with color: " + e.getMessage());
      e.printStackTrace();
    } finally {

    }
  }
}
