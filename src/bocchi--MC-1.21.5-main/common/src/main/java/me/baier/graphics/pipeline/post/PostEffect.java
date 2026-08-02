package me.baier.graphics.pipeline.post;

import aka.bocchi.injection.mixins.interfaces.IAccessorMinecraft;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import me.baier.graphics.MinecraftRenderInstance;
import me.baier.graphics.pipeline.Mesh;
import me.baier.graphics.pipeline.RenderPass;
import me.baier.graphics.shader.Shader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

@Getter
public class PostEffect implements MinecraftRenderInstance {
    private final Shader shader;
    private final RenderPass pass;
    private final TextureTarget framebuffer;

    private RenderTarget mcFramebuffer;

    public PostEffect(Shader shader) {
        this.shader = shader;

        this.framebuffer = new TextureTarget(
                "post_effect",
                mc.getMainRenderTarget().width,
                mc.getMainRenderTarget().height,
                true
        );

        ((GlTexture) framebuffer.getColorTexture()).flushModeChanges();

        this.pass = new RenderPass(DefaultVertexFormat.BLIT_SCREEN, VertexFormat.Mode.QUADS);
        this.pass.setShader(shader);
        this.pass.disableMatrix();

        var indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        this.pass.setIndexBuffer(indexBuffer.getBuffer(6), indexBuffer.type());
        this.pass.setVertexBuffer(RenderSystem.getQuadVertexBuffer());
    }

    public void pre() {
        mcFramebuffer = mc.getMainRenderTarget();
        ((IAccessorMinecraft) mc).setMainRenderTarget(this.framebuffer);
    }

    public void post() {
        ((IAccessorMinecraft) mc).setMainRenderTarget(this.mcFramebuffer);
        mcFramebuffer = null;
    }

    protected void setup(RenderPass pass, Shader shader) {
    }

    protected void end() {
    }

    public void render() {
        GlStateManager._activeTexture(GL33C.GL_TEXTURE0);
        GlStateManager._bindTexture(((GlTexture) framebuffer.getColorTexture()).glId());

        var shader = this.pass.shader();

        var mat = new Matrix4f().setOrtho(0.0F, framebuffer.width, 0.0F, framebuffer.height, 0.1F, 1000.0F);
        shader.find("ProjMat").push(mat);
        shader.find("InSampler").push1i(0);
        shader.find("Size").push2f(framebuffer.width, framebuffer.height);
        shader.find("Time").push1f((float) glfwGetTime());
        setup(this.pass, shader);

        GL33C.glDisable(GL33C.GL_STENCIL_TEST);
        RenderSystem.disableScissor();
        GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
        GlStateManager._disableDepthTest();
        GL33C.glDisable(GL33C.GL_DEPTH_TEST);
        GlStateManager._depthFunc(GL33C.GL_LEQUAL);
        GL33C.glDepthFunc(GL33C.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GL33C.glDepthMask(true);
        GlStateManager._disableCull();
        RenderSystem.lineWidth(1);

        GlStateManager._disableBlend();

        this.pass.setFramebuffer(mc.getMainRenderTarget());
        this.pass.drawIndexed(0, 6);

        end();
    }

    public void onResized(int width, int height) {
        if (framebuffer == null) return;
        framebuffer.resize(width, height);
        ((GlTexture) framebuffer.getColorTexture()).flushModeChanges();
    }
}
