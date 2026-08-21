package me.baier.client.ui.mainmenu.poulsen;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.mainmenu.common.LogoComponent;
import me.baier.client.ui.mainmenu.component.interfaces.IComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.client.ui.theme.ThemeManager;
import me.baier.graphics.SkiaContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainMenuScreen extends Screen {
  public static MainMenuScreen INSTANCE;
  private static final Component TITLE = Component.literal("Main Menu");
  private static final int BUTTON_Z_INDEX = 14;
  private static final Logger LOGGER = LoggerFactory.getLogger(MainMenuScreen.class);
  private final Map<Integer, IComponent<? super MainMenuPoulsenFrameContext>> components =
      new HashMap<>();
  private final List<ButtonControl> buttons = new ArrayList<>();

  private int mouseX, mouseY;

  @NotNull private BezierAnimation<Float> alphaAnimation;

  public MainMenuScreen() {
    super(TITLE);
    INSTANCE = this;
    components.put(12, new CirclesComponent());
    components.put(2, new BigFirstNameComponent());
    components.put(10, new MainTachieComponent());
    components.put(4, new BigLastNameComponent());
    components.put(8, new AdditionInfoComponent());
    components.put(5, new ThreeSquaresComponent());
    components.put(6, new ImagesBlockComponent());
    components.put(7, new JapaneseNamesComponent());
    components.put(3, new BottomLastNameComponent());
    components.put(9, new AliasTextComponent());
    components.put(1, new BgImageComponent());
    components.put(11, new DotsComponent());
    components.put(13, new LogoComponent());
    components.put(0, new BgRectsComponent());

    addButton("SinglePlayer", btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)), 0);
    addButton("MultiPlayer", btn -> Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this)), 1);
    addButton(
        "Options",
        btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)),
        2);
    addButton("Quit", btn -> Minecraft.getInstance().stop(), 3);
    addButton(
        "MISAYOS",
        btn -> {
          ThemeManager.set("misayos");
          Minecraft.getInstance().setScreen(ThemeManager.get().getMainMenuScreen());
        },
        4);

    onBeginFadeIn();
  }

  private void addButton(String label, ButtonControl.OnPress onPress, int index) {
    var button =
        new ButtonControl(
            label,
            onPress,
            (btn, frame) -> {
              float fontSize = frame.getBgFontSize() * 0.221f * 0.3f;
              return new Point(
                  frame.getScaledWidth() * 0.03f,
                  frame.getScaledHeight() * 0.1f + index * fontSize * 2.6f);
            });
    buttons.add(button);
    components.put(BUTTON_Z_INDEX + index, button);
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return true;
  }

  @Override
  public void onClose() {
    // 不能在这里 setScreen(new TitleScreen()): MixinMinecraftClient.hookSetScreen
    // 会把 TitleScreen 重定向回本菜单, 造成 ESC 死循环.
    super.onClose();
  }

  public void onBeginFadeIn() {
    MainMenuPoulsenFrameContext frame = new MainMenuPoulsenFrameContext();

    alphaAnimation = BezierAnimation.createFloat(0.f, 255.f, 1500, BezierControlPoints.EASE_IN_OUT);

    var lastMajorAnchorAnimation = alphaAnimation;
    for (Map.Entry<Integer, IComponent<? super MainMenuPoulsenFrameContext>> entry :
        components.entrySet()) {
      if (entry.getKey() < BUTTON_Z_INDEX) {
        lastMajorAnchorAnimation = entry.getValue().initAnimations(frame, lastMajorAnchorAnimation);
      }
    }

    alphaAnimation.set(alphaAnimation.getTargetValue());
  }

  public void render() {
    render(false);
  }

  public void render(boolean renderAsBackground) {
    var frame = new MainMenuPoulsenFrameContext();
    alphaAnimation.update();
    for (Map.Entry<Integer, IComponent<? super MainMenuPoulsenFrameContext>> entry :
        components.entrySet()) {
      entry.getValue().update(frame);
    }

    for (ButtonControl button : buttons) {
      button.setHovered(!renderAsBackground && button.contains(mouseX, mouseY));
    }

    var ctx = SkiaContext.get();
    if (!ctx.canRender()) {
      return; // 窗口最小化 (0 像素) 等场景, 跳过本帧渲染
    }
    var canvas = ctx.canvas();

    ctx.begin();

    // try/finally 保证 saveLayer/end 配对: 任一组件抛异常后不污染 GL 状态与 canvas 层栈
    int saveCount = -1;
    try {
      Rect screenBounds = Rect.makeWH(frame.getScaledWidth(), frame.getScaledHeight());
      int globalAlpha = Math.round(Math.clamp(alphaAnimation.getCurrentValue(), 0, 255));
      saveCount = canvas.saveLayerAlpha(screenBounds, globalAlpha);

      for (int i = 0; i < components.size(); i++) {
        if (renderAsBackground && i >= BUTTON_Z_INDEX) {
          continue;
        }
        components.get(i).render(ctx, frame);
      }
    } catch (Throwable t) {
      LOGGER.error("bocchi: main menu render failed", t);
    } finally {
      if (saveCount >= 0) {
        canvas.restoreToCount(saveCount);
      }
      ctx.end();
    }

    if (renderAsBackground) {

      GlStateManager._blendFuncSeparate(
          GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
  }

  @Override
  public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    // 布局固定 1920x1080: 把 guiScaled 鼠标坐标映射到布局坐标
    this.mouseX = (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
    this.mouseY = (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));

    this.render();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int lx = (int) (mouseX * (480.0 / Minecraft.getInstance().getWindow().getGuiScaledWidth()));
    int ly = (int) (mouseY * (270.0 / Minecraft.getInstance().getWindow().getGuiScaledHeight()));
    for (ButtonControl control : buttons) {
      if (control.contains(lx, ly)) {
        control.press();
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
      // 主菜单是顶层菜单 (取代原版标题画面): 与原版 TitleScreen 一致, ESC 不退出
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }
}
