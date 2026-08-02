package me.baier.client.mod;

import com.google.gson.JsonObject;
import lombok.Getter;
import me.baier.client.Labelable;
import me.baier.client.Saveable;
import me.baier.client.mod.setting.SettingGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class Mod implements Labelable, Saveable {
    private final String label;
    @Getter
    private final Category category;
    protected final SettingGroup defaultGroup = new SettingGroup("default");
    public final List<SettingGroup> groups = new ArrayList<>(1);

    protected Mod() {
        this.label = label();
        this.category = category();
    }

    protected abstract String label();

    protected abstract Category category();

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public JsonObject save(JsonObject json) {
        for (var group : groups) {
            for (var setting : group.getSettings()) {
                json.add(setting.getLabel(), setting.save(new JsonObject()));
            }
        }
        return json;
    }

    @Override
    public void load(JsonObject json) {
        for (var group : groups) {
            for (var setting : group.getSettings()) {
                var settingJson = json.getAsJsonObject(setting.getLabel());
                if (settingJson != null) {
                    setting.load(settingJson);
                }
            }
        }
    }
}
