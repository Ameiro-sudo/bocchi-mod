package me.baier.graphics.shader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import me.baier.graphics.MinecraftRenderInstance;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

import static org.lwjgl.opengl.GL45C.*;

@Slf4j
public class ShaderFactory implements MinecraftRenderInstance {
    private static final Map<String, CompiledStage> compiledShaders = new Object2ObjectOpenHashMap<>();
    private static final Map<String, UniformBuffer> ubo = new Object2ObjectOpenHashMap<>();
    private static final Map<String, ShaderStorageBuffer> ssbo = new Object2ObjectOpenHashMap<>();

    public static CompiledStage compile(ResourceLocation source, String identifier, ShaderType type) {
        try {
            return compile(
                    IOUtils.toString(
                            mc.getResourceManager().open(source),
                            StandardCharsets.UTF_8
                    ),
                    identifier,
                    type
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to compile shader ", e);
        }
    }

    public static CompiledStage compile(String source, String identifier, ShaderType type) {
        return compile(MemoryUtil.memUTF8(source), identifier, type);
    }

    public static CompiledStage compile(ByteBuffer source, String identifier, ShaderType type) {
        var shader = glCreateShader(type.nativeType);
        if (shader == 0) {
            return null;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long string = stack.npointer(source);
            long length = stack.nint(source.remaining());
            nglShaderSource(shader, 1, string, length);
        } finally {
            Reference.reachabilityFence(source);
        }

        glCompileShader(shader);

        var src = MemoryUtil.memUTF8(source);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            handleCompileError(src, log);
            return null;
        }

        var stage = new CompiledStage(type, shader, identifier, src);
        compiledShaders.put(identifier, stage);
        return stage;
    }

    public static Shader make(CompiledStage... stages) {
        return make(false, null, stages);
    }

    public static Shader make(ShaderCallback callback, CompiledStage... stages) {
        return make(false, callback, stages);
    }

    public static Shader make(boolean useMatUniformBlock, CompiledStage... stages) {
        return make(useMatUniformBlock, null, stages);
    }

    public static Shader make(boolean useMatUniformBlock, ShaderCallback callback, CompiledStage... stages) {
        var program = glCreateProgram();
        for (CompiledStage stage : stages) {
            stage.attach(program);
        }

        {
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                try {
                    String log = glGetProgramInfoLog(program);

                    String[] headers = new String[stages.length];
                    for (int i = 0; i < headers.length; i++) {
                        headers[i] = stages[i].getIdentifier() + " / " + stages[i].getType().name;
                    }

                    String[] sources = new String[stages.length];
                    for (int i = 0; i < headers.length; i++) {
                        sources[i] = stages[i].getSource();
                    }

                    handleLinkError(headers, sources, log);
                    return null;
                } finally {
                    glDeleteProgram(program);
                }
            }
        }

        for (CompiledStage stage : stages) {
            stage.detach(program);
            if (stage.isFragmentShader()) {
                compiledShaders.remove(stage.getIdentifier()).close();
            }
        }

        var shader = new Shader(program);
        if (useMatUniformBlock) {
            shader.bindUniformBlock("MatrixBlock", 0);
        }

        if (callback != null) callback.call(shader);
        return shader;
    }

    public static void bindUBO(String name, int index) {
        var buf = findUBO(name);
        if (buf != null) buf.bindBase(index);
    }

    public static void bindSSBO(String name, int index) {
        var buf = findSSBO(name);
        if (buf != null) buf.bindBase(index);
    }

    public static UniformBuffer findOrCreateUBO(String name, int size) {
        return ubo.computeIfAbsent(name, __ -> makeUniformBuffer(size));
    }

    public static ShaderStorageBuffer findOrCreateSSBO(String name, int size) {
        return ssbo.computeIfAbsent(name, __ -> makeShaderStorageBuffer(size));
    }

    public static UniformBuffer findUBO(String name) {
        return ubo.get(name);
    }

    public static ShaderStorageBuffer findSSBO(String name) {
        return ssbo.get(name);
    }

    public static UniformBuffer makeUniformBuffer(int size) {
        var buffer = new UniformBuffer();
        buffer.allocate(size);
        return buffer;
    }

    public static ShaderStorageBuffer makeShaderStorageBuffer(int size) {
        var buffer = new ShaderStorageBuffer();
        buffer.allocate(size);
        return buffer;
    }

    private static void handleCompileError(String source, String errors) {
        var f = new Formatter();
        f.format("Shader compilation error%n");
        f.format("------------------------%n");
        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            f.format(Locale.ROOT, "%4d\t%s%n", i + 1, lines[i]);
        }
        f.format(errors);
        log.error(f.toString());
    }

    public static void handleLinkError(String[] headers, String[] sources, String errors) {
        var f = new Formatter();
        f.format("Program linking error%n");
        f.format("---------------------%n");
        for (int i = 0; i < headers.length; i++) {
            if (sources[i] == null) continue;
            f.format("%s%n", headers[i]);
            String[] lines = sources[i].split("\n");
            for (int j = 0; j < lines.length; ++j) {
                f.format(Locale.ROOT, "%4d\t%s%n", j + 1, lines[j]);
            }
        }
        f.format(errors);
        log.error(f.toString());
    }

    public static void close() {
        ubo.values().forEach(UniformBuffer::close);
        ssbo.values().forEach(ShaderStorageBuffer::close);
        ubo.clear();
        ssbo.clear();
    }
}
