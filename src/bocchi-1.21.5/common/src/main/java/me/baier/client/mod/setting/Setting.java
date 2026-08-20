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
            return (B) this;
        }

        public B defaultValue(V defaultValue) {
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            return (B) this;
        }


        public abstract S build();
    }
}
