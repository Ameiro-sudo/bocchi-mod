package me.baier.utils;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.graphics.font.SkiaFontRenderer;
import org.w3c.dom.css.CSSFontFaceRule;

public class RenderUtils {
  public static void drawTextBox(
      Canvas canvas,
      SkiaFontRenderer font,
      String text,
      float x,
      float y,
      int rectColor,
      int textColor,
      float spacing,
      float expandX,
      float expandY,
      float radius) {
    float textContentWidth = font.getStringWidth(text) + spacing * text.length();

    float boxFinalWidth = textContentWidth * expandX;
    float boxFinalHeight = font.getHeight() * expandY;

    float boxDrawX = x - textContentWidth * (expandX - 1) / 2.f;
    float boxDrawY = y - font.getHeight() * (expandY - 1) / 2.f;

    if (radius == 0) {
      SkiaRenderEngine.drawRect(boxDrawX, boxDrawY, boxFinalWidth, boxFinalHeight, rectColor);
    } else {
      SkiaRenderEngine.drawRoundRect(
          boxDrawX, boxDrawY, boxFinalWidth, boxFinalHeight, radius, rectColor);
    }
    font.drawString(text, x, y - font.getHalfHeight() / 4f, textColor, spacing);
  }

  public static void drawTextStrokeBox(
      Canvas canvas,
      SkiaFontRenderer font,
      String text,
      float x,
      float y,
      int rectColor,
      int textColor,
      float spacing,
      float expandX,
      float expandY,
      float radius,
      float storkeSize) {
    float textContentWidth = font.getStringWidth(text) + spacing * text.length();

    float boxFinalWidth = textContentWidth * expandX;
    float boxFinalHeight = font.getHeight() * expandY;

    float boxDrawX = x - textContentWidth * (expandX - 1) / 2.f;
    float boxDrawY = y - font.getHeight() * (expandY - 1) / 2.f;
    try (var paint = new Paint().setStroke(true).setStrokeWidth(storkeSize)) {
      if (radius == 0) {
        canvas.drawRect(
            Rect.makeXYWH(boxDrawX, boxDrawY, boxFinalWidth, boxFinalHeight),
            paint.setColor(rectColor));
        // SkiaRenderEngine.drawRect(boxDrawX, boxDrawY, boxFinalWidth, boxFinalHeight,
        // rectColor,paint -> );
      } else {
        canvas.drawRRect(
            RRect.makeXYWH(boxDrawX, boxDrawY, boxFinalWidth, boxFinalHeight, radius),
            paint.setColor(rectColor));
        /* SkiaRenderEngine.drawRoundRect(
        boxDrawX, boxDrawY, boxFinalWidth, boxFinalHeight, radius, rectColor);*/
      }
    }

    font.drawString(text, x, y - font.getHalfHeight() / 4f, textColor, spacing);
  }
}
