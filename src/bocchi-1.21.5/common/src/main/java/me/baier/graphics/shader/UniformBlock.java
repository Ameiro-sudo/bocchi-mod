package me.baier.graphics.shader;

import lombok.Getter;

import static org.lwjgl.opengl.GL45C.*;

@Getter
public class UniformBlock {
    private final Shader shader;
    private final int index;

    public UniformBlock(Shader shader, String name) {
        this.shader = shader;
        this.index = glGetUniformBlockIndex(shader.getProgram(), name);
    }

    public void bind(int ubo_index) {
        glUniformBlockBinding(shader.getProgram(), this.index, ubo_index);
    }

    public void upload() {
    }
}
