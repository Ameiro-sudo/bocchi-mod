package me.baier.graphics.shader.stages;

import me.baier.graphics.shader.CompiledStage;
import me.baier.graphics.shader.ShaderType;

class VertexStage extends CompiledStage {
    public VertexStage(int id, String identifier, String source) {
        super(ShaderType.VERTEX, id, identifier, source);
    }
}
