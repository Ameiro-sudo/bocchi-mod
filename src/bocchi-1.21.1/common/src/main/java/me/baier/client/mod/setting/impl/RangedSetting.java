package me.baier.client.mod.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.SettingGroup;

@Getter
public class RangedSetting<T extends Number> extends Setting<T> {
    private final T minimum;
    private final T maximum;
    private final T increment;
    private final T leftValue;
    private final T rightValue;

    public RangedSetting(String label, T defaultValue, T value, SettingGroup parent, 
                       T minimum, T maximum, T increment, T leftValue, T rightValue) {
        super(label, defaultValue, value, parent);
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
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
                setValue((T) number);
            } catch (Exception e) {
                // 保持默认值
            }
        }
    }

    public static class Builder<T extends Number> extends Setting.SettingBuilder<Builder<T>, T, RangedSetting<T>> {
        private T minimum;
        private T maximum;
        private T increment;
        private T leftValue;
        private T rightValue;

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

        public Builder<T> leftValue(T left) {
            this.leftValue = left;
            return this;
        }

        public Builder<T> rightValue(T right) {
            this.rightValue = right;
            return this;
        }

        @Override
        public RangedSetting<T> build() {
            return new RangedSetting<>(name, (T) defaultValue, (T) value, parent, 
                                     minimum, maximum, increment, leftValue, rightValue);
        }
    }
}