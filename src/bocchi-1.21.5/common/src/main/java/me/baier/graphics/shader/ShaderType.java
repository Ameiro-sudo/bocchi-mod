package me.baier.graphics.shader;

import lombok.AllArgsConstructor;

import static org.lwjgl.opengl.GL20.*;

@AllArgsConstructor
public enum ShaderType {
    VERTEX("Vertex", GL_VERTEX_SHADER),
    FRAGMENT("Fragment", GL_FRAGMENT_SHADER);

    public final String name;
    public final int nativeType;
}
