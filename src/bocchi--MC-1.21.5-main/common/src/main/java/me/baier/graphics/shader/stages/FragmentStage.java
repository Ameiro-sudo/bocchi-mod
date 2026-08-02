package me.baier.graphics.shader.stages;

import me.baier.graphics.shader.CompiledStage;
import me.baier.graphics.shader.ShaderType;

public class FragmentStage extends CompiledStage {
    public FragmentStage(int id, String identifier, String source) {
        super(ShaderType.FRAGMENT, id, identifier, source);
    }
}
