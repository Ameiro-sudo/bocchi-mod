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
import lombok.SneakyThrows;
import me.baier.client.ui.mainmenu.misayos.MainTachieComponent;
import me.baier.design.Design;
import me.baier.graphics.SkiaCallback;
import me.baier.graphics.SkiaRenderEngine;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public class AlbumRenderer {

  private static final ResourceLocation RECORD_TEXTURE = Design.resource("textures.bocchi");

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

  @SneakyThrows
  public static void render(Canvas canvas, Rect dst) {
    float dstCenterX = dst.getLeft() + dst.getWidth() / 2f;
    float dstCenterY = dst.getTop() + dst.getHeight() / 2f;

    canvas.save();

    canvas.translate(dstCenterX, dstCenterY);
    long l = System.currentTimeMillis() / 50 % 360;
    canvas.rotate(l);
    canvas.translate(-dstCenterX, -dstCenterY);

    int vinylBaseColor = Design.color("colors.vinyl_base", 0xFF1A1A1A);

    float baseSize = Math.min(dst.getWidth(), dst.getHeight());
    float recordOuterRadius = baseSize * 0.48f;
    float albumArtDiscRadius = recordOuterRadius * 0.98f;
    float albumArtRadius = recordOuterRadius * 0.65f;
    float labelRadius = recordOuterRadius * 0.22f;

    try (Paint edgePaint = new Paint().setColor(Design.color("colors.vinyl_edge", 0xFF050505)).setAntiAlias(true)) {
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
              new int[] {
                Design.color("colors.vinyl_shine_1", 0x33FFFFFF),
                Design.color("colors.vinyl_shine_2", 0x1AFFFFFF),
                Design.color("colors.vinyl_shine_3", 0x001A1A1A)
              },
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

    try (Paint paint = new Paint().setColor(Design.color("colors.vinyl_label", 0x981A1A1A)).setAntiAlias(true)) {
      canvas.drawCircle(dstCenterX, dstCenterY, labelRadius + grooveStartOffset, paint);
    }
    try (Paint groovePaint =
        new Paint()
            .setColor(Design.color("colors.vinyl_groove", 0x1FFFFFFF))
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

    // 唱片封面: 走 drawImage 预缩放管线 (MITCHELL 缩小 + 缓存), 避免 shader 直接大比例
    // 缩小无 mipmap 采样产生摩尔纹/条纹 (旋转时更明显)
    var albumArtImage = SkiaRenderEngine.getImageFromResource(RECORD_TEXTURE);
    if (albumArtImage != null) {
      float imgW = albumArtImage.getWidth();
      float imgH = albumArtImage.getHeight();
      float scale;

      if (imgW / imgH >= 1.0f) {
        scale = (2 * albumArtRadius) / imgH;
      } else {
        scale = (2 * albumArtRadius) / imgW;
      }

      float scaledWidth = imgW * scale * 1.6f;
      float scaledHeight = imgH * scale * 1.6f;

      // 坐标对齐原版 shader 语义 (makeTranslate(dx, dy - h/4.5).makePreScale(1.6)):
      // 偏移用未乘 1.6 的尺寸居中, 渲染再放大 1.6 倍 -> 图片中心偏右下方, 与游戏一致
      float dx = dstCenterX - imgW * scale / 2f;
      float dy = dstCenterY - imgH * scale / 2f - dst.getHeight() / 4.5f;

      Path circlePath = new Path().addCircle(dstCenterX, dstCenterY, albumArtRadius);
      canvas.save();
      canvas.clipPath(circlePath, ClipMode.INTERSECT, true);
      SkiaRenderEngine.drawImage(
          canvas, RECORD_TEXTURE, dx, dy, scaledWidth, scaledHeight, 1f, SkiaCallback.DEFAULT);
      canvas.restore();
      circlePath.close();
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
