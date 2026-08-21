package me.baier.client.ui.mainmenu.misayos;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.mainmenu.common.LogoComponent;
import me.baier.client.ui.mainmenu.component.interfaces.IComponent;
import me.baier.client.ui.model.IFrameContext;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaEnvironment;
import me.baier.skui.SkComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MainMenuMisayosScreen extends Screen {
  public static volatile MainMenuMisayosScreen INSTANCE;
  private static final Component TITLE = Component.literal("Main Menu(Misayos)");
  private static final Logger LOGGER = LoggerFactory.getLogger(MainMenuMisayosScreen.class);
  private final Map<Integer, IComponent<? super MainMenuMisayosFrameContext>> components =
      new HashMap<>();

  @NotNull private BezierAnimation<Float> alphaAnimation;
  private GuiComponent guiComponent;
  private SkComponent root;

  public MainMenuMisayosScreen() {
    super(TITLE);
    INSTANCE = this;
    root = new SkComponent();
    guiComponent = new GuiComponent();
    components.put(0, new BgRectsComponent());
    components.put(1, new StrokeElementsComponent());
    components.put(2, new TextElementsComponent());
    components.put(3, new MainTachieComponent());
    components.put(4, new LogoComponent());
    // guiComponent 只挂在 root 下渲染 (面板 UI + 交互层), 不再重复放进 components,
    // 避免每帧被画两遍; 其 update() 在 render() 中单独驱动
    root.addChild(guiComponent);
    onBeginFadeIn();
    root.initialize();
    this.root.attach();
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return true;
  }

  @Override
  public void resize(Minecraft minecraft, int width, int height) {
    super.resize(minecraft, width, height);
    this.root.handleResizeWindow();
  }

  public void onBeginFadeIn() {
    var frame = new MainMenuMisayosFrameContext();

    alphaAnimation = BezierAnimation.createFloat(0.f, 255.f, 1500, BezierControlPoints.EASE_IN_OUT);

    var lastMajorAnchorAnimation = alphaAnimation;
    for (Map.Entry<Integer, IComponent<? super MainMenuMisayosFrameContext>> entry :
        components.entrySet()) {
      lastMajorAnchorAnimation = entry.getValue().initAnimations(frame, lastMajorAnchorAnimation);
    }
    // guiComponent 不进 components(避免每帧重复渲染), 但入场动画链必须在此接上:
    // v1.0 曾漏掉这行, fadeInAnimation 恒为 null, 主菜单第一帧渲染即 NPE 崩溃
    lastMajorAnchorAnimation = guiComponent.initAnimations(frame, lastMajorAnchorAnimation);

    alphaAnimation.set(alphaAnimation.getTargetValue());
  }

  public void render(boolean renderAsBackground) {
    render(renderAsBackground, 0, 0);
  }

  public void render(int mouseX, int mouseY) {
    render(false, mouseX, mouseY);
  }

  public void render(boolean renderAsBackground, int mouseX, int mouseY) {
    var frame = new MainMenuMisayosFrameContext();
    this.root.setHeight(frame.getScaledHeight());
    this.root.setWidth(frame.getScaledWidth());
    alphaAnimation.update();
    for (Map.Entry<Integer, IComponent<? super MainMenuMisayosFrameContext>> entry :
        components.entrySet()) {
      entry.getValue().update(frame);
    }
    guiComponent.update(frame); // 面板 UI 由 root 渲染, 动画仍需逐帧更新

    var ctx = SkiaContext.get();
    if (!ctx.canRender()) {
      return; // 窗口最小化 (0 像素) 等场景, 跳过本帧渲染
    }
    var canvas = ctx.canvas();

    SkiaEnvironment.run(
        ctx,
        env -> {
          // 与 poulsen MainMenuScreen 同款异常隔离: 任一组件渲染抛错时只丢弃本帧,
          // 不让异常穿透 SkiaEnvironment.run 崩掉游戏, 也不在画布上残留未配对的图层/变换.
          // (v1.0 只加固了 poulsen 一侧, misayos 漏了同步)
          int saveLevel = canvas.getSaveCount();
          try {
            Rect screenBounds = Rect.makeWH(frame.getScaledWidth(), frame.getScaledHeight());
            int globalAlpha = Math.round(Math.clamp(alphaAnimation.getCurrentValue(), 0, 255));
            canvas.saveLayerAlpha(screenBounds, globalAlpha);
            var sigma =
                Mth.lerp(guiComponent.getExpandAnimation().getCurrentValue(), 0.01f, 1.f);
            try (ImageFilter blurFilter =
                ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)) {
              var blurPaint = env.borrowPaint().setImageFilter(blurFilter);
              canvas.saveLayer(screenBounds, blurPaint);
              canvas.translate(screenBounds.getWidth() / 2, screenBounds.getHeight() / 2);
              canvas.scale(
                  1 + guiComponent.getExpandAnimation().getCurrentValue() / 20,
                  1 + guiComponent.getExpandAnimation().getCurrentValue() / 20);
              canvas.translate(-screenBounds.getWidth() / 2, -screenBounds.getHeight() / 2);
              env.recyclePaint(blurPaint);

              for (int i = 0; i < components.size(); i++) {
                components.get(i).render(ctx, frame);
              }

              canvas.restore();
            }

            if (!renderAsBackground) {
              this.root.render(env, mouseX, mouseY);
            }
            canvas.restore();
          } catch (Throwable t) {
            LOGGER.error("bocchi: main menu render failed", t);
          } finally {
            // 正常路径下图层栈已配对回 saveLevel, 此调用为空操作; 异常路径负责兜底回收
            canvas.restoreToCount(saveLevel);
          }
        });
    GlStateManager._blendFuncSeparate(
        GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
  }

  @Override
  public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    // 布局固定 1920x1080: 把 guiScaled 鼠标坐标映射到布局坐标
    int lx = (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
    int ly = (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    this.root.handleMouseMove(lx, ly);

    this.render(lx, ly);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      // 主菜单是顶层菜单 (取代原版标题画面): 与原版 TitleScreen 一致, ESC 不退出
      return true;
    }
    return this.root.handleKeyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onClose() {
    // 不能在这里 setScreen(new TitleScreen()): MixinMinecraftClient.hookSetScreen
    // 会把 TitleScreen 重定向回本菜单, 造成 ESC 死循环.
    super.onClose();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int lx = (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
    int ly = (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    return this.root.handleMouseClick(lx, ly, button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    int lx = (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
    int ly = (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    return this.root.handleMouseRelease(lx, ly, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double xOffset, double yOffset) {
    int lx = (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
    int ly = (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    return this.root.handleMouseScroll(lx, ly, (int) yOffset);
  }

  @Override
  public boolean charTyped(char codePoint, int modifiers) {
    return this.root.handleCharTyped(codePoint, modifiers);
  }
}
