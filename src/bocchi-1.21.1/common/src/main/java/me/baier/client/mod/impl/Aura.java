package me.baier.client.mod.impl;

import me.baier.client.mod.Category;
import me.baier.client.mod.Mod;
import me.baier.client.mod.setting.SettingGroup;
import me.baier.client.mod.setting.impl.BooleanSetting;
import me.baier.client.mod.setting.impl.EnumSetting;
import me.baier.client.mod.setting.impl.RangedSetting;

/**
 * 模板占位 Mod (v1.0 审计轮结论: 无实际玩法逻辑, 仅作注册表/持久化的活体样例).
 * v1.1 设置面板接入时把原 Test/Test2 占位换成了自描述条目, 顺带覆盖全部四类设置控件.
 */
public class Aura extends Mod {
    private final SettingGroup render = new SettingGroup("Render");
    private final BooleanSetting enabled =
            render.add(new BooleanSetting.Builder().label("Enabled").value(true).build());
    private final EnumSetting<Mode> mode =
            render.add(new EnumSetting.Builder<Mode>().label("Mode").value(Mode.SWITCH).build());
    private final RangedSetting<Integer> radius =
            render.add(new RangedSetting.Builder<Integer>()
                    .label("Radius")
                    .value(4)
                    .min(1)
                    .max(8)
                    .increment(1)
                    .leftValue(0)
                    .rightValue(16)
                    .build());

    enum Mode {
        SWITCH,
        HOLD,
        TOGGLE
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