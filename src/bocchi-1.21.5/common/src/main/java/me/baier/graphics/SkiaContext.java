package me.baier.graphics;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.*;
import lombok.Getter;
import me.baier.graphics.util.ScissorStack;
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
public class SkiaContext implements MinecraftRenderInstance {
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

  public void init(float scale) {
    RenderSystem.assertOnRenderThread();
    this.scale = scale;
    closeRenderTarget();
    // 用窗口像素尺寸 (主 FBO = 窗口像素). 1.21.5 的 RenderTarget 延迟 resize,
    // framebuffer.width 可能未及时更新, 导致 surface 不随窗口放大.
    int fbW = mc.getWindow().getWidth();
    int fbH = mc.getWindow().getHeight();
    this.backendRenderTarget =
        BackendRenderTarget.makeGL(
            fbW,
            fbH,
            0, // samples
            8, // stencil
            getMinecraftFBO(),
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
    lastWidth = fbW;
    lastHeight = fbH;
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

  /** 窗口尺寸有效 (非最小化); 首帧 surface 尚未初始化时也可为 true, 用于渲染入口的跳帧判断. */
  public boolean canRender() {
    return mc.getWindow().getWidth() > 0 && mc.getWindow().getHeight() > 0;
  }

  /** 窗口尺寸有效且 Skia surface 可用; 窗口最小化 (0 像素) 时为 false, 本帧应跳过渲染. */
  public boolean isReady() {
    return canRender() && canvas != null && surface != null;
  }

  public void begin() {
    //        RenderSystem.assertOnRenderThread();

    // 窗口最小化时宽高为 0: 无法创建合法的 BackendRenderTarget/Surface, 且后续缩放会除零, 直接跳过本帧
    if (mc.getWindow().getWidth() <= 0 || mc.getWindow().getHeight() <= 0) {
      return;
    }

    if (lastWidth != mc.getWindow().getWidth() || lastHeight != mc.getWindow().getHeight()) {
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

    // begin() 因 0 尺寸跳过时, 这里同样跳过, 避免除零与对失效 surface 的操作
    if (!isReady()) {
      return;
    }

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

    // 与 1.21.1 对齐: 解绑 Skia 可能残留的 VAO/program/buffer/纹理, 避免污染原版渲染状态
    GL30.glBindVertexArray(0);
    GlStateManager._glBindVertexArray(0);
    GL20.glUseProgram(0);
    GlStateManager._glUseProgram(0);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    GlStateManager._bindTexture(0);

    //        if (OffScreenRenderer.ENABLED) {
    //            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    //        }
  }

  public void close() {
    RenderSystem.assertOnRenderThread();
    // canvas 由 surface 持有, 关闭 surface 即随之释放; 再显式 close canvas 会 double-free
    closeRenderTarget();
    context.close();
  }

  public Surface surface() {
    return surface;
  }

  public Canvas canvas() {

    return canvas;
  }
}
