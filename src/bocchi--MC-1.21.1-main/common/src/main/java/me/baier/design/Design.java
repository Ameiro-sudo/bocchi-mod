package me.baier.design;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.baier.utils.ResPack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * bocchi 设计模板加载器.
 *
 * <p>所有设计资源 (纹理/SVG/字体/视频/动画/色板) 的路径都集中在本类管理. 默认值唯一来源是内置
 * <code>assets/minecraft/client/design.json</code>; 材质包作者在材质包里放同路径文件即可覆盖任意项
 * (支持只改几项, 其余自动回退 mod 内置默认). 以 <code>_</code> 开头的 JSON 键是注释, 会被忽略.
 */
public final class Design {

  private static final Logger LOGGER = LoggerFactory.getLogger(Design.class);

  /** 设计模板在资源管理器中的位置 (材质包/Mod 资源都可覆盖). */
  public static final ResourceLocation TEMPLATE = ResourceLocation.parse("client/design.json");

  private static final String CLASSPATH_TEMPLATE = "/assets/minecraft/client/design.json";

  /** key = "section.key" (如 "textures.bocchi"), value = 路径字符串. */
  private static final Map<String, String> VALUES = new HashMap<>();
  private static final Set<String> WARNED_KEYS = new HashSet<>();
  private static volatile boolean loaded;

  private Design() {}

  /** 强制下次访问时重新读取 design.json (如资源重载后调用). */
  public static void reload() {
    loaded = false;
    // 重载后允许同一 key 再次告警, 防止问题被永久压制
    WARNED_KEYS.clear();
  }

  /** 按 "section.key" 取资源路径, 材质包优先, 未定义时返回 null. */
  public static ResourceLocation resource(String key) {
    String path = value(key, null);
    if (path == null) return null;
    try {
      return ResourceLocation.parse(path);
    } catch (RuntimeException e) {
      warnOnce(key, "路径非法, 已忽略: " + path);
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
    String value = value(key, null);
    if (value == null) return fallback;
    try {
      String hex = value.trim().startsWith("#") ? value.trim().substring(1) : value.trim();
      long parsed = Long.parseLong(hex, 16);
      if (hex.length() == 6) parsed |= 0xFF000000L;
      return (int) parsed;
    } catch (RuntimeException e) {
      warnOnce(key, "颜色格式非法 (需 #RRGGBB 或 #AARRGGBB): " + value);
      return fallback;
    }
  }

  /** 取任意字符串配置 (如 "menu.theme"), 未定义时回退 fallback. */
  public static String value(String key, String fallback) {
    ensureLoaded();
    return VALUES.getOrDefault(key, fallback);
  }

  private static synchronized void ensureLoaded() {
    if (loaded) return;
    VALUES.clear();
    List<Resource> stack = ResPack.getAll(TEMPLATE);
    if (stack.isEmpty()) {
      // 游戏未就绪 (ResourceManager 为空) 时回退 classpath 内置模板
      try (InputStream in = ResPack.class.getResourceAsStream(CLASSPATH_TEMPLATE)) {
        if (in == null) {
          LOGGER.error("内置 design.json 缺失: {}", CLASSPATH_TEMPLATE);
          return;
        }
        merge(in);
      } catch (Exception e) {
        LOGGER.error("内置 design.json 读取失败", e);
        return;
      }
    } else {
      // 资源包栈从低到高合并: mod 内置在最底层, 材质包在上层, 后者覆盖前者
      for (int i = stack.size() - 1; i >= 0; i--) {
        Resource res = stack.get(i);
        try (InputStream in = res.open()) {
          merge(in);
        } catch (Exception e) {
          LOGGER.error("design.json 解析失败", e);
        }
      }
    }
    loaded = true;
  }

  private static void merge(InputStream in) {
    JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    for (Map.Entry<String, JsonElement> sectionEntry : root.entrySet()) {
      String section = sectionEntry.getKey();
      if (section.startsWith("_") || !sectionEntry.getValue().isJsonObject()) continue; // 注释键
      JsonObject obj = sectionEntry.getValue().getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
        String key = entry.getKey();
        if (key.startsWith("_")) continue; // 注释键
        JsonElement element = entry.getValue();
        if (element.isJsonPrimitive()) {
          VALUES.put(section + "." + key, element.getAsString());
        }
      }
    }
  }

  private static void warnOnce(String key, String message) {
    if (WARNED_KEYS.add(key)) {
      LOGGER.warn("design.json [{}] {}", key, message);
    }
  }
}
