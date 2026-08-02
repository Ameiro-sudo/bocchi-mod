package me.baier.client.mod.impl;

import me.baier.client.mod.Category;
import me.baier.client.mod.Mod;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.SettingGroup;
import me.baier.client.mod.setting.impl.BooleanSetting;
import me.baier.client.mod.setting.impl.EnumSetting;

public class Aura extends Mod {
    private final BooleanSetting test = new BooleanSetting.Builder().label("Test").value(true).build();
    private final SettingGroup group = new SettingGroup("Test Page");
    private final BooleanSetting test2 = group.add(new BooleanSetting.Builder().label("Test2").value(false).build());
    private final EnumSetting<Mode> mode = new EnumSetting.Builder<Mode>().label("Mode").value(Mode.SWITCH).build();

    enum Mode {
        SWITCH
    }
    @Override
    protected String label() {
        return "Aura";
    }

    @Override
    protected Category category() {
        return Category.FIGHT;
    }
}
