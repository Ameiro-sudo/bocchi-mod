package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.utils.ColorUtil;
import me.baier.utils.ScreenUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BgImageComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private final ResourceLocation GOTOH_TEXTURE =
      ResourceLocation.parse("client/textures/gotoh.png");
  private BezierAnimation<Float> bgLargeImageAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    bgLargeImageAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(bgLargeImageAnim, 0.1f);
    animations.add(bgLargeImageAnim);
    return bgLargeImageAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();
    float bgFontSize = frame.getBgFontSize();
    float rect3PosX = rect1PosX + rect1Width;
    float rect3PosY = screenHeight * 0.586f;
    var rect3Height = frame.getScaledHeight() * 0.086f;
    var canvas = ctx.canvas();
    float bgLargeImageFinalWidth = (rect1Width * 0.644f) * 2.74f;
    float bgLargeImageFinalHeight = bgLargeImageFinalWidth * 3.51f;
    float bgLargeImageFinalX = -(screenWidth * 0.188f);
    float bgLargeImageFinalY = -bgLargeImageFinalHeight / 30;

    Point bgLargeImgStartPos =
        ScreenUtils.calculateStartPosition(
            bgLargeImageFinalX,
            bgLargeImageFinalY,
            bgLargeImageFinalWidth,
            bgLargeImageFinalHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (bgLargeImageAnim != null) {
      float currentBgLargeImageX =
          Mth.lerp(
              bgLargeImageAnim.getCurrentValue(), bgLargeImgStartPos.getX(), bgLargeImageFinalX);
      float currentBgLargeImageY =
          Mth.lerp(
              bgLargeImageAnim.getCurrentValue(), bgLargeImgStartPos.getY(), bgLargeImageFinalY);
      float currentBgLargeImageAlpha = bgLargeImageAnim.getCurrentValue() * 0.18f;

      float bgImgRenderWidth = (rect1Width * 0.644f) * 2.74f;
      float bgImgRenderHeight = bgImgRenderWidth * 3.51f;

      canvas.save();
      canvas.translate(
          currentBgLargeImageX + bgImgRenderWidth / 2.0f,
          currentBgLargeImageY + bgImgRenderHeight / 2.0f);
      canvas.scale(-1, 1);
      SkiaRenderEngine.drawImage(
          GOTOH_TEXTURE,
          -bgImgRenderWidth / 2.0f,
          -bgImgRenderHeight / 2.0f,
          bgImgRenderWidth,
          bgImgRenderHeight,
          currentBgLargeImageAlpha);

      canvas.restore();
      SkiaRenderEngine.drawRectLTRB(
          rect3PosX,
          rect3PosY,
          (float) frame.getScaledWidth(),
          rect3PosY + rect3Height,
          ColorUtil.replaceAlpha(0XFFFFFFFF, Math.round(bgLargeImageAnim.getCurrentValue() * 255)));
    } else {
      float bgImgRenderWidth = (rect1Width * 0.644f) * 2.74f;
      float bgImgRenderHeight = bgImgRenderWidth * 3.51f;
      float bgImgStaticX = -(frame.getScaledWidth() * 0.188f);
      float bgImgStaticY = -bgImgRenderHeight / 30;
      canvas.save();
      canvas.translate(
          bgImgStaticX + bgImgRenderWidth / 2.0f, bgImgStaticY + bgImgRenderHeight / 2.0f);
      canvas.scale(-1, 1);
      SkiaRenderEngine.drawImage(
          GOTOH_TEXTURE,
          -bgImgRenderWidth / 2.0f,
          -bgImgRenderHeight / 2.0f,
          bgImgRenderWidth,
          bgImgRenderHeight,
          0.18f);

      canvas.restore();
      SkiaRenderEngine.drawRectLTRB(
          rect3PosX,
          rect3PosY,
          (float) frame.getScaledWidth(),
          rect3PosY + rect3Height,
          0xFFFFFFFF);
    }
  }
}
