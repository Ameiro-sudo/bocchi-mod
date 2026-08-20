package me.baier.graphics.pipeline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.baier.design.Design;
import me.baier.graphics.pipeline.post.PostEffectRenderer;
import me.baier.graphics.pipeline.post.TestPostEffect;
import me.baier.graphics.shader.Shader;
import me.baier.graphics.shader.ShaderFactory;
import me.baier.graphics.shader.ShaderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import static org.lwjgl.system.MemoryUtil.*;

public class PassTest {
    public static Shader shader;
    public static Shader post_process;

    public static TestPostEffect post;

    private static boolean init = false;
    public static Mesh test;

    public static void init() {
        if (!init) {
            var vert = ShaderFactory.compile(
                    Design.resource("shaders.pos_color_vert"),
                    "v_test",
                    ShaderType.VERTEX
            );
            var frag = ShaderFactory.compile(
                    Design.resource("shaders.test_frag"),
                    "f_test",
                    ShaderType.FRAGMENT
            );

            shader = ShaderFactory.make(vert, frag);


            test = new Mesh(
                    shader, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS
            );

            var vertPost = ShaderFactory.compile(
                    Design.resource("shaders.post_base_vert"),
                    "v_post_test",
                    ShaderType.VERTEX
            );
            var fragPost = ShaderFactory.compile(
                    Design.resource("shaders.post_test_frag"),
                    "f_post_test",
                    ShaderType.FRAGMENT
            );

            post_process = ShaderFactory.make(vertPost, fragPost);

            post = new TestPostEffect();

            init = true;
        }
    }

    public static void test(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();

        PostEffectRenderer.set(post);
    }
}
