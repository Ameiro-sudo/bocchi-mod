package me.baier.client.mod.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.SettingGroup;

@Getter
public class NumberSetting<T extends Number> extends Setting<T> {
    private final T minimum;
    private final T maximum;
    private final T increment;

    public NumberSetting(String label, T defaultValue, T value, SettingGroup parent, T minimum, T maximum, T increment) {
        super(label, defaultValue, value, parent);
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    @Override
    public JsonObject save(JsonObject json) {
        if (value instanceof Integer) {
            json.addProperty("value", value.intValue());
        } else if (value instanceof Double) {
            json.addProperty("value", value.doubleValue());
        } else if (value instanceof Float) {
            json.addProperty("value", value.floatValue());
        } else if (value instanceof Long) {
            json.addProperty("value", value.longValue());
        }
        return json;
    }

    @Override
    public void load(JsonObject json) {
        if (json.has("value")) {
            try {
                Number number = json.get("value").getAsNumber();
                // 按当前值的实际类型转换, 避免把 Double 塞进 Integer 字段造成堆污染/CCE
                if (value instanceof Integer) {
                    setValue((T) Integer.valueOf(number.intValue()));
                } else if (value instanceof Double) {
                    setValue((T) Double.valueOf(number.doubleValue()));
                } else if (value instanceof Float) {
                    setValue((T) Float.valueOf(number.floatValue()));
                } else if (value instanceof Long) {
                    setValue((T) Long.valueOf(number.longValue()));
                } else {
                    setValue((T) number);
                }
            } catch (Exception e) {
                // 保持默认值
            }
        }
    }

    public static class Builder<T extends Number> extends Setting.SettingBuilder<Builder<T>, T, NumberSetting<T>> {
        private T minimum;
        private T maximum;
        private T increment;

        public Builder<T> min(T min) {
            this.minimum = min;
            return this;
        }

        public Builder<T> max(T max) {
            this.maximum = max;
            return this;
        }

        public Builder<T> increment(T inc) {
            this.increment = inc;
            return this;
        }

        @Override
        public NumberSetting<T> build() {
            return new NumberSetting<>(name, (T) defaultValue, (T) value, parent, minimum, maximum, increment);
        }
    }
}