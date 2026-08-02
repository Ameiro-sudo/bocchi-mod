package me.baier.graphics.pipeline.post;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import me.baier.graphics.pipeline.Mesh;
import me.baier.graphics.pipeline.PassTest;
import me.baier.graphics.pipeline.RenderPass;
import me.baier.graphics.shader.Shader;

import static org.lwjgl.opengl.GL45C.*;

public class TestPostEffect extends PostEffect {
    private TextureTarget history;

    public TestPostEffect() {
        super(PassTest.post_process);
    }

    @Override
    protected void setup(RenderPass pass, Shader shader) {

        if (history == null) {
            history = new TextureTarget(
                    "history",
                    mc.getWindow().getWidth(),
                    mc.getWindow().getHeight(),
                    true
            );
            ((GlTexture) history.getColorTexture()).flushModeChanges();
            copyFrom(mc.getMainRenderTarget());
        }

        GlStateManager._activeTexture(GL_TEXTURE1);
        GlStateManager._bindTexture(((GlTexture) history.getColorTexture()).glId());

        shader.find("HistorySampler").push1i(1);
        shader.find("blurriness").push1f(0.3f);
        shader.find("renderRGB").push1i(1);
    }

    @Override
    protected void end() {
        copyFrom(mc.getMainRenderTarget());
    }

    @Override
    public void onResized(int width, int height) {
        super.onResized(width, height);

        if (history != null) {
            history.resize(width, height);
            ((GlTexture) history.getColorTexture()).flushModeChanges();
        }
    }

    public void copyFrom(RenderTarget otherTarget) {
        RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(
                        otherTarget.getColorTexture(), this.history.getColorTexture(),
                        0, 0, 0, 0, 0,
                        mc.getWindow().getWidth(), mc.getWindow().getHeight()
                );
    }
}
