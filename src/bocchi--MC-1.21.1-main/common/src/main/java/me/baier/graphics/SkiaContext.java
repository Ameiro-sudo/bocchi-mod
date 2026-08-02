package me.baier.graphics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import lombok.Getter;
import me.baier.client.ClientInstance;
import me.baier.graphics.util.ScissorStack;
import me.baier.utils.ColorUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/**
 * @author AquaVase
 * @since 6/22/2024 - 5:35 PM
 */
public class SkiaContext implements ClientInstance {
  private static SkiaContext instance;
  @Getter private final DirectContext context;
  private Surface surface;
  private BackendRenderTarget backendRenderTarget;
  private Canvas canvas;
  @Getter private final long window;
  private final RenderTarget framebuffer;
  private float scale;

  private int lastWidth, lastHeight;

  private SkiaContext(long window, RenderTarget renderTarget) {
    RenderSystem.assertOnRenderThread();
    this.window = window;
    this.framebuffer = renderTarget;
    this.context = DirectContext.makeGL();
  }

  // Lazy-init
  public static SkiaContext get() {
    if (instance == null) {
      instance =
          new SkiaContext(
              mc.getWindow().getWindow(),
              //                    OffScreenRenderer.ENABLED ? OffScreenRenderer.framebuffer :
              mc.getMainRenderTarget());
    }
    return instance;
  }

  public void uninitStencilBuffer() {
    // Clear the stencil surface
    canvas.clear(ColorUtil.color(0, 0, 0, 0));
  }

  public void initStencilToWrite() {
    canvas.clear(ColorUtil.color(0, 0, 0, 0));
  }

  public void writeToStencil(Runnable drawOperation) {
    Canvas stencilCanvas = canvas;
    stencilCanvas.save();
    try (Paint paint = new Paint()) {
      paint.setColor(-1); // Use white for the stencil
      drawOperation.run();
    }
    stencilCanvas.restore();
  }

  public void readStencilBuffer(Runnable drawOperation) {
    Canvas mainCanvas = canvas;
    mainCanvas.save();
    try (Paint paint = new Paint()) {
      // Use the stencil surface as a mask
      paint.setBlendMode(BlendMode.DST_IN);
      drawOperation.run();
      mainCanvas.drawImage(surface.makeImageSnapshot(), 0, 0, paint);
    }
    mainCanvas.restore();
  }

  public void endStencilBuffer() {
    // In Skija, we don't need to explicitly end the stencil buffer
    // This method can be used for any necessary cleanup
  }

  public void push() {
    canvas.save();
  }

  public void pop() {
    canvas.restore();
  }

  public void scale(float x, float y) {
    canvas.scale(x, y);
  }

  public void translate(float x, float y) {
    canvas.translate(x, y);
  }

  public void rotate(float deg) {
    canvas.rotate(deg);
  }

  public void startScale(float x, float y, float scale) {
    push();
    canvas.translate(x, y);
    canvas.scale(scale, scale);
    canvas.translate(-x, -y);
  }

  public void stopScale() {
    pop();
  }

  public void startRotate(float x, float y, float deg) {
    push();
    canvas.translate(x, y);
    canvas.rotate(deg);
    canvas.translate(-x, -y);
  }

  public void stopRotate() {
    pop();
  }

  public void pushAlpha(float x, float y, float width, float height, int alpha) {
    try (Paint paint = new Paint()) {
      paint.setAlpha(alpha);
      canvas.saveLayer(Rect.makeXYWH(x, y, width, height), paint);
    }
  }

  public void pushAlpha(float x, float y, float width, float height, float alpha) {
    try (Paint paint = new Paint()) {
      paint.setAlphaf(alpha);
      canvas.saveLayer(Rect.makeXYWH(x, y, width, height), paint);
    }
  }

  public void startScissor(float x, float y, float width, float height) {
    Rect rect = Rect.makeXYWH(x, y, width, height);
    canvas.save();
    canvas.clipRect(rect, ClipMode.INTERSECT);
  }

  public void stopScissor() {
    canvas.restore();
  }

  public void renderShadow(
      float x, float y, float width, float height, float radius, int shadowRadius) {}

  public void renderGlow(
      float x, float y, float width, float height, float radius, int shadowRadius, int color) {}

  public void renderGlow(
      float x, float y, float width, float height, float radius, float offset, int color) {}

  private int applyOpacity(int color, float opacity) {
    int alpha = (int) (opacity * 255);
    return (color & 0x00FFFFFF) | (alpha << 24);
  }

  public void pushAlpha(float alpha) {
    try (Paint paint = new Paint()) {
      paint.setAlphaf(alpha);
      canvas.saveLayer(null, paint);
    }
  }

  public void popAlpha() {
    pop();
  }

  public void init(float scale) {
    RenderSystem.assertOnRenderThread();
    this.scale = scale;
    closeRenderTarget();
    this.backendRenderTarget =
        BackendRenderTarget.makeGL(
            framebuffer.width,
            framebuffer.height,
            0, // samples
            8, // stencil
            framebuffer.frameBufferId,
            FramebufferFormat.GR_GL_RGBA8);

    this.surface =
        Surface.wrapBackendRenderTarget(
            context,
            backendRenderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB(),
            new SurfaceProps(PixelGeometry.RGB_H));

    this.canvas = surface.getCanvas();
    // offScreenFrame = OffScreenFrame.create();
    lastWidth = framebuffer.width;
    lastHeight = framebuffer.height;
  }

  private void closeRenderTarget() {
    RenderSystem.assertOnRenderThread();
    if (surface != null) {
      surface.close();
      surface = null;
    }

    if (backendRenderTarget != null) {
      backendRenderTarget.close();
      backendRenderTarget = null;
    }
  }

  public void begin() {
    //        RenderSystem.assertOnRenderThread();

    if (lastWidth != framebuffer.width || lastHeight != framebuffer.height) {
      init((float) mc.getWindow().getGuiScale());
    }

    // Reset OpenGL state to Minecraft defaults
    GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
    GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
    GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
    GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);

    context.resetAll();

    // 布局固定 1920x1080, 按窗口像素拉伸 (画面占比不随窗口/界面尺寸变化)
    canvas.scale(mc.getWindow().getWidth() / 480f, mc.getWindow().getHeight() / 270f);
  }

  public void end() {
    //        RenderSystem.assertOnRenderThread();

    canvas.scale(480f / mc.getWindow().getWidth(), 270f / mc.getWindow().getHeight());
    surface.flush();

    // Restore OpenGL state to Minecraft's tracked state.
    // 注意: 必须恢复原版标准 GUI 混合 (SRC_ALPHA/ONE_MINUS_SRC_ALPHA) 且同步 GlStateManager 追踪.
    // 否则原版界面 (暂停/设置) 文字会用直调残留的加法混合渲染, 出现重影.
    GL33.glBindSampler(0, 0);

    GlStateManager._enableBlend();
    GlStateManager._blendFuncSeparate(
        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glEnable(GL11.GL_BLEND);
    GL14.glBlendFuncSeparate(
        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

    GlStateManager._colorMask(true, true, true, true);
    GL11.glColorMask(true, true, true, true);

    GlStateManager._depthMask(true);
    GL11.glDepthMask(true);

    RenderSystem.disableScissor();
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
    ScissorStack.apply();

    GL11.glDisable(GL11.GL_STENCIL_TEST);

    GlStateManager._disableDepthTest();
    GL11.glDisable(GL11.GL_DEPTH_TEST);

    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    GlStateManager._activeTexture(GL13.GL_TEXTURE0);

    GlStateManager._disableCull();

    GL30.glBindVertexArray(0);
    GlStateManager._glBindVertexArray(0);
    BufferUploader.invalidate();
    GL20.glUseProgram(0);
    GlStateManager._glUseProgram(0);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    GlStateManager._bindTexture(0);

    //        if (OffScreenRenderer.ENABLED) {
    //            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    //        }
  }

  public void close() {
    RenderSystem.assertOnRenderThread();
    closeRenderTarget();
    canvas.close();
    context.close();
  }

  public Surface surface() {
    return surface;
  }

  public Canvas canvas() {

    return canvas;
  }
}
