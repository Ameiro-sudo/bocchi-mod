package me.baier.graphics.shader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import me.baier.graphics.MinecraftRenderInstance;

import java.util.Map;

import static org.lwjgl.opengl.GL45C.*;

public class Shader implements MinecraftRenderInstance {
    private static int currentProgram;
    private int last;

    @Getter
    private int program;

    private final Map<Integer, ShaderStorageBlock> shaderStorageBlocks = new Object2ObjectOpenHashMap<>();
    private final Map<String, UniformBlock> uniformBlocks = new Object2ObjectOpenHashMap<>();
    private final Map<String, Uniform> uniforms = new Object2ObjectOpenHashMap<>();

    public Shader(int program) {
        this.program = program;

        glBindFragDataLocation(program, 0, "fragColor");
    }

    public void bind() {
        currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        last = currentProgram;

        if (currentProgram != this.program) {
            glUseProgram(program);
            currentProgram = this.program;
        }
    }

    public Uniform find(String name) {
        return uniforms.computeIfAbsent(name, __ -> new Uniform(this, name));
    }

    public void upload() {
        for (Uniform value : uniforms.values()) {
            value.upload();
        }

        for (UniformBlock value : uniformBlocks.values()) {
            value.upload();
        }
    }

    public void bindUniformBlock(String name, int ubo_index) {
        var block = uniformBlocks.computeIfAbsent(name, __ -> new UniformBlock(this, name));
        block.bind(ubo_index);
    }

    public void bindShaderStorageBlock(int index, int ssbo_index) {
        var block = shaderStorageBlocks.computeIfAbsent(index, __ -> new ShaderStorageBlock(this, index));
        block.bind(ssbo_index);
    }

    public void unbind() {
        glUseProgram(last);
        currentProgram = last;
    }

    public void close() {
        glDeleteProgram(this.program);
        this.program = 0;
    }

    public boolean useUniformBlock() {
        return !uniformBlocks.isEmpty();
    }
}
