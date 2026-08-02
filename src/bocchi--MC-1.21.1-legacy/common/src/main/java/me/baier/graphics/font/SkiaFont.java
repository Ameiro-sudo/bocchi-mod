package me.baier.graphics.font;

import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author AquaVase Created on 7/10/2024
 */
@Accessors(chain = true)
@Getter
@Setter
public class SkiaFont {
  private final Map<Float, SkiaFontRenderer> rendererMap = new HashMap<>();
  private final Typeface typeface;
  private boolean fallback;
  private SkiaFont fallbackFont;

  public SkiaFont(String name) {
    FontSet.FONTS.add(name);

    InputStream inputStream =
        SkiaFont.class.getResourceAsStream("/assets/minecraft/client/fonts/" + name + ".ttf");

    if (inputStream == null) {
      throw new RuntimeException();
    }

    try {
      byte[] array = inputStream.readAllBytes();
      Data data = Data.makeFromBytes(array, 0, array.length);
      this.typeface = Typeface.makeFromData(data, 0);
      inputStream.close();
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  public SkiaFontRenderer getFont(float size) {
    if (!rendererMap.containsKey(size)) {
      rendererMap.put(size, new SkiaFontRenderer(new Font(typeface, size), size, this));
    }

    return rendererMap.get(size);
  }

  public void free() {}
}
