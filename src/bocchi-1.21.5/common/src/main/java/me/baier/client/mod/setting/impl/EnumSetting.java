package me.baier.client.mod.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.SettingGroup;

@Getter
public class EnumSetting <T extends Enum<T>> extends Setting<T> {

    private final T[] constants;

    public EnumSetting(String label, T defaultValue, T value, SettingGroup parent) {
        super(label, defaultValue, value, parent);
        this.constants = extractConstantsFromEnumValue(value);
    }

    public T[] extractConstantsFromEnumValue(T value) {
        return value.getDeclaringClass().getEnumConstants();
    }

    public void increment() {
        T currentValue = getValue();

        for (T constant : constants) {
            if (constant != currentValue) {
                continue;
            }

            T newValue;

            int ordinal = constant.ordinal();
            if (ordinal == constants.length - 1) {
                newValue = constants[0];
            } else {
                newValue = constants[ordinal + 1];
            }

            setValue(newValue);
            return;
        }
    }

    public void decrement() {
        T currentValue = getValue();

        for (T constant : constants) {
            if (constant != currentValue) {
                continue;
            }

            T newValue;

            int ordinal = constant.ordinal();
            if (ordinal == 0) {
                newValue = constants[constants.length - 1];
            } else {
                newValue = constants[ordinal - 1];
            }

            setValue(newValue);
            return;
        }
    }

    @Override
    public JsonObject save(JsonObject json) {
        json.addProperty("value", value.name());
        return json;
    }

    @Override
    public void load(JsonObject json) {
        if (json.has("value")) {
            String name = json.get("value").getAsString();
            for (T constant : constants) {
                if (constant.name().equalsIgnoreCase(name)) {
                    setValue(constant);
                }
            }
        }
    }

    public static class Builder<T extends Enum<T>> extends Setting.SettingBuilder<Builder<T>, T, EnumSetting<T>> {

        public Builder() {
        }

        public Builder<T> value(T value) {
            this.value = value;
            this.defaultValue = value;
            return this;
        }

        @Override
        public EnumSetting<T> build() {
            // Setting 构造器参数顺序为 (label, defaultValue, value, parent)
            return new EnumSetting<>(name, defaultValue, value, parent);
        }
    }
}
