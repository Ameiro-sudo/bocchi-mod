package me.baier.client.ui.mainmenu.misayos;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.Bocchi;
import me.baier.client.ui.api.BreathingUtil;
import me.baier.client.ui.common.components.AlbumRenderer;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.design.Design;
import me.baier.event.EventMonitor;
import me.baier.event.impl.MouseClickEvent;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaEnvironment;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.graphics.effects.SkijaEffects;
import me.baier.manager.EventManager;
import me.baier.utils.ScreenUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MainTachieComponent extends AbstractBaseComponent<MainMenuMisayosFrameContext>
    implements EventMonitor {
  private BezierAnimation<Float> mainGotohImageAnim;
  private BezierAnimation<Float> mainGotohImageAlphaAnim;
  private BezierAnimation<Float> albumAnim;
  private BezierAnimation<Float> albumAlphaAnim;
  private BreathingUtil breathing = new BreathingUtil(3000, -3, 0);
  private final ResourceLocation TACHIE_TEXTURE = Design.resource("textures.bocchi");

  public MainTachieComponent() {}

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuMisayosFrameContext frame, BezierAnimation<Float> lastAnimation) {
    albumAnim = BezierAnimation.createFloat(0.f, 1.f, 500, BezierControlPoints.CIRC_OUT);

    albumAlphaAnim =
        BezierAnimation.createFloat(
            0.f, 1.f, 500, new BezierControlPoints(0.83f, 0.02f, 0.98f, 0.80f));
    mainGotohImageAlphaAnim =
        BezierAnimation.createFloat(
            0.f, 1.f, 500, new BezierControlPoints(0.83f, 0.02f, 0.98f, 0.80f));
    mainGotohImageAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(mainGotohImageAnim, 0.1f);
    lastAnimation.then(mainGotohImageAlphaAnim, 0.1f);
    mainGotohImageAnim.then(albumAnim, 0.05f);
    mainGotohImageAnim.then(albumAlphaAnim, 0.05f);
    animations.add(mainGotohImageAnim);
    animations.add(albumAnim);
    animations.add(albumAlphaAnim);
    animations.add(mainGotohImageAlphaAnim);
    return mainGotohImageAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuMisayosFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();
    var block3Pos = frame.getBlock3Pos();
    var block3Size = frame.getBlock3Size();

    var canvas = ctx.canvas();

    float mainImageFinalHeight = screenHeight * 0.95f;
    float mainImageFinalWidth = mainImageFinalHeight * 1.035483870967742f;
    float mainImageFinalX = block3Pos.getX() + block3Size * 0.1f;
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
      float currentImageAlpha =
          Math.max(0f, Math.min(1f, mainGotohImageAlphaAnim.getCurrentValue()));
      var rect3 = Rect.makeXYWH(block3Pos.getX(), block3Pos.getY(), block3Size, block3Size);
      var recordRect =
          Rect.makeXYWH(
              screenWidth * 0.6f, screenHeight * 0.18f, screenHeight * 0.65f, screenHeight * 0.65f);
      /*
      var colorPaint = SkiaEnvironment.getCurrent().borrowPaint();
      canvas.drawRect(recordRect, colorPaint.setColor(0xAAFFFFFF));
      SkiaEnvironment.getCurrent().recyclePaint(colorPaint);*/
      Point albumStartPos =
          ScreenUtils.calculateStartPosition(
              recordRect.getLeft(),
              recordRect.getTop(),
              recordRect.getWidth(),
              recordRect.getHeight(),
              screenWidth,
              screenHeight,
              animOffset);
      float currenAlbumX =
          Mth.lerp(albumAnim.getCurrentValue(), albumStartPos.getX(), recordRect.getLeft());
      float currentAlbumY =
          Mth.lerp(albumAnim.getCurrentValue(), albumStartPos.getY(), recordRect.getTop());
      var currentAlbumAlpha = Math.round(Mth.lerp(albumAlphaAnim.getCurrentValue(), 0.f, 255.f));
      var rect =
          Rect.makeXYWH(currenAlbumX, currentAlbumY, recordRect.getWidth(), recordRect.getHeight())
              .inflate(-5.f);
      canvas.saveLayerAlpha(recordRect, currentAlbumAlpha);
      AlbumRenderer.render(canvas, rect);
      canvas.restore();

      canvas.save();
      canvas.clipRect(rect3);

      try (var blendFilter = ColorFilter.makeBlend(0x40000000, BlendMode.SRC_IN)) {
        SkiaRenderEngine.drawImage(
            TACHIE_TEXTURE,
            currentImageX - 15.f,
            currentImageY,
            mainImageFinalWidth,
            mainImageFinalHeight,
            currentImageAlpha,
            paint -> {
              paint.setColorFilter(blendFilter);
              paint.setAntiAlias(true);
            });
      }
      canvas.restore();

      canvas.save();
      // canvas.translate(0, breathing.getCurrentValue());
      ctx.startRotate(
          currentImageX + mainImageFinalWidth * 0.38f,
          currentImageY + mainImageFinalHeight + breathing.getCurrentValue(),
          -(breathing.getCurrentValue() + 1.5f) / 3);

      SkiaRenderEngine.drawImage(
          TACHIE_TEXTURE,
          currentImageX,
          currentImageY,
          mainImageFinalWidth,
          mainImageFinalHeight,
          currentImageAlpha,
          paint -> {
            paint.setAntiAlias(true);
          });
      ctx.stopRotate();
      float squareSize = 9.f;
      Path squarePath = new Path();
      squarePath.addRect(Rect.makeXYWH(-squareSize / 2f, -squareSize / 2f, squareSize, squareSize));
      squarePath.closePath();

      Paint basePaint = new Paint().setColor(0xFFFFFFFF).setAntiAlias(true);

      Point p0 =
          new Point(
              currentImageX + mainImageFinalWidth * 0.45f,
              currentImageY + mainImageFinalHeight * 0.85f);
      Point p1 =
          new Point(
              currentImageX + mainImageFinalWidth * 0.55f,
              currentImageY + mainImageFinalHeight * 0.55f);
      Point p2 =
          new Point(
              currentImageX + mainImageFinalWidth * 0.8f,
              currentImageY + mainImageFinalHeight * 0.8f);

      int numCopies = 9;
      float initialRotation = -30f;
      float startScale = 1.0f;
      float endScale = 0.01f;
      float startOpacity = 1.0f;
      float endOpacity = 0.1f;

      var currentNumCopies = Mth.lerp(mainGotohImageAnim.getCurrentValue(), 3, numCopies);

      SkijaEffects.drawShapeAlongCurve(
          canvas,
          squarePath,
          basePaint,
          Math.round(currentNumCopies),
          p0,
          p1,
          p2,
          initialRotation,
          startScale,
          endScale,
          startOpacity,
          endOpacity);

      canvas.restore();
      squarePath.close();
      basePaint.close();
    }
  }
}
