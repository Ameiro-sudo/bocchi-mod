package me.baier.client.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import me.baier.client.Bocchi;
import me.baier.client.mod.Mod;
import me.baier.client.mod.Mods;
import me.baier.client.mod.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CfgsRoundtripTest {

  @TempDir Path tempHome;

  /** 面板外的通用改值口: 走真实 JSON 加载路径 (enum 按名匹配 / number 类型保真转换)。 */
  private static void setByJson(Setting<?> setting, Object value) {
    JsonObject json = new JsonObject();
    if (value instanceof Number n) {
      json.addProperty("value", n);
    } else {
      json.addProperty("value", String.valueOf(value));
    }
    setting.load(json);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void setValue(Setting setting, Object value) {
    // Setting<?> 的捕获类型无法在调用点证明, 测试里以裸类型直写
    setting.setValue(value);
  }

  private static Mod mod(String label) {
    return Mods.INSTANCE.getMods().stream()
        .filter(m -> m.getLabel().equals(label))
        .findFirst()
        .orElseThrow();
  }

  private static Setting<?> setting(Mod mod, String label) {
    return mod.groups.stream()
        .flatMap(g -> g.getSettings().stream())
        .filter(s -> s.getLabel().equals(label))
        .findFirst()
        .orElseThrow(() -> new AssertionError("setting not found: " + label));
  }

  @Test
  void saveThenReloadRestoresMutatedState() throws Exception {
    // user.home 在 getBase() 内实时读取, 重定向到临时目录避免污染真实 ~/.bocchi
    System.setProperty("user.home", tempHome.toString());

    Bocchi.INSTANCE.start(); // 注册 Aura + 初始化 Cfgs (写入并加载 default.json)
    assertEquals("default", Cfgs.INSTANCE.getActiveName());
    assertTrue(Files.exists(tempHome.resolve(".bocchi/cfgs/default.json")));

    Mod aura = mod("Aura");
    Setting<?> enabled = setting(aura, "Enabled");
    Setting<?> mode = setting(aura, "Mode");
    Setting<?> radius = setting(aura, "Radius");

    // 用户在面板里改值 (enum 走按名加载路径)
    setValue(enabled, Boolean.FALSE);
    setByJson(mode, "HOLD");
    setByJson(radius, 6);
    Cfgs.INSTANCE.save();

    // 文件结构对称断言: name + Aura 节点 + 每个 setting 一个 value 键
    String raw = Files.readString(tempHome.resolve(".bocchi/cfgs/default.json"));
    JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
    assertEquals("default", root.get("name").getAsString());
    assertNotNull(root.getAsJsonObject("Aura"));
    assertEquals(6, root.getAsJsonObject("Aura").getAsJsonObject("Radius").get("value").getAsInt());
    assertEquals("HOLD", root.getAsJsonObject("Aura").getAsJsonObject("Mode").get("value").getAsString());

    // 模拟面板内继续乱改之后游戏重启: 状态被破坏后从磁盘重扫恢复
    setValue(enabled, Boolean.TRUE);
    setByJson(radius, 99);
    Cfgs.INSTANCE.initialize();

    assertEquals(Boolean.FALSE, enabled.getValue());
    assertEquals(6, ((Number) radius.getValue()).intValue());
    assertEquals("HOLD", String.valueOf(mode.getValue()));
    assertEquals("default", Cfgs.INSTANCE.getActiveName());
  }
}