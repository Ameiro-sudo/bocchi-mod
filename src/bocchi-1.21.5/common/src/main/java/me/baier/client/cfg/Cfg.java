package me.baier.client.cfg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import me.baier.client.Bocchi;
import me.baier.client.Saveable;

@Getter
public class Cfg implements Saveable {
    private final String name;

    public Cfg(String name) {
        this.name = name;
    }

    @Override
    public JsonObject save(JsonObject json) {
        // 保存格式: { "name": ..., <modLabel>: { <settingLabel>: {...} } }
        // 与 load() 严格对称, 由 Mods 统一负责 mod 节点的读写
        json.addProperty("name", name);
        return Bocchi.INSTANCE.getModManager().save(json);
    }

    @Override
    public void load(JsonObject json) {
        Bocchi.INSTANCE.getModManager().load(json);
    }
}
