package me.baier.client.mod.setting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.baier.client.Labelable;
import me.baier.client.Saveable;
import me.baier.client.mod.Category;

@Getter
@Setter
@AllArgsConstructor
public abstract class Setting<T> implements Labelable, Saveable {
    private final String label;
    public final T defaultValue;
    public T value;
    protected SettingGroup parent;

    public abstract static class SettingBuilder<B, V, S> {
        protected String name = "UnDefined";
        protected V value;
        protected V defaultValue = null;
        protected SettingGroup parent = null;

        protected SettingBuilder(V value) {
            this.value = value;
            this.defaultValue = value;
        }

        protected SettingBuilder() {
        }

        public B label(String name) {
            this.name = name;
            return (B) this;
        }

        public B value(V value) {
            this.value = value;
            // 未显式给过 defaultValue 时跟随当前值, 修复裸 .value() 后 default 恒为 null 的旧语义
            // (显式 .defaultValue(...) 在链上后置时仍可覆盖)
            if (this.defaultValue == null) {
                this.defaultValue = value;
            }
            return (B) this;
        }

        public B defaultValue(V defaultValue) {
            // 只声明默认值, 不再强制覆盖当前值: "默认开、当前关" 的合法组合需要能表达
            this.defaultValue = defaultValue;
            return (B) this;
        }


        public abstract S build();
    }
}
