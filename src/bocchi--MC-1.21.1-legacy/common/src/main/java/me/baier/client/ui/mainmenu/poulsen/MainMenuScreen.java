package me.baier.client.ui.mainmenu.poulsen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.mainmenu.common.LogoComponent;
import me.baier.client.ui.mainmenu.component.interfaces.IComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL33C;

import java.util.HashMap;
import java.util.Map;

public class MainMenuScreen extends Screen {
  public static MainMenuScreen INSTANCE;
  private static final Component TITLE = Component.literal("Main Menu");
  private final Map<Integer, IComponent<? super MainMenuPoulsenFrameContext>> components =
      new HashMap<>();

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
    components.put(
        14,
        new ButtonControl(
            "SinglePlayer",
            button -> {},
            (button, frame) -> {
              var buttonX = frame.getScaledWidth() * 0.03f;
              var buttonY = frame.getScaledHeight() * 0.1f;
              return new Point(buttonX - 3.5f, buttonY);
            }));

    onBeginFadeIn();
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return true;
  }

  public void onBeginFadeIn() {
    MainMenuPoulsenFrameContext frame = new MainMenuPoulsenFrameContext();

    alphaAnimation = BezierAnimation.createFloat(0.f, 255.f, 1500, BezierControlPoints.EASE_IN_OUT);

    var lastMajorAnchorAnimation = alphaAnimation;
    for (Map.Entry<Integer, IComponent<? super MainMenuPoulsenFrameContext>> entry :
        components.entrySet()) {
      lastMajorAnchorAnimation = entry.getValue().initAnimations(frame, lastMajorAnchorAnimation);
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

    var ctx = SkiaContext.get();
    var canvas = ctx.canvas();

    ctx.begin();

    Rect screenBounds = Rect.makeWH(frame.getScaledWidth(), frame.getScaledHeight());
    int globalAlpha = Math.round(Math.clamp(alphaAnimation.getCurrentValue(), 0, 255));
    canvas.saveLayerAlpha(screenBounds, globalAlpha);

    for (int i = 0; i < components.size(); i++) {
      components.get(i).render(ctx, frame);
    }

    canvas.restore();
    ctx.end();

    if (renderAsBackground) {

      GlStateManager._blendFuncSeparate(
          GL33C.GL_SRC_ALPHA, GL33C.GL_ONE_MINUS_SRC_ALPHA, GL33C.GL_ONE, GL33C.GL_ZERO);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
  }

  @Override
  public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {

    this.render();
  }
}
