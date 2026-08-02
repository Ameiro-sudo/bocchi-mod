package me.baier.client.mod.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import me.baier.client.mod.Category;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.SettingGroup;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String label, Boolean defaultValue, Boolean value, SettingGroup parent) {
        super(label, defaultValue, value, parent);
    }

    @Override
    public JsonObject save(JsonObject json) {
        json.addProperty("value", value);
        return json;
    }

    @Override
    public void load(JsonObject json) {
        if (json.has("value")) {
            setValue(json.get("value").getAsBoolean());
        }
    }

    public static class Builder extends Setting.SettingBuilder<Builder, Boolean, BooleanSetting> {
        @Override
        public BooleanSetting build() {
            return new BooleanSetting(name, value, defaultValue, parent);
        }
    }
}
