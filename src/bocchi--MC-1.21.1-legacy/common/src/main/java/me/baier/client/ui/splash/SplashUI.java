package me.baier.client.ui.splash;

import java.io.IOException;
import java.util.Objects;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.ui.api.BreathingUtil;
import me.baier.client.ui.common.components.LogoRenderer;
import me.baier.client.ui.mainmenu.misayos.MainMenuMisayosScreen;
import me.baier.client.ui.mainmenu.poulsen.MainMenuScreen;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.event.EventMonitor;
import me.baier.event.impl.MouseClickEvent;
import me.baier.graphics.SkiaCallback;
import me.baier.graphics.SkiaContext;
import me.baier.client.ui.model.SplashFrameContext;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import static me.baier.utils.ColorUtil.lerpColor;

@Slf4j
public class SplashUI implements EventMonitor {

  public static final int BRAND_ARGB = FastColor.ARGB32.color(255, 0X1F, 0X1F, 0X1F);
  public static SplashUI INSTANCE;

  public ResourceLocation BOCCHI_LOADING_TEXTURE =
      ResourceLocation.parse("client/textures/bocchi_loading.png");

  private SplashFrameContext frame;

  private final LoadingAnimateRenderer animateRenderer;

  private final float fontHeight = 9.4f;

  private BezierAnimation<Float> globalAlphaAnim;
  private BezierAnimation<Float> progressAnim;
  private BezierAnimation<Float> logoAnimation;

  private BreathingUtil breathingTextAlpha;

  private float iLogoX, tLogoX;
  private float iLogoY, tLogoY;
  private float iLogoW, tLogoW;
  private float iLogoH, tLogoH;
  private int iLogoC, tLogoC;
  @Getter private boolean waitingForInput = false;

  public SplashUI() {
    INSTANCE = this;
    animateRenderer = new LoadingAnimateRenderer(BOCCHI_LOADING_TEXTURE, 20);
    if (frame == null) {
      frame = new SplashFrameContext();
    }
    iLogoW = 94.f;
    iLogoH = iLogoW * 0.317f;
    iLogoX = frame.getMidX() - iLogoW / 2;
    iLogoY = frame.getMidY() - fontHeight - frame.getSpacing();
    iLogoC = 0xFFFFFFFF;
    createMonitor(
        MouseClickEvent.class,
        event -> {
          if (waitingForInput) {
            waitingForInput = false;
            onBeginFadeOut();
          }
        });
  }

  public void onOKToFadeOut() {
    waitingForInput = true;
    progressAnim = BezierAnimation.createFloat(0.f, 1.f, 500, BezierControlPoints.CUBIC_OUT);
    progressAnim.start();
    breathingTextAlpha = new BreathingUtil(1000, 0.65f, 1.f);
  }

  public void onBeginFadeOut() {
    var screen =
        Objects.requireNonNullElseGet(MainMenuMisayosScreen.INSTANCE, MainMenuMisayosScreen::new);

    // screen.onBeginFadeIn();
    Minecraft.getInstance().setScreen(screen);
    screen.onBeginFadeIn();
    globalAlphaAnim =
        BezierAnimation.createFloat(0.0F, 250.f, 1000, BezierControlPoints.EASE_IN_OUT);

    globalAlphaAnim.set(255.f);
    float rect1Width = frame.getScaledWidth() * 0.412f;
    var rect1PosX = (frame.getScaledWidth() - rect1Width) / 2;
    tLogoX = rect1PosX * 0.3f;
    tLogoY = frame.getScaledHeight() * 0.1f;
    tLogoW = 60.f;
    tLogoH = tLogoW * 0.317f;
    tLogoC = 0XFFFFFFFF;
  }

  public boolean isEnded() {
    return globalAlphaAnim.isComplete();
  }

  public boolean isOkToFadeOut() {
    return globalAlphaAnim == null && !waitingForInput;
  }

  public void renderLogoWithColor(Canvas canvas, float x, float y, float w, float h, int color) {
    LogoRenderer.renderLogoWithColor(canvas, x, y, w, h, color);
  }

  public void render(SkiaContext ctx) {
    frame = new SplashFrameContext();

    var canvas = ctx.canvas();
    if (globalAlphaAnim != null) {
      globalAlphaAnim.update();
      if (globalAlphaAnim.getProgress() >= 0.1f && logoAnimation == null) {
        logoAnimation =
            BezierAnimation.createFloat(
                0.0F, 1.f, 1000, new BezierControlPoints(0.9f, 0.0f, 0.1f, 1.0f));
        logoAnimation.set(1.f);
      }

      Rect screenBounds = Rect.makeWH(frame.getScaledWidth(), frame.getScaledHeight());
      int alpha = Math.clamp(Math.round(255.f - globalAlphaAnim.getCurrentValue()), 0, 255);
      canvas.saveLayerAlpha(screenBounds, alpha);
    }

    canvas.clear(SplashUI.BRAND_ARGB);
  }

  public void renderProgress(SkiaContext ctx, float progress, float f) throws IOException {
    if (progressAnim != null) progressAnim.update();
    int alpha = Math.round(Math.clamp(f, 0.0F, 3.f) * 85);
    int argb = FastColor.ARGB32.color(alpha, 255, 255, 255);
    var posX = (float) (frame.getScaledWidth()) * 0.2f;
    var posY = frame.getMidY() + frame.getSpacing() + fontHeight / 2;
    var width = (frame.getScaledWidth() * 0.6f) * progress;

    var canvas = SkiaContext.get().canvas();

    canvas.saveLayerAlpha(
        null,
        Math.round(255 - (progressAnim != null ? progressAnim.getCurrentValue() : 0.f) * 255.f));

    animateRenderer.computeFrame();
    try (Paint paint = new Paint()) {
      paint.setAntiAlias(true);
      paint.setColor(argb);
      var rrect = RRect.makeXYWH(posX, posY, width, fontHeight, 15.f);
      canvas.saveLayer(rrect, null);
      // Render as mask
      canvas.drawRRect(rrect, paint);
      animateRenderer.render(
          canvas,
          posX + width - fontHeight * 1.5f,
          posY - fontHeight * 1.5f,
          fontHeight * 2.5f,
          fontHeight * 2.5f,
          1,
          b -> b.setBlendMode(BlendMode.DST_OUT));
      canvas.restore();
    }

    animateRenderer.render(
        canvas,
        posX + width - fontHeight * 1.5f,
        posY - fontHeight * 1.5f,
        fontHeight * 2.5f,
        fontHeight * 2.5f,
        alpha / 255.f,
        SkiaCallback.DEFAULT);

    canvas.restore(); // Restore layer(alpha)

    if (progressAnim != null) {
      var fontRenderer = FontSet.RADIKAL_REGULAR.getFont(15);
      var stringWidth = fontRenderer.getStringWidth("TAP TO START");
      var midX = frame.getMidX() - stringWidth / 2;
      var textAlpha = progressAnim.getCurrentValue() * 255.f;
      var color =
          ColorUtil.replaceAlpha(
              0xFFFFFFFF,
              Math.round(Mth.lerp(breathingTextAlpha.getCurrentValue(), 0.f, textAlpha)));
      fontRenderer.drawString(
          "TAP TO START",
          midX,
          posY + fontRenderer.getHalfHeight() / 2f,
          color,
          false,
          paint -> {
            paint.setMaskFilter(MaskFilter.makeBlur(FilterBlurMode.SOLID, 5f));
          });
    }

    if (globalAlphaAnim != null) {
      canvas.restore();
    }

    if (logoAnimation != null) {
      logoAnimation.update();
      float t = logoAnimation.getCurrentValue();
      float currentX = Mth.lerp(t, iLogoX, tLogoX);
      float currentY = Mth.lerp(t, iLogoY, tLogoY);
      float currentW = Mth.lerp(t, iLogoW, tLogoW);
      float currentH = Mth.lerp(t, iLogoH, tLogoH);
      int currentColor = lerpColor(iLogoC, tLogoC, t);
      this.renderLogoWithColor(canvas, currentX, currentY, currentW, currentH, currentColor);
    } else {
      this.renderLogoWithColor(canvas, iLogoX, iLogoY, iLogoW, iLogoH, iLogoC);
    }
  }
}
