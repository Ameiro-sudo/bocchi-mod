package me.baier.client.mod.setting;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.baier.client.Labelable;
import me.baier.client.Saveable;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public class SettingGroup implements Labelable, Saveable {
    private final String label;
    private final List<Setting<?>> settings = new ArrayList<>();

    public <T extends Setting<?>> T add(T setting) {
        setting.setParent(this);
        settings.add(setting);
        return setting;
    }

    @Override
    public JsonObject save(JsonObject json) {
        for (var setting : settings) {
            json.add(setting.getLabel(), setting.save(new JsonObject()));
        }
        return json;
    }

    @Override
    public void load(JsonObject json) {
        for (var setting : settings) {
            var settingJson = json.getAsJsonObject(setting.getLabel());
            if (settingJson != null) {
                setting.load(settingJson);
            }
        }
    }
}
