package me.baier.graphics.shader;

import lombok.Getter;

import static org.lwjgl.opengl.GL45C.*;

@Getter
public class ShaderStorageBlock {
    private final Shader shader;
    private final int index;

    public ShaderStorageBlock(Shader shader,int index) {
        this.shader = shader;
        this.index = index;
    }

    public void bind(int ssbo_index) {
        glShaderStorageBlockBinding(shader.getProgram(), this.index, ssbo_index);
    }

    public void upload() {
    }
}
