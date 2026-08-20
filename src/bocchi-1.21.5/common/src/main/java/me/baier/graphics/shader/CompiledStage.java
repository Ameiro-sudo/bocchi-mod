package me.baier.graphics.shader;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Closeable;

import static org.lwjgl.opengl.GL45C.*;

@Getter
@AllArgsConstructor
public class CompiledStage {
    private final ShaderType type;
    private int id;
    private final String identifier;

    private final String source;

    public boolean isVertexShader() {
        return type == ShaderType.VERTEX;
    }

    public boolean isFragmentShader() {
        return type == ShaderType.FRAGMENT;
    }

    public void attach(int program) {
        glAttachShader(program, this.id);
    }

    public void detach(int program) {
        glDetachShader(program, this.id);
    }

    public void close() {
        if (this.id != 0) {
            glDeleteShader(this.id);
            this.id = 0;
        }
    }
}
