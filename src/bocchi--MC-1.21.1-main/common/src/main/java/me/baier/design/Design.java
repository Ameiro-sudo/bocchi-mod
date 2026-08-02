package me.baier.design;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import me.baier.utils.ResPack;
import net.minecraft.resources.ResourceLocation;

/**
 * bocchi 设计模板加载器.
 *
 * <p>所有设计资源 (纹理/SVG/字体/视频/动画/色板) 的路径都集中在本类管理. 默认值来自内置
 * design.json 模板, 材质包作者在材质包里放 <code>assets/minecraft/client/design.json</code>
 * 即可覆盖任意项, 其余自动回退默认. 以 <code>_</code> 开头的 JSON 键是注释, 会被忽略.
 */
public final class Design {

  /** 设计模板在资源管理器中的位置 (材质包/Mod 资源都可覆盖). */
  public static final ResourceLocation TEMPLATE = ResourceLocation.parse("client/design.json");

  private static final String CLASSPATH_TEMPLATE = "/assets/minecraft/client/design.json";
  private static final String[] SECTIONS = {
    "textures", "svgs", "fonts", "media", "animations", "colors"
  };

  /** key = "section.key" (如 "textures.bocchi"), value = 路径字符串. */
  private static final Map<String, String> DEFAULTS = new HashMap<>();
  private static final Map<String, String> OVERRIDES = new HashMap<>();
  private static volatile boolean loaded;

  static {
    registerDefaults();
  }

  private Design() {}

  private static void registerDefaults() {
    putDefault("textures.bocchi", "client/textures/bocchi.png");
    putDefault("textures.bocchi_loading", "client/textures/bocchi_loading.png");
    putDefault("textures.logo", "client/textures/logo.png");
    putDefault("textures.gotoh", "client/textures/gotoh.png");
    putDefault("textures.gotoh_image_1", "client/textures/gotoh_image_1.png");
    putDefault("textures.gotoh_image_2", "client/textures/gotoh_image_2.png");

    putDefault("svgs.lang", "client/svgs/lang.svg");
    putDefault("svgs.multi", "client/svgs/multi.svg");
    putDefault("svgs.option", "client/svgs/option.svg");
    putDefault("svgs.quit", "client/svgs/quit.svg");
    putDefault("svgs.single", "client/svgs/single.svg");
    putDefault("svgs.theme", "client/svgs/theme.svg");

    putDefault("fonts.Radikal-Black", "client/fonts/radikal-black.ttf");
    putDefault("fonts.Radikal-Regular", "client/fonts/radikal-regular.ttf");
    putDefault("fonts.meiryo-bold", "client/fonts/meiryo-bold.ttf");
    putDefault("fonts.SourceHanSansSC-Light", "client/fonts/sourcehansanssc-light.ttf");
    putDefault("fonts.SourceHanSansSC-Regular", "client/fonts/sourcehansanssc-regular.ttf");
    putDefault("fonts.SourceHanSansSC-Heavy", "client/fonts/sourcehansanssc-heavy.ttf");
    putDefault("fonts.SourceHanSansSC-Normal", "client/fonts/sourcehansanssc-normal.ttf");
    putDefault("fonts.SourceHanSansSC-Bold", "client/fonts/sourcehansanssc-bold.ttf");

    putDefault("colors.vinyl_edge", "#050505");
    putDefault("colors.vinyl_base", "#1A1A1A");
    putDefault("colors.vinyl_shine_1", "#33FFFFFF");
    putDefault("colors.vinyl_shine_2", "#1AFFFFFF");
    putDefault("colors.vinyl_shine_3", "#001A1A1A");
    putDefault("colors.vinyl_groove", "#1FFFFFFF");
    putDefault("colors.vinyl_label", "#981A1A1A");
  }

  private static void putDefault(String key, String value) {
    DEFAULTS.put(key, value);
  }

  /** 强制下次访问时重新读取材质包里的 design.json (如资源重载后调用). */
  public static void reload() {
    loaded = false;
  }

  /** 按 "section.key" 取资源路径, 材质包优先, 无覆盖时回退内置默认. */
  public static ResourceLocation resource(String key) {
    ensureLoaded();
    String path = OVERRIDES.getOrDefault(key, DEFAULTS.get(key));
    if (path == null) return null;
    try {
      return ResourceLocation.parse(path);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** 取资源路径, 未定义时回退调用方给的默认值. */
  public static ResourceLocation resource(String key, ResourceLocation fallback) {
    ResourceLocation res = resource(key);
    return res != null ? res : fallback;
  }

  /** 按 "colors.xxx" 取色板, 支持 #RRGGBB / #AARRGGBB, 未定义或非法时回退 fallback. */
  public static int color(String key, int fallback) {
    ensureLoaded();
    String value = OVERRIDES.getOrDefault(key, DEFAULTS.get(key));
    if (value == null) return fallback;
    try {
      String hex = value.trim().startsWith("#") ? value.trim().substring(1) : value.trim();
      long parsed = Long.parseLong(hex, 16);
      if (hex.length() == 6) parsed |= 0xFF000000L;
      return (int) parsed;
    } catch (RuntimeException e) {
      return fallback;
    }
  }

  /** 取任意字符串配置 (如 "menu.theme"), 未定义时回退 fallback. */
  public static String value(String key, String fallback) {
    ensureLoaded();
    String v = OVERRIDES.getOrDefault(key, DEFAULTS.get(key));
    return v != null ? v : fallback;
  }
  private static void ensureLoaded() {
    if (loaded) return;
    loaded = true; // 防递归
    OVERRIDES.clear();
    try (InputStream in = ResPack.open(TEMPLATE, CLASSPATH_TEMPLATE)) {
      JsonObject root = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
      for (String section : SECTIONS) {
        if (!root.has(section) || !root.get(section).isJsonObject()) continue;
        JsonObject obj = root.getAsJsonObject(section);
        for (String key : obj.keySet()) {
          if (key.startsWith("_")) continue; // 注释键
          JsonElement element = obj.get(key);
          if (element.isJsonPrimitive()) {
            OVERRIDES.put(section + "." + key, element.getAsString());
          }
        }
      }
    } catch (Exception e) {
      // 内置模板缺失或语法错误 (如 JsonParseException) 也不致命, 全部走 DEFAULTS
    }
  }
}
