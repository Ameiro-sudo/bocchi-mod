package me.baier.graphics.font;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import me.baier.client.ClientInstance;
import me.baier.graphics.SkiaCallback;
import me.baier.graphics.SkiaContext;
import me.baier.utils.ColorUtil;
import net.minecraft.ChatFormatting;

public class SkiaFontRenderer implements IFontRenderer, ClientInstance {
  private final Map<String, Float> widthCache = new HashMap<>();
  @Getter private final Font font;
  private final float size;
  private final SkiaContext ctx = SkiaContext.get();

  //    private final SkiaFont parent;

  public SkiaFontRenderer(Font font, float size, SkiaFont parent) {
    //        this.parent = parent;
    this.font = font;
    this.size = size;
    this.font.setSubpixel(true);
    this.font.setMetricsLinear(true);

    /*        this.font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
    this.font.setHinting(FontHinting.FULL);*/

    //        fc = new FontCollection();
    //        fc.setDefaultFontManager(FontMgr.getDefault());
    //        TypefaceFontProvider fm = new TypefaceFontProvider();
    //        fm.registerTypeface(font.getTypeface(), null);
    //        fc.setAssetFontManager(fm);
  }

  public void drawStringWithFade(
      String text, double x, double y, float alpha, boolean shadow, SkiaCallback callback) {
    /*     Canvas canvas = SkiaContext.get().canvas();
    canvas.save();
    canvas.scale(0.5f, 0.5f);

    try (Paint paint = new Paint()) {
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            int color = shadow ? ColorUtil.getBlack(alpha * 0.65f).getRGB() : ColorUtil.applyOpacity(Theme.getClientColor(i), alpha);
            callback.apply(paint);
            drawStr(canvas, c, (float) x, (float) y, paint.setColor(color));
            x += getStringWidth(c);
        }
    }

    canvas.restore();*/
  }

  public void drawStringWithFade(String text, double x, double y, float alpha, boolean shadow) {
    /*   Canvas canvas = SkiaContext.get().canvas();
    canvas.save();
    canvas.scale(0.5f, 0.5f);

    try (Paint paint = new Paint()) {
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            int color = shadow ? ColorUtil.getBlack(alpha * 0.65f).getRGB() : ColorUtil.applyOpacity(Theme.getClientColor(i), alpha);
            drawStr(canvas, c, (float) x, (float) y, paint.setColor(color));
            x += getStringWidth(c);
        }
    }

    canvas.restore();*/
  }

  public int drawStringWithShadow(String name, float x, float y, int color) {
    float alpha = (float) (color >> 24 & 255) / 255f;
    drawString(name, x + .5f, y + .5f, new Color(0, 0, 0, alpha * 0.65f).getRGB(), true);
    return drawString(name, x, y, color, false);
  }

  public int drawString(String text, float x, float y, Color color) {
    return this.drawString(text, x, y, color.getRGB(), false);
  }

  public int drawString(String text, float x, float y, int color) {
    return this.drawString(text, x, y, color, false);
  }

  public int drawString(String text, float x, float y, int color, float spacing) {
    return this.drawString(text, x, y, color, false, spacing, SkiaCallback.DEFAULT);
  }

  public int drawString(
      String text, double x, double y, int color, boolean shadow, SkiaCallback callback) {
    return drawString(text, x, y, color, shadow, 0, callback);
  }

  public int drawString(
      String text,
      double x,
      double y,
      int color,
      boolean shadow,
      float spacing,
      SkiaCallback callback) {

    Canvas canvas = ctx.canvas();
    canvas.save();
    canvas.scale(0.5f, 0.5f);
    String[] strings = text.split("§");

    boolean finished = false;

    try (Paint paint = new Paint()) {

      callback.apply(paint);

      if (!text.contains("§")) {
        if (spacing != 0) {
          for (int i = 0; i < text.length(); i++) {
            var c = String.valueOf(text.charAt(i));
            drawStr(canvas, c, x, y, paint.setColor(color));
            x += getStringWidth(c) + spacing;
          }

        } else {
          drawStr(canvas, text, x, y, paint.setColor(color));
        }
        finished = true;
      }

      if (!finished) {
        if (spacing != 0) {
          var str = strings[0];
          for (int j = 0; j < str.length(); j++) {
            var c = String.valueOf(str.charAt(j));
            drawStr(canvas, c, x, y, paint.setColor(color));
            x += getStringWidth(c) + spacing;
          }
        } else {
          drawStr(canvas, strings[0], x, y, paint.setColor(color));

          x += getStringWidth(strings[0]);
        }

        int rgba = color;
        float alpha = (float) (color >> 24 & 255) / 255f;
        for (int i = 1; i < strings.length; i++) {
          String str = strings[i];
          // 连续 "§§" 会切出空段, 跳过以避免 charAt(0) 越界
          if (str.isEmpty()) {
            continue;
          }
          char code = str.charAt(0);
          ChatFormatting formatting = ChatFormatting.getByCode(code);
          if (formatting != null) {
            if (formatting == ChatFormatting.RESET) {
              rgba = color;
            } else if (!shadow) {
              if (formatting.getColor() != null) {
                rgba = ColorUtil.applyOpacity(formatting.getColor(), alpha);
              }
            }
          }
          str = str.substring(1);
          if (spacing != 0) {
            for (int j = 0; j < str.length(); j++) {
              var c = String.valueOf(str.charAt(j));
              drawStr(canvas, c, x, y, paint.setColor(rgba));
              x += getStringWidth(c) + spacing;
            }
          } else {

            drawStr(canvas, str, x, y, paint.setColor(rgba));
            x += getStringWidth(str);
          }
        }
      }
    }

    canvas.restore();
    return (int) x;
  }

  public int drawString(String text, double x, double y, int color, boolean shadow) {
    return drawString(text, x, y, color, shadow, 0, SkiaCallback.DEFAULT);
  }

  private void drawStr(Canvas canvas, String text, double x, double y, Paint paint) {
    //        try (Paint paint = new Paint()){
    //            paint.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.SOLID, 8f));
    //            callback.apply(paint);

    canvas.drawString(text, (float) (x * 2), (float) (y + getHeight()) * 2, font, paint);

    //        }
  }

  public float getHeight() {
    FontMetrics metrics = font.getMetrics();
    return (metrics.getLeading() - metrics.getAscent() - metrics.getDescent()) / 2 /*+ 1*/;
  }

  public float getHalfHeight() {
    return getHeight() / 2f;
  }

  public float getStringWidth(String text) {
    if (text == null) return 0;
    return widthCache.computeIfAbsent(text, k -> font.measureTextWidth(k.replaceAll("§.", "")) / 2);
  }

  public float getStringWidth(String text, float spacing) {
    if (text == null) return 0;
    var width = getStringWidth(text);
    return width + spacing * text.length();
  }
}
