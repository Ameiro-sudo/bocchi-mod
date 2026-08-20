package me.baier.client.mod;

import com.google.gson.JsonObject;
import me.baier.client.Saveable;
import me.baier.client.mod.impl.Aura;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.SettingGroup;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Mods implements Saveable {
    INSTANCE;

    private final Map<String, Mod> map;

    Mods() {
        map = new HashMap<>();
    }

    public Mods initialize() {
        register(Aura.class);
        return INSTANCE;
    }

    public void register(Class<? extends Mod> klass) {
        try {
            final var mod = klass.getConstructor().newInstance();
            for (final var field : klass.getDeclaredFields()) {
                field.setAccessible(true);
                final var o = field.get(mod);
                if (o instanceof Setting<?> setting) {
                    if (setting.getParent() == null) mod.defaultGroup.add(setting);
                } else if (o instanceof SettingGroup group) {
                    mod.groups.add(group);
                }

            }
            mod.groups.add(mod.defaultGroup);
            map.put(mod.getLabel(), mod);
        } catch (Exception e) {
            // 单个 mod 注册失败不应让游戏启动崩溃: 记录并跳过
            log.error("Failed to register mod {}", klass.getName(), e);
        }
    }

    public final Collection<Mod> getMods() {
        return map.values();
    }

    @Override
    public JsonObject save(JsonObject json) {
        // 直接将每个Mod作为根JSON对象的子项
        for (var entry : map.entrySet()) {
            json.add(entry.getKey(), entry.getValue().save(new JsonObject()));
        }
        return json;
    }

    @Override
    public void load(JsonObject json) {
        for (var entry : map.entrySet()) {
            var modJson = json.getAsJsonObject(entry.getKey());
            if (modJson != null) {
                entry.getValue().load(modJson);
            }
        }
    }
}