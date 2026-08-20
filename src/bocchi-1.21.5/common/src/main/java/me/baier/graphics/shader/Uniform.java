package me.baier.graphics.shader;

import com.mojang.blaze3d.textures.GpuTexture;
import lombok.Getter;
import me.baier.graphics.MinecraftRenderInstance;
import org.joml.*;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL45C.*;

@Getter
public class Uniform implements MinecraftRenderInstance {
    private final Shader shader;
    private final String name;
    private final int location;

    // 延迟上传任务
    private Runnable upload;

    // 矩阵缓冲区复用
    private final FloatBuffer MATRIX33 = MemoryUtil.memAllocFloat(3 * 3);
    private final FloatBuffer MATRIX44 = MemoryUtil.memAllocFloat(4 * 4);

    public Uniform(Shader shader, String name) {
        this.shader = shader;
        this.name = name;
        this.location = glGetUniformLocation(shader.getProgram(), name);
    }

    public void upload() {
        if (upload != null) {
            upload.run();
            upload = null;
        }
    }

    // 以下是各种类型参数的push方法实现

    public void push(Color c) {
        push4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
    }

    public void push(Vector2ic c) { push2i(c.x(), c.y()); }
    public void push(Vector2fc c) { push2f(c.x(), c.y()); }
    public void push(Vector2dc c) { push2d(c.x(), c.y()); }
    public void push(Vector3ic c) { push3i(c.x(), c.y(), c.z()); }
    public void push(Vector3fc c) { push3f(c.x(), c.y(), c.z()); }
    public void push(Vector3dc c) { push3d(c.x(), c.y(), c.z()); }
    public void push(Vector4ic c) { push4i(c.x(), c.y(), c.z(), c.w()); }
    public void push(Vector4fc c) { push4f(c.x(), c.y(), c.z(), c.w()); }
    public void push(Vector4dc c) { push4d(c.x(), c.y(), c.z(), c.w()); }

    // 标量类型
    public void push1i(int value) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform1i(shader.getProgram(), location, value);
            } else {
                shader.bind();
                glUniform1i(location, value);
            }
        };
    }

    public void push1iv(int[] value) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform1iv(shader.getProgram(), location, value);
            } else {
                shader.bind();
                glUniform1iv(location, value);
            }
        };
    }

    public void push1f(float value) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform1f(shader.getProgram(), location, value);
            } else {
                shader.bind();
                glUniform1f(location, value);
            }
        };
    }

    public void push1fv(float[] value) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform1fv(shader.getProgram(), location, value);
            } else {
                shader.bind();
                glUniform1fv(location, value);
            }
        };
    }

    public void push1d(double value) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform1d(shader.getProgram(), location, value);
            } else {
                shader.bind();
                glUniform1d(location, value);
            }
        };
    }

    public void push1dv(double[] value) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform1dv(shader.getProgram(), location, value);
            } else {
                shader.bind();
                glUniform1dv(location, value);
            }
        };
    }

    // 浮点向量
    public void push2f(float x, float y) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform2f(shader.getProgram(), location, x, y);
            } else {
                shader.bind();
                glUniform2f(location, x, y);
            }
        };
    }

    public void push2fv(float[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform2fv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform2fv(location, values);
            }
        };
    }

    public void push3f(float x, float y, float z) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform3f(shader.getProgram(), location, x, y, z);
            } else {
                shader.bind();
                glUniform3f(location, x, y, z);
            }
        };
    }

    public void push3fv(float[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform3fv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform3fv(location, values);
            }
        };
    }

    public void push4f(float x, float y, float z, float w) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform4f(shader.getProgram(), location, x, y, z, w);
            } else {
                shader.bind();
                glUniform4f(location, x, y, z, w);
            }
        };
    }

    public void push4fv(float[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform4fv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform4fv(location, values);
            }
        };
    }

    // 整数向量
    public void push2i(int x, int y) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform2i(shader.getProgram(), location, x, y);
            } else {
                shader.bind();
                glUniform2i(location, x, y);
            }
        };
    }

    public void push2iv(int[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform2iv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform2iv(location, values);
            }
        };
    }

    public void push3i(int x, int y, int z) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform3i(shader.getProgram(), location, x, y, z);
            } else {
                shader.bind();
                glUniform3i(location, x, y, z);
            }
        };
    }

    public void push3iv(int[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform3iv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform3iv(location, values);
            }
        };
    }

    public void push4i(int x, int y, int z, int w) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform4i(shader.getProgram(), location, x, y, z, w);
            } else {
                shader.bind();
                glUniform4i(location, x, y, z, w);
            }
        };
    }

    public void push4iv(int[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform4iv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform4iv(location, values);
            }
        };
    }

    // 双精度向量
    public void push2d(double x, double y) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform2d(shader.getProgram(), location, x, y);
            } else {
                shader.bind();
                glUniform2d(location, x, y);
            }
        };
    }

    public void push2dv(double[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform2dv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform2dv(location, values);
            }
        };
    }

    public void push3d(double x, double y, double z) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform3d(shader.getProgram(), location, x, y, z);
            } else {
                shader.bind();
                glUniform3d(location, x, y, z);
            }
        };
    }

    public void push3dv(double[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform3dv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform3dv(location, values);
            }
        };
    }

    public void push4d(double x, double y, double z, double w) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform4d(shader.getProgram(), location, x, y, z, w);
            } else {
                shader.bind();
                glUniform4d(location, x, y, z, w);
            }
        };
    }

    public void push4dv(double[] values) {
        upload = () -> {
            if (dsa()) {
                glProgramUniform4dv(shader.getProgram(), location, values);
            } else {
                shader.bind();
                glUniform4dv(location, values);
            }
        };
    }

    public void push(Matrix3fc m) {
        m.get(MATRIX33);
        pushMatrix3f(MATRIX33, false);
    }

    public void push(Matrix4fc m) {
        m.get(MATRIX44);
       pushMatrix4f(MATRIX44, false);
    }

    public void pushTranspose(Matrix3fc m) {
        m.getTransposed(MATRIX33);
       pushMatrix3f(MATRIX33, true);
    }

    public void pushTranspose(Matrix4fc m) {
        m.getTransposed(MATRIX44);
        pushMatrix4f(MATRIX44, true);
    }

    // 矩阵数组上传
    public void pushMatrix2f(float[] matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix2fv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix2fv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix3f(float[] matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix3fv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix3fv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix4f(float[] matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix4fv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix4fv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix2d(double[] matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix2dv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix2dv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix3d(double[] matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix3dv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix3dv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix4d(double[] matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix4dv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix4dv(location, transpose, matrix);
            }
        };
    }

    // 矩阵缓冲区上传
    public void pushMatrix2f(FloatBuffer matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix2fv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix2fv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix3f(FloatBuffer matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix3fv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix3fv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix4f(FloatBuffer matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix4fv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix4fv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix2d(DoubleBuffer matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix2dv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix2dv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix3d(DoubleBuffer matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix3dv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix3dv(location, transpose, matrix);
            }
        };
    }

    public void pushMatrix4d(DoubleBuffer matrix, boolean transpose) {
        upload = () -> {
            if (dsa()) {
                glProgramUniformMatrix4dv(shader.getProgram(), location, transpose, matrix);
            } else {
                shader.bind();
                glUniformMatrix4dv(location, transpose, matrix);
            }
        };
    }
}