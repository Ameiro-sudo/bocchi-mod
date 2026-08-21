package me.baier.graphics;

import aka.bocchi.injection.mixins.interfaces.IAccessorTextureManager;
import com.mojang.blaze3d.opengl.GlTexture;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import me.baier.client.ClientInstance;
import me.baier.utils.ColorUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author AquaVase Created on 7/10/2024
 */
@UtilityClass
public class SkiaRenderEngine implements ClientInstance {
  /** 纹理缓存 (LRU 访问序), 上限 MAX_CACHE_ENTRIES, 超限淘汰最久未用的并 close. */
  private static final int MAX_CACHE_ENTRIES = 256;
  private static final int TRIM_TO_ENTRIES = 128;

  private static final Logger LOGGER = LoggerFactory.getLogger(SkiaRenderEngine.class);

  private final Map<String, Image> TEXTURE_MAP = new LinkedHashMap<>(64, 0.75f, true);

  /** 加载失败的资源: 缓存失败态避免每帧重试 IO 与日志刷屏; 资源重载时随纹理缓存一并清空. */
  private static final Set<String> FAILED_RESOURCES = new HashSet<>();

  public void clearTextureCache() {
    TEXTURE_MAP.values().forEach(Image::close);
    TEXTURE_MAP.clear();
    FAILED_RESOURCES.clear();
  }

  /** 记录一次资源加载失败并缓存失败态, 本会话内不再重试该资源. */
  private void markFailed(ResourceLocation res, IOException e) {
    if (FAILED_RESOURCES.add(res.toString())) {
      LOGGER.warn("bocchi: resource load failed, skipping for this session: {} ({})", res, e);
    }
  }

  private void putCached(String key, Image image) {
    Image old = TEXTURE_MAP.put(key, image);
    if (old != null && old != image) {
      old.close();
    }
    while (TEXTURE_MAP.size() > MAX_CACHE_ENTRIES) {
      var it = TEXTURE_MAP.entrySet().iterator();
      var oldest = it.next();
      oldest.getValue().close();
      it.remove();
      if (TEXTURE_MAP.size() <= TRIM_TO_ENTRIES) {
        break;
      }
    }
  }

  public void render(SkiaCallback drawable) {
    try (Paint paint = new Paint()) {
      drawable.apply(paint);
    }
  }

  public void drawRect(float x, float y, float width, float height, java.awt.Color color) {
    drawRect(x, y, width, height, color.getRGB());
  }

  public void drawRect(float x, float y, float width, float height, int color) {
    try (Paint paint = new Paint()) {
      paint.setColor(color);
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawRect(Rect.makeXYWH(x, y, width, height), paint);
    }
  }

  public void drawRectLTRB(float x, float y, float x1, float y1, int color) {
    try (Paint paint = new Paint()) {
      paint.setColor(color);
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawRect(Rect.makeLTRB(x, Math.min(y, y1), x1, Math.max(y, y1)), paint);
    }
  }

  public void drawRectWithStroke(
      float x, float y, float width, float height, float stokeWidth, int color) {
    try (Paint paint = new Paint()) {
      paint.setColor(color);
      paint.setStroke(true);
      paint.setStrokeWidth(stokeWidth);
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawRect(Rect.makeXYWH(x, y, width, height), paint);
    }
  }

  public void drawRoundRect(
      float x, float y, float width, float height, float radius, java.awt.Color color) {
    drawRoundRect(x, y, width, height, radius, color, SkiaCallback.DEFAULT);
  }

  public void drawRoundRect(float x, float y, float width, float height, float radius, int color) {
    drawRoundRect(x, y, width, height, radius, color, SkiaCallback.DEFAULT);
  }

  public void drawRoundRect(
      Canvas canvas, float x, float y, float width, float height, float radius, int color) {
    drawRoundRect(canvas, x, y, width, height, radius, color, SkiaCallback.DEFAULT);
  }

  public void drawRoundRect(
      float x,
      float y,
      float width,
      float height,
      float radius,
      java.awt.Color color,
      SkiaCallback callback) {
    drawRoundRect(x, y, width, height, radius, color.getRGB(), callback);
  }

  public void drawRoundRect(
      float x, float y, float width, float height, float radius, int color, SkiaCallback callback) {
    drawRoundRect(SkiaContext.get().canvas(), x, y, width, height, radius, color, callback);
  }

  public void drawRoundRect(
      Canvas canvas,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      SkiaCallback callback) {
    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setColor(color);
      callback.apply(paint);
      canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
    }
  }

  public void drawRoundRectWithShdaow(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float shadowRadius,
      java.awt.Color color) {
    Canvas canvas = SkiaContext.get().canvas();
    canvas.drawRectShadow(
        RRect.makeXYWH(x, y, width, height, radius), 0, 0, shadowRadius, color.getRGB());
  }

  public void drawRoundRect(
      float x,
      float y,
      float width,
      float height,
      float tlRad,
      float trRad,
      float brRad,
      float blRad,
      java.awt.Color color,
      SkiaCallback callback) {
    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setColor(color.getRGB());
      callback.apply(paint);
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawRRect(RRect.makeXYWH(x, y, width, height, tlRad, trRad, brRad, blRad), paint);
    }
  }

  public void drawRoundRectWithGradient(
      float x,
      float y,
      float width,
      float height,
      float radius,
      java.awt.Color color,
      java.awt.Color color2,
      SkiaCallback callback) {
    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setColor(color.getRGB());
      float max = Math.max(width, height);
      try (Shader shader =
          linearCS(
              x + width / 2.0F - max / 2.0F,
              y + height / 2.0F - max / 2.0F,
              x + width / 2.0F + max / 2.0F,
              y + height / 2.0F + (max + 2.0F),
              color.getRGB(),
              color2.getRGB(),
              ColorSpace.getSRGBLinear(),
              10)) {
        paint.setShader(shader);
        callback.apply(paint);
      }
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
    }
  }

  public void drawRoundRectWithGradient(
      float x,
      float y,
      float width,
      float height,
      float radius,
      java.awt.Color color,
      java.awt.Color color2) {
    drawRoundRectWithGradient(x, y, width, height, radius, color, color2, SkiaCallback.DEFAULT);
  }

  public void drawRoundRectOutlineWithGradient(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float strokeWidth,
      java.awt.Color color,
      java.awt.Color color2) {
    drawRoundRectOutlineWithGradient(
        x, y, width, height, radius, strokeWidth, color, color2, SkiaCallback.DEFAULT);
  }

  public void drawRoundRectOutlineWithGradient(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float strokeWidth,
      java.awt.Color color,
      java.awt.Color color2,
      SkiaCallback callback) {
    try (Paint paint = new Paint()) {
      paint.setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth);
      paint.setAntiAlias(true);
      paint.setColor(color.getRGB());
      float max = Math.max(width, height);
      paint.setShader(
          Shader.makeLinearGradient(
              x + width / 2.0F - max / 2.0F,
              y + height / 2.0F - max / 2.0F,
              x + width / 2.0F + max / 2.0F,
              y + height / 2.0F + (max + 2.0F),
              new int[] {color.getRGB(), color2.getRGB()},
              null,
              GradientStyle.DEFAULT));
      callback.apply(paint);
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
    }
  }

  public void drawCircle(
      float x, float y, float radius, java.awt.Color color, SkiaCallback callback) {
    drawCircle(x, y, radius, color.getRGB(), SkiaCallback.DEFAULT);
  }

  public void drawCircle(float x, float y, float radius, int color, SkiaCallback callback) {
    try (Paint paint = new Paint()) {
      callback.apply(paint);
      paint.setColor(color);
      Canvas canvas = SkiaContext.get().canvas();
      canvas.drawCircle(x, y, radius, paint);
    }
  }

  public void drawCircle(float x, float y, float radius, java.awt.Color color) {
    drawCircle(x, y, radius, color, SkiaCallback.DEFAULT);
  }

  public void drawCircle(float x, float y, float radius, int color) {
    drawCircle(x, y, radius, color, SkiaCallback.DEFAULT);
  }

  public Image getImageFromResource(ResourceLocation res) {
    if (FAILED_RESOURCES.contains(res.toString())) {
      return null;
    }
    if (!TEXTURE_MAP.containsKey(res.toString())) {

      try (InputStream inputStream =
          ((IAccessorTextureManager) mc.getTextureManager())
              .getResourceContainer()
              .getResourceOrThrow(res)
              .open()) {
        var image = Image.makeDeferredFromEncodedBytes(inputStream.readAllBytes());
        putCached(res.toString(), image);
        return image;
      } catch (IOException e) {
        markFailed(res, e);
        return null;
      }
    }
    return TEXTURE_MAP.get(res.toString());
  }

  public void drawRoundedHead(
      AbstractClientPlayer entity,
      float x,
      float y,
      float width,
      float height,
      float radius,
      float alpha) {
    var res = entity.getSkin();
    var abstractTexture = mc.getTextureManager().getTexture(res.texture());

    Canvas canvas = SkiaContext.get().canvas();

    if (!TEXTURE_MAP.containsKey(res.toString())) {
      putCached(res.toString(), convertToSkiaImage(abstractTexture));

      Image texture = TEXTURE_MAP.get(res.toString());

      try (Paint paint = new Paint()) {
        //    paint.setAntiAlias(true);
        paint.setAlphaf(alpha);
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), true);
        canvas.drawImageRect(
            texture, Rect.makeXYWH(8, 8, 8, 8), Rect.makeXYWH(x, y, width, height), paint);
        canvas.restore();
      }

      drawRoundRect(
          x,
          y,
          width,
          height,
          radius,
          ColorUtil.applyOpacity(
              new java.awt.Color(255, 0, 0, Math.min(220, entity.hurtTime * 12)), alpha));
    }
  }

  @SneakyThrows
  public void drawImage(
      ResourceLocation res,
      float x,
      float y,
      float width,
      float height,
      float alpha,
      SkiaCallback callback) {
    if (res == null) return;

    Canvas canvas = SkiaContext.get().canvas();

    drawImage(canvas, res, x, y, width, height, alpha, callback);
  }

  public void drawImage(
      Canvas canvas,
      ResourceLocation res,
      float x,
      float y,
      float width,
      float height,
      float alpha,
      SkiaCallback callback)
      throws IOException {
    if (res == null) return;

    // 布局固定 480x270, 设备缩放比 = 窗口像素 / 布局 (均匀, 与 SkiaContext 拉伸一致)
    float uiScale = Math.min(mc.getWindow().getWidth() / 480f, mc.getWindow().getHeight() / 270f);
    // 预缩放: 仅当两个维度都明显缩小 (< 0.9) 且原图大于目标时, 按最大绘制边取 2 的幂预缩放,
    // 缩小交给 GPU MITCHELL 采样, 细线/勾线不再断裂产生条纹.
    // 目标按设备尺寸计算: 窗口放大后预缩放纹理足够大, 避免从过小纹理放大产生振铃/锯齿.
    // 原图直出 (竖长图/轻微缩放/放大) 统一存 #0 桶: 窗口尺寸变化不会重复解码, 与预缩放桶分离.
    int targetDim = (int) (Math.max(width, height) * uiScale);
    int targetW = 1;
    while (targetW < targetDim * 1.5f) targetW *= 2;
    String preKey = res + "#" + targetW;
    String rawKey = res + "#0";

    Image texture = TEXTURE_MAP.get(preKey);
    if (texture == null) {
      texture = TEXTURE_MAP.get(rawKey);
    }
    if (texture == null) {
      if (FAILED_RESOURCES.contains(res.toString())) {
        return;
      }
      Image decoded;
      try (InputStream inputStream =
          ((IAccessorTextureManager) mc.getTextureManager())
              .getResourceContainer()
              .getResourceOrThrow(res)
              .open()) {
        decoded = Image.makeDeferredFromEncodedBytes(inputStream.readAllBytes());
      } catch (IOException e) {
        markFailed(res, e);
        return;
      }
      float scaleW = width * uiScale / decoded.getWidth();
      float scaleH = height * uiScale / decoded.getHeight();
      boolean shouldPreScale =
          scaleW < 1f
              && scaleH < 1f
              && Math.min(scaleW, scaleH) < 0.9f
              && Math.max(decoded.getWidth(), decoded.getHeight()) > targetW;
      if (shouldPreScale) {
        float scale = targetW / (float) Math.max(decoded.getWidth(), decoded.getHeight());
        int pw = Math.max(1, Math.round(decoded.getWidth() * scale));
        int ph = Math.max(1, Math.round(decoded.getHeight() * scale));
        Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(pw, ph));
        try (Paint paint = new Paint()) {
          paint.setAntiAlias(true);
          surface
              .getCanvas()
              .drawImageRect(
                  decoded,
                  Rect.makeWH(decoded.getWidth(), decoded.getHeight()),
                  Rect.makeWH(pw, ph),
                  SamplingMode.MITCHELL,
                  paint,
                  true);
        }
        texture = surface.makeImageSnapshot();
        surface.close();
        decoded.close();
        putCached(preKey, texture);
      } else {
        texture = decoded;
        putCached(rawKey, texture);
      }
    }

    // 采样策略: 缩小用 MITCHELL (消条纹/摩尔纹), 放大用 LINEAR (更锐利, 少柔化)
    // 采样策略按设备尺寸判断: 缩小用 MITCHELL (消条纹/摩尔纹), 放大用 LINEAR (无振铃/锯齿)
    boolean scalingDown =
        texture.getWidth() >= width * uiScale && texture.getHeight() >= height * uiScale;
    SamplingMode sampling = scalingDown ? SamplingMode.MITCHELL : SamplingMode.LINEAR;
    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setAlphaf(alpha);
      callback.apply(paint);
      canvas.drawImageRect(
          texture,
          Rect.makeWH(texture.getWidth(), texture.getHeight()),
          Rect.makeXYWH(x, y, width, height),
          sampling,
          paint,
          true);
    }
  }

  @SneakyThrows
  public void drawImage(
      ResourceLocation res, float x, float y, float width, float height, float alpha) {

    drawImage(res, x, y, width, height, alpha, SkiaCallback.DEFAULT);
  }

  public void drawImage(
      ResourceLocation res,
      float x,
      float y,
      float width,
      float height,
      float u,
      float v,
      float uWidth,
      float vHeight,
      float alpha,
      SkiaCallback callback)
      throws IOException {
    if (res == null) return;

    Canvas canvas = SkiaContext.get().canvas();

    drawImage(canvas, res, x, y, width, height, u, v, uWidth, vHeight, alpha, callback);
  }

  @SneakyThrows
  public void drawImage(
      Canvas canvas,
      ResourceLocation res,
      float x,
      float y,
      float width,
      float height,
      float u,
      float v,
      float uWidth,
      float vHeight,
      float alpha,
      SkiaCallback callback) {
    if (res == null) return;

    if (!TEXTURE_MAP.containsKey(res.toString())) {
      if (FAILED_RESOURCES.contains(res.toString())) {
        return;
      }

      Image image;
      try (InputStream inputStream =
          ((IAccessorTextureManager) mc.getTextureManager())
              .getResourceContainer()
              .getResourceOrThrow(res)
              .open()) {
        image = Image.makeDeferredFromEncodedBytes(inputStream.readAllBytes());
      } catch (IOException e) {
        markFailed(res, e);
        return;
      }

      putCached(res.toString(), image);
    }

    Image texture = TEXTURE_MAP.get(res.toString());
    if (texture == null) {
      return;
    }

    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setAlphaf(alpha);

      // paint.setBlendMode(BlendMode.DST_OUT);
      callback.apply(paint);
      canvas.drawImageRect(
          texture,
          Rect.makeXYWH(u, v, uWidth, vHeight),
          Rect.makeXYWH(x, y, width, height),
          new FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
          paint,
          true);
    }
  }

  public void drawImage(
      ResourceLocation res,
      float x,
      float y,
      float width,
      float height,
      float u,
      float v,
      float uWidth,
      float vHeight,
      float alpha)
      throws IOException {
    drawImage(res, x, y, width, height, u, v, uWidth, vHeight, alpha, SkiaCallback.DEFAULT);
  }

  public int[] toIntArray(int... i) {
    return i;
  }

  @SneakyThrows
  public Image convertToSkiaImage(AbstractTexture texture) {
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, ((GlTexture) texture.getTexture()).glId());
    GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
    GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

    // Get the texture width and height
    int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
    int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

    // Allocate a buffer to hold the pixel data
    int size = width * height * 4;
    ByteBuffer buffer = BufferUtils.createByteBuffer(size); // 4 bytes per pixel (RGBA)
    byte[] data = new byte[size];
    GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_BYTE, buffer);
    buffer.get(data);

    Bitmap bitmap = new Bitmap();
    ImageInfo info = new ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE);
    bitmap.allocPixels(info);
    bitmap.installPixels(data);
    return Image.makeFromBitmap(bitmap);
  }

  public Shader linearCS(
      float x0,
      float y0,
      float x1,
      float y1,
      int colorFrom,
      int colorTo,
      ColorSpace cs,
      int steps) {
    Color4f from = ColorSpace.getSRGB().convert(cs, new Color4f(colorFrom));
    Color4f to = ColorSpace.getSRGB().convert(cs, new Color4f(colorTo));
    Color4f[] colors = new Color4f[steps + 1];
    float[] pos = new float[steps + 1];
    for (int i = 0; i < steps + 1; ++i) {
      pos[i] = (float) i / steps;
      colors[i] = from.makeLerp(to, pos[i]);
    }
    return Shader.makeLinearGradient(x0, y0, x1, y1, colors, cs, pos, GradientStyle.DEFAULT);
  }

  public Path drawSquircle(float x, float y, float w, float h, float r, java.awt.Color color) {
    return drawSquircle(x, y, w, h, r, color, SkiaCallback.DEFAULT);
  }

  public Path drawSquircle(
      float x, float y, float w, float h, float r, java.awt.Color color, SkiaCallback callback) {
    return drawSquircle(
        x,
        y,
        w,
        h,
        r,
        paint -> {
          paint.setColor(color.getRGB());
          callback.apply(paint);
        });
  }

  public Path drawSquircle(float x, float y, float w, float h, float r, SkiaCallback callback) {
    Path p = genSquirclePath(x, y, w, h, r);

    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      callback.apply(paint);
      SkiaContext.get().canvas().drawPath(p, paint);
    }

    return p;
  }

  public Path genSquirclePath(float x, float y, float w, float h, float r) {
    Path p = new Path();
    r = Math.min(w / 3.2f, Math.min(h / 3.2f, r));

    p.moveTo(x, y + 1.6f * r);
    p.cubicTo(x, y + 1.04f * r, x, y + 0.76f * r, x + 0.109f * r, y + 0.546f * r);
    p.cubicTo(
        x + 0.205f * r,
        y + 0.358f * r,
        x + 0.358f * r,
        y + 0.205f * r,
        x + 0.546f * r,
        y + 0.109f * r);
    p.cubicTo(x + 0.76f * r, y, x + 1.04f * r, y, x + 1.6f * r, y);

    p.lineTo(x + w - 1.6f * r, y);
    p.cubicTo(x + w - 1.04f * r, y, x + w - 0.76f * r, y, x + w - 0.546f * r, y + 0.109f * r);
    p.cubicTo(
        x + w - 0.358f * r,
        y + 0.205f * r,
        x + w - 0.205f * r,
        y + 0.358f * r,
        x + w - 0.109f * r,
        y + 0.546f * r);
    p.cubicTo(x + w, y + 0.76f * r, x + w, y + 1.04f * r, x + w, y + 1.6f * r);

    p.lineTo(x + w, y + h - 1.6f * r);
    p.cubicTo(
        x + w, y + h - 1.04f * r, x + w, y + h - 0.76f * r, x + w - 0.109f * r, y + h - 0.546f * r);
    p.cubicTo(
        x + w - 0.205f * r,
        y + h - 0.358f * r,
        x + w - 0.358f * r,
        y + h - 0.205f * r,
        x + w - 0.546f * r,
        y + h - 0.109f * r);
    p.cubicTo(x + w - 0.76f * r, y + h, x + w - 1.04f * r, y + h, x + w - 1.6f * r, y + h);

    p.lineTo(x + 1.6f * r, y + h);
    p.cubicTo(x + 1.04f * r, y + h, x + 0.76f * r, y + h, x + 0.546f * r, y + h - 0.109f * r);
    p.cubicTo(
        x + 0.358f * r,
        y + h - 0.205f * r,
        x + 0.205f * r,
        y + h - 0.358f * r,
        x + 0.109f * r,
        y + h - 0.546f * r);
    p.cubicTo(x, y + h - 0.76f * r, x, y + h - 1.04f * r, x, y + h - 1.6f * r);

    p.closePath();
    return p;
  }
}
