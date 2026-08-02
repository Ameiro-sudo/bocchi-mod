package me.baier.graphics.font;

import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.baier.design.Design;
import me.baier.utils.ResPack;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
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

    try {
      ResourceLocation res =
          Design.resource(
              "fonts." + name,
              ResourceLocation.parse("client/fonts/" + name.toLowerCase() + ".ttf"));
      byte[] array =
          ResPack.readBytes(res, "/assets/minecraft/client/fonts/" + name.toLowerCase() + ".ttf");
      Data data = Data.makeFromBytes(array, 0, array.length);
      this.typeface = Typeface.makeFromData(data, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final int MAX_RENDERERS = 128;

  public SkiaFontRenderer getFont(float size) {
    if (!rendererMap.containsKey(size)) {
      // 防泄漏: 连续浮点字号 (悬停动画 lerp) 会产生无限个原生 Font, 超上限清理最旧的
      if (rendererMap.size() >= MAX_RENDERERS) {
        var oldest = rendererMap.entrySet().iterator().next();
        rendererMap.remove(oldest.getKey());
      }
      rendererMap.put(size, new SkiaFontRenderer(new Font(typeface, size), size, this));
    }

    return rendererMap.get(size);
  }

  public void free() {}
}
