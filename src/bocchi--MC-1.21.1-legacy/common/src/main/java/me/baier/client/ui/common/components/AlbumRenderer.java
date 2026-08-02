package me.baier.client.ui.common.components;

import aka.bocchi.injection.mixins.interfaces.IAccessorTextureManager;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.resources.CachingResourceProvider;
import io.github.humbleui.skija.resources.DataURIResourceProviderProxy;
import io.github.humbleui.skija.resources.FileResourceProvider;
import io.github.humbleui.skija.skottie.Animation;
import io.github.humbleui.skija.skottie.AnimationBuilder;
import io.github.humbleui.skija.skottie.AnimationBuilderFlag;
import io.github.humbleui.types.Rect;
import me.baier.client.ui.mainmenu.misayos.MainTachieComponent;
import me.baier.graphics.SkiaRenderEngine;
import net.minecraft.client.renderer.entity.TadpoleRenderer;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AlbumRenderer {

  private static final ResourceLocation RECORD_TEXTURE =
      ResourceLocation.parse("client/textures/bocchi.png");

  /*private static final Animation recordAnimation;

  static {
    try (var stream =
        MainTachieComponent.class.getResourceAsStream(
            "/assets/minecraft/client/animations/data.json")) {
      Path targetDir = Paths.get(System.getProperty("user.home")).resolve(".bocchi");
      var resourceProvider =
          CachingResourceProvider.make(
              DataURIResourceProviderProxy.make(
                  FileResourceProvider.make(targetDir.toString(), false), false));
      recordAnimation =
          new AnimationBuilder(
                  AnimationBuilderFlag.DEFER_IMAGE_LOADING,
                  AnimationBuilderFlag.PREFER_EMBEDDED_FONTS)
              .setResourceProvider(resourceProvider)
              .buildFromString(new String(stream.readAllBytes()));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }*/

  public static void render(Canvas canvas, Rect dst) {
    float dstCenterX = dst.getLeft() + dst.getWidth() / 2f;
    float dstCenterY = dst.getTop() + dst.getHeight() / 2f;

    canvas.save();

    canvas.translate(dstCenterX, dstCenterY);
    long l = System.currentTimeMillis() / 50 % 360;
    canvas.rotate(l);
    canvas.translate(-dstCenterX, -dstCenterY);

    int vinylBaseColor = 0xFF1A1A1A;

    float baseSize = Math.min(dst.getWidth(), dst.getHeight());
    float recordOuterRadius = baseSize * 0.48f;
    float albumArtDiscRadius = recordOuterRadius * 0.98f;
    float albumArtRadius = recordOuterRadius * 0.65f;
    float labelRadius = recordOuterRadius * 0.22f;

    try (Paint edgePaint = new Paint().setColor(0xFF050505).setAntiAlias(true)) {
      canvas.drawCircle(dstCenterX, dstCenterY, recordOuterRadius, edgePaint);
    }

    try (Paint vinylPaint = new Paint().setColor(vinylBaseColor).setAntiAlias(true)) {
      canvas.drawCircle(dstCenterX, dstCenterY, albumArtDiscRadius, vinylPaint);
    }

    try (var paint = new Paint().setColor(0x80FFFFFF).setAntiAlias(true)) {
      canvas.drawCircle(dstCenterX, dstCenterY, albumArtRadius, paint);
    }

    try (Paint shinePaint = new Paint().setAntiAlias(true)) {
      float shineOffsetX = dstCenterX + albumArtDiscRadius * 0.1f;
      float shineOffsetY = dstCenterY - albumArtDiscRadius * 0.3f;
      float shineEffectRadius = albumArtDiscRadius * 1.2f;

      try (Shader shineShader =
          Shader.makeRadialGradient(
              shineOffsetX,
              shineOffsetY,
              shineEffectRadius,
              new int[] {0x33FFFFFF, 0x1AFFFFFF, 0x001A1A1A},
              new float[] {0f, 0.4f, 1f},
              GradientStyle.DEFAULT
              )) {
        shinePaint.setShader(shineShader);
        canvas.drawCircle(dstCenterX, dstCenterY, albumArtDiscRadius, shinePaint);
      }
    }

    float grooveStrokeWidth = Math.max(0.5f, 0.75f * (baseSize / 600f));
    float grooveStep = Math.max(1.0f, 1.8f * (baseSize / 600f));
    float grooveStartOffset = Math.max(1.5f, 3f * (baseSize / 600f));

    try (Paint paint = new Paint().setColor(0x981A1A1A).setAntiAlias(true)) {
      canvas.drawCircle(dstCenterX, dstCenterY, labelRadius + grooveStartOffset, paint);
    }
    try (Paint groovePaint =
        new Paint()
            .setColor(0x1FFFFFFF)
            .setStroke(true)
            .setStrokeWidth(grooveStrokeWidth)
            .setAntiAlias(true)) {
      for (float r = labelRadius + grooveStartOffset;
          r < albumArtRadius - grooveStartOffset / 2f;
          r += grooveStep) {
        canvas.drawCircle(dstCenterX, dstCenterY, r, groovePaint);
      }
      for (float r = albumArtRadius + grooveStartOffset;
          r < albumArtDiscRadius - grooveStartOffset / 2f;
          r += grooveStep) {
        canvas.drawCircle(dstCenterX, dstCenterY, r, groovePaint);
      }
    }

    try (Paint albumArtPaint = new Paint().setAntiAlias(true)) {
      var albumArtImage = SkiaRenderEngine.getImageFromResource(RECORD_TEXTURE);
      float imgW = albumArtImage.getWidth();
      float imgH = albumArtImage.getHeight();
      float scale;

      if (imgW / imgH >= 1.0f) {
        scale = (2 * albumArtRadius) / imgH;
      } else {
        scale = (2 * albumArtRadius) / imgW;
      }

      float scaledWidth = imgW * scale;
      float scaledHeight = imgH * scale;

      float dx = dstCenterX - scaledWidth / 2f;
      float dy = dstCenterY - scaledHeight / 2f;

      Matrix33 shaderMatrix =
          Matrix33.makeTranslate(dx, dy - dst.getHeight() / 4.5f)
              .makePreScale(scale * 1.6f, scale * 1.6f);

      try (Shader albumShader =
          albumArtImage.makeShader(
              FilterTileMode.CLAMP, FilterTileMode.CLAMP, shaderMatrix)) {
        albumArtPaint.setShader(albumShader);
        canvas.drawCircle(dstCenterX, dstCenterY, albumArtRadius, albumArtPaint);
      }
    }

    float innerBorderWidth = Math.max(1f, 2f * (baseSize / 600f));
    try (Paint innerBorderPaint =
        new Paint().setColor(0xFFFFFFFF).setStroke(true).setStrokeWidth(5f).setAntiAlias(true)) {
      canvas.drawCircle(
          dstCenterX, dstCenterY, albumArtRadius - (innerBorderWidth / 2f), innerBorderPaint);
    }

    canvas.restore();
  }
}
