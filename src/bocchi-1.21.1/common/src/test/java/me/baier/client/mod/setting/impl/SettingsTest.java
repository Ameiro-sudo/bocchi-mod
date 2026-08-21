package me.baier.client.mod.setting.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import me.baier.client.mod.setting.SettingGroup;
import org.junit.jupiter.api.Test;

class SettingsTest {

  private static JsonObject roundtrip(JsonObject saved) {
    // 走真实 JSON 序列化文本, 模拟落盘再解析 (gson 数字反序列化为 LazilyParsedNumber)
    return JsonParser.parseString(saved.toString()).getAsJsonObject();
  }

  @Test
  void booleanBuilderKeepsValueAndDefault() {
    // 回归 745af6e: Builder.build() 曾把 value/defaultValue 传反, .value(true) 后真实值为 null
    BooleanSetting on = new BooleanSetting.Builder().label("B").value(true).build();
    assertEquals(Boolean.TRUE, on.getValue());
    assertEquals(Boolean.TRUE, on.getDefaultValue());

    BooleanSetting off = new BooleanSetting.Builder().label("B2").value(false).defaultValue(true).build();
    assertEquals(Boolean.FALSE, off.getValue());
    assertEquals(Boolean.TRUE, off.getDefaultValue());
  }

  @Test
  void numberSettingSaveLoadPreservesType() {
    NumberSetting<Integer> intSetting =
        new NumberSetting.Builder<Integer>().label("N").value(3).min(1).max(9).increment(1).build();
    assertEquals(1, intSetting.getMinimum());
    assertEquals(9, intSetting.getMaximum());

    JsonObject loaded = roundtrip(intSetting.save(new JsonObject()));
    intSetting.load(loaded);
    assertInstanceOf(Integer.class, intSetting.getValue());
    assertEquals(3, intSetting.getValue());
  }

  @Test
  void rangedSettingLoadDoesNotPolluteIntegerField() {
    RangedSetting<Integer> ranged =
        new RangedSetting.Builder<Integer>()
            .label("R")
            .value(4)
            .min(1)
            .max(8)
            .increment(1)
            .leftValue(0)
            .rightValue(16)
            .build();

    JsonObject json = JsonParser.parseString("{\"value\":7}").getAsJsonObject();
    ranged.load(json);

    // 修复前: gson 的 LazilyParsedNumber 被 (T) 强转塞入, instanceof Integer 永假, 再 save 即丢值
    assertInstanceOf(Integer.class, ranged.getValue());
    assertEquals(7, ranged.getValue());

    JsonObject reloaded = roundtrip(ranged.save(new JsonObject()));
    assertEquals(7, reloaded.get("value").getAsInt());
  }

  @Test
  void enumSettingCyclesAndLoadsCaseInsensitive() {
    SettingGroup group = new SettingGroup("g");
    EnumSetting<Mode> mode = group.add(new EnumSetting.Builder<Mode>().label("M").value(Mode.SWITCH).build());
    List<Mode> constants = List.of(Mode.values());
    assertEquals(3, constants.size());

    mode.increment();
    assertEquals(Mode.HOLD, mode.getValue());
    mode.increment();
    assertEquals(Mode.TOGGLE, mode.getValue());
    mode.increment(); // 环绕回第一个
    assertEquals(Mode.SWITCH, mode.getValue());

    mode.decrement();
    assertEquals(Mode.TOGGLE, mode.getValue());

    JsonObject json = JsonParser.parseString("{\"value\":\"hold\"}").getAsJsonObject();
    mode.load(json);
    assertEquals(Mode.HOLD, mode.getValue());

    // 非法值忽略, 保持现值
    mode.load(JsonParser.parseString("{\"value\":\"NOT_A_MODE\"}").getAsJsonObject());
    assertEquals(Mode.HOLD, mode.getValue());

    JsonObject saved = roundtrip(mode.save(new JsonObject()));
    assertFalse(saved.entrySet().isEmpty());
    assertTrue(saved.has("value"));
  }

  enum Mode {
    SWITCH,
    HOLD,
    TOGGLE
  }
}