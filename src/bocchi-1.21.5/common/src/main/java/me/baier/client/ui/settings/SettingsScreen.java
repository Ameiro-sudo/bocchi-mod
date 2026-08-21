package me.baier.client.ui.settings;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.types.Rect;
import me.baier.client.ui.theme.ThemeManager;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaEnvironment;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.skui.SkComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL33C;

/**
 * Cfgs 设置面板. 从主菜单按钮进入, 关闭时回到来源菜单.
 * 背景复用当前主题的主菜单场景渲染 (ThemeManager.renderBackground),
 * 其上叠加暗化遮罩与面板卡片; 控件全部走 skui 组件树.
 */
public class SettingsScreen extends Screen {
  private static final Component TITLE = Component.literal("Cfgs");

  private final Screen parent;
  private final SkComponent root = new SkComponent();
  private final SettingsPanel panel;

  public SettingsScreen(Screen parent) {
    super(TITLE);
    this.parent = parent;
    this.panel = new SettingsPanel(this::returnToParent);
    root.addChild(panel);
    root.initialize();
    root.attach();
  }

  private void returnToParent() {
    Minecraft.getInstance().setScreen(parent);
  }

  /** guiScaled 鼠标坐标 -> 480x270 布局坐标 (与两块主菜单屏同一映射). */
  private static int mapX(double mouseX) {
    return (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
  }

  private static int mapY(double mouseY) {
    return (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));
  }

  @Override
  public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    int lx = mapX(mouseX);
    int ly = mapY(mouseY);
    root.handleMouseMove(lx, ly);

    var ctx = SkiaContext.get();
    if (!ctx.canRender()) {
      return; // 窗口最小化等场景跳过本帧 (与主菜单同守卫)
    }
    ThemeManager.get().renderBackground();

    SkiaEnvironment.run(
        ctx,
        env -> {
          var canvas = env.getCanvas();
          Rect screenBounds = Rect.makeWH(480, 270);
          canvas.saveLayerAlpha(screenBounds, 255);
          // 暗化遮罩: 与 GuiComponent 展开态遮罩同色阶
          SkiaRenderEngine.drawRect(0, 0, 480, 270, 0x7D000000);
          root.render(env, lx, ly);
          canvas.restore();
        });

    // 恢复原版标准 GUI 混合状态 (skia 直绘后的状态归位, 与主菜单一致)
    GlStateManager._blendFuncSeparate(
        GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      returnToParent();
      return true;
    }
    return root.handleKeyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onClose() {
    returnToParent();
  }

  @Override
  public void resize(Minecraft minecraft, int width, int height) {
    super.resize(minecraft, width, height);
    root.handleResizeWindow();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    return root.handleMouseClick(mapX(mouseX), mapY(mouseY), button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    return root.handleMouseRelease(mapX(mouseX), mapY(mouseY), button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double xOffset, double yOffset) {
    return root.handleMouseScroll(mapX(mouseX), mapY(mouseY), (int) yOffset);
  }

  @Override
  public boolean charTyped(char codePoint, int modifiers) {
    return root.handleCharTyped(codePoint, modifiers);
  }
}