package me.baier.graphics.pipeline;

import aka.bocchi.injection.mixins.interfaces.IAccessorGlBuffer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Setter;
import me.baier.graphics.MinecraftRenderInstance;
import me.baier.graphics.shader.Shader;
import me.baier.graphics.shader.ShaderFactory;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.system.MemoryUtil.*;

public class RenderPass implements MinecraftRenderInstance {
    @Setter
    protected GpuBuffer vertexBuffer;
    protected GpuBuffer indexBuffer;

    /**
     * @see com.mojang.blaze3d.vertex.DefaultVertexFormat
     */
    protected final VertexFormat vertexFormat;
    protected final VertexFormat.Mode mode;

    public RenderPass(VertexFormat vertexFormat, VertexFormat.Mode mode) {
        this.vertexFormat = vertexFormat;
        this.mode = mode;
    }

    protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;

    @Setter
    protected Shader shader;
    @Setter
    protected Matrix4f model_mat;
    @Setter
    protected RenderTarget framebuffer;

    @Setter
    protected Matrix4f proj_mat;

    protected boolean disableMatrix;

    public void disableMatrix() {
        disableMatrix = true;
    }

    private FloatBuffer modelView;


    private static FloatBuffer proj, screenSize;
    private static long screenSizeAddr;

    // 这个在多个shader一起渲染的时候可以用
    // 单个就别用了
    public static void uploadMatrix(Matrix4f proj_mat) {
        var ubo = ShaderFactory.findOrCreateUBO("matrix",
                4 * 4 * Float.BYTES * 2 + // matrix
                        2 * Float.BYTES // screen size
        );
        var window = mc.getWindow();

        if (proj == null) proj = memAllocFloat(4 * 4 * Float.BYTES);
        if (screenSize == null) {
            screenSize = memAllocFloat(2 * Float.BYTES);
            screenSizeAddr = memAddress0(screenSize);
        }

        proj_mat.get(proj);

        memPutFloat(screenSizeAddr, mc.getMainRenderTarget().width);
        memPutFloat(screenSizeAddr + 4, mc.getMainRenderTarget().height);

        ubo.upload(0, 64, memAddress0(proj));
        ubo.upload(128, 8, screenSizeAddr);
    }

    public Shader shader() {
        return shader;
    }

    public void setIndexBuffer(GpuBuffer indexBuffer, VertexFormat.IndexType indexType) {
        this.indexBuffer = indexBuffer;
        this.indexType = indexType;
    }

    public void drawIndexed(int offset, int count) {
        if (this.shader == null) {
            return;
        }

        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, getFBO(this.framebuffer == null ? mc.getMainRenderTarget() : this.framebuffer));
        glViewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());

        shader.bind();

        if (!disableMatrix) {
            RenderSystem.getModelViewStack().pushMatrix();
            if (model_mat != null) RenderSystem.getModelViewStack().mul(model_mat);

            if (!shader.useUniformBlock()) {
                shader.find("ModelMat").push(RenderSystem.getModelViewStack());
                shader.find("ProjMat").push(this.proj_mat == null ? RenderSystem.getProjectionMatrix() : this.proj_mat);
                shader.find("ScreenSize").push2f(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
            } else {
                var ubo = ShaderFactory.findOrCreateUBO("matrix",
                        4 * 4 * Float.BYTES * 2 + // matrix
                                2 * Float.BYTES // screen size
                );

                ubo.bindBase(0);

                if (modelView == null) modelView = memAllocFloat(4 * 4 * Float.BYTES);
                RenderSystem.getModelViewStack().get(modelView);
                ubo.upload(64, 64, memAddress0(modelView));
            }

            RenderSystem.getModelViewStack().popMatrix();
        }

        shader.upload();

        getDevice().vertexArrayCache().bindVertexArray(vertexFormat, (GlBuffer) this.vertexBuffer);

        GlStateManager._glBindBuffer(GlConst.GL_ELEMENT_ARRAY_BUFFER, ((IAccessorGlBuffer) this.indexBuffer).getHandle());
        GlStateManager._drawElements(GlConst.toGl(mode), count, GlConst.toGl(indexType), (long) offset * indexType.bytes);

        shader.unbind();
    }

    public void drawArray(int offset, int count) {
        if (this.shader == null) {
            return;
        }

    }
}
