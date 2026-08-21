package me.baier.client.ui.theme;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import me.baier.design.Design;

/**
 * 主题注册表. 优先级: ~/.bocchi/theme.json (游戏内切换, 持久化) > design.json "menu.theme" (材质包可覆盖).
 * 新增主题: 实现 {@link Theme} 并在静态块注册一行即可.
 */
public final class ThemeManager {

  private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
  private static final String DEFAULT_THEME = "misayos";
  private static final String THEME_FILE = "theme.json";

  /** 持久化的用户选择, null = 未设置过 (走 design.json). */
  private static volatile String persisted;

  static {
    register(new MisayosTheme());
    register(new PoulsenTheme());
    loadPersisted();
  }

  private ThemeManager() {}

  public static void register(Theme theme) {
    THEMES.put(theme.id(), theme);
  }

  private static Path getThemeFile() {
    return Paths.get(System.getProperty("user.home"), ".bocchi", THEME_FILE);
  }

  private static void loadPersisted() {
    try {
      Path file = getThemeFile();
      if (Files.exists(file)) {
        try (var reader = Files.newBufferedReader(file)) {
          JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
          String id = json.get("theme").getAsString();
          if (THEMES.containsKey(id)) {
            persisted = id;
          }
        }
      }
    } catch (Exception e) {
      // 配置损坏不致命, 走默认
    }
  }

  /** 当前主题: 持久化优先, 未设置时走 design.json, 非法 id 回退默认. */
  public static Theme get() {
    String id = persisted != null ? persisted : Design.value("menu.theme", DEFAULT_THEME);
    Theme theme = THEMES.get(id);
    return theme != null ? theme : THEMES.get(DEFAULT_THEME);
  }

  public static String currentId() {
    return get().id();
  }

  /** 切换主题并持久化到 ~/.bocchi/theme.json. */
  public static void set(String id) {
    if (!THEMES.containsKey(id)) {
      return;
    }
    persisted = id;
    try {
      Path file = getThemeFile();
      Files.createDirectories(file.getParent());
      JsonObject obj = new JsonObject();
      obj.addProperty("theme", id);
      Files.write(file, obj.toString().getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      // 写失败不致命, 本次会话仍生效
    }
  }

  /** 清除持久化主题, 回到 design.json "menu.theme" (材质包可覆盖). */
  public static void reset() {
    persisted = null;
    try {
      Files.deleteIfExists(getThemeFile());
    } catch (Exception e) {
      // 删除失败不致命, 下次 set 会覆盖
    }
  }

  /** 下一个主题 id (按注册顺序循环), 不改变当前选择; 供按钮文案等 UI 预告用. */
  public static String nextId() {
    String current = currentId();
    var it = THEMES.keySet().iterator();
    while (it.hasNext()) {
      if (it.next().equals(current)) {
        return it.hasNext() ? it.next() : THEMES.keySet().iterator().next();
      }
    }
    return THEMES.keySet().iterator().next();
  }

  /** 切换到下一个主题 (注册顺序), 返回切换后的主题. */
  public static Theme toggle() {
    String next = nextId();
    set(next);
    return THEMES.get(next);
  }

  public static Map<String, Theme> themes() {
    return Collections.unmodifiableMap(THEMES);
  }
}
