package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.design.Design;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.utils.ScreenUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MainTachieComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> mainGotohImageAnim;

  private final ResourceLocation GOTOH_TEXTURE = Design.resource("textures.gotoh");

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    mainGotohImageAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(mainGotohImageAnim, 0.1f);
    animations.add(mainGotohImageAnim);
    return mainGotohImageAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();

    float mainImageFinalWidth = rect1Width * 0.644f;
    float mainImageFinalHeight = mainImageFinalWidth * 3.51f;
    float mainImageFinalX = rect1PosX + mainImageFinalWidth / 2.5f;
    float mainImageFinalY = screenHeight * 0.05f;
    if (mainImageFinalHeight <= screenHeight) mainImageFinalY = screenHeight - mainImageFinalHeight;
    Point gotohImgStartPos =
        ScreenUtils.calculateStartPosition(
            mainImageFinalX,
            mainImageFinalY,
            mainImageFinalWidth,
            mainImageFinalHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (mainGotohImageAnim != null) {
      float currentImageX =
          Mth.lerp(mainGotohImageAnim.getCurrentValue(), gotohImgStartPos.getX(), mainImageFinalX);
      float currentImageY =
          Mth.lerp(mainGotohImageAnim.getCurrentValue(), gotohImgStartPos.getY(), mainImageFinalY);
      float currentImageAlpha = Math.max(0f, Math.min(1f, mainGotohImageAnim.getCurrentValue()));
      SkiaRenderEngine.drawImage(
          GOTOH_TEXTURE,
          currentImageX,
          currentImageY,
          mainImageFinalWidth,
          mainImageFinalHeight,
          currentImageAlpha,
          paint -> {
            paint.setAntiAlias(true);
          });
    }
  }
}
