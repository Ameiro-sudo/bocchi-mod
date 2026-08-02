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
        // 直接将所有Mod扁平化保存到根JSON对象中
        Bocchi.INSTANCE.getModManager().getMods().forEach(mod -> 
            json.add(mod.getLabel(), mod.save(new JsonObject()))
        );
        return json;
    }

    @Override
    public void load(JsonObject json) {
        final var maybe = json.get("mod").getAsJsonObject();
        Bocchi.INSTANCE.getModManager().getMods().forEach(mod -> mod.load(maybe));
    }
}
