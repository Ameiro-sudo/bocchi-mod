package me.baier.client.ui.mainmenu.misayos;

import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaCallback;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.utils.ColorUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.util.List;

public class BgRectsComponent extends AbstractBaseComponent<MainMenuMisayosFrameContext> {
  private BezierAnimation<Float> smallerRectAnim;
  private BezierAnimation<Float> biggerRectAnim;
  private final ResourceLocation BOCCHI_TEXTURE =
      ResourceLocation.parse("client/textures/bocchi.png");

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuMisayosFrameContext frame, BezierAnimation<Float> lastAnimation) {
    smallerRectAnim = BezierAnimation.createFloat(0.f, 1.f, 1000, easeFunc);
    biggerRectAnim = BezierAnimation.createFloat(0.f, 1.f, 1000, easeFunc);
    lastAnimation.then(smallerRectAnim, 0.4f);
    smallerRectAnim.then(biggerRectAnim, 0.1f);
    animations = List.of(smallerRectAnim, biggerRectAnim);
    return biggerRectAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuMisayosFrameContext frame) {
    var screenWidth = frame.getScaledWidth();
    var screenHeight = frame.getScaledHeight();
    var group1Alpha = Math.round(Mth.lerp(smallerRectAnim.getCurrentValue(), 0, 255));

    Point gradientStart = new Point(screenWidth / 2.f, 0);
    Point gradientEnd = new Point(screenWidth / 2.f, screenHeight);
    int[] gradientColors =
        new int[] {
          ColorUtil.replaceAlpha(0xFF07021C, group1Alpha),
          ColorUtil.replaceAlpha(0xFF492F49, group1Alpha)
        };
    var shader = Shader.makeLinearGradient(gradientStart, gradientEnd, gradientColors);
    var paint = new Paint().setShader(shader).setAntiAlias(true);

    var canvas = ctx.canvas();
    // canvas.saveLayerAlpha(Rect.makeWH(screenWidth, screenHeight), group1Alpha);

    canvas.drawRect(Rect.makeWH(screenWidth, screenHeight), paint);

    var block1Pos = frame.getBlock1Pos();

    var block1Size = frame.getBlock1Size();
    var currentBlock1Size = Mth.lerp(smallerRectAnim.getCurrentValue(), 0.f, block1Size);
    SkiaRenderEngine.drawRect(
        block1Pos.getX(),
        block1Pos.getY(),
        block1Size,
        currentBlock1Size,
        ColorUtil.replaceAlpha(0xFFFC67A7, group1Alpha));
    SkiaRenderEngine.drawRectLTRB(
        block1Pos.getX(),
        Math.max(0, block1Size - currentBlock1Size - 1.f),
        block1Pos.getX() + block1Size,
        block1Pos.getY(),
        ColorUtil.replaceAlpha(0xFFFB85AE, group1Alpha));

    // canvas.restore();
    var grounp2Alpha = Math.round(Mth.lerp(biggerRectAnim.getCurrentValue(), 0, 255));
    var block3Pos = frame.getBlock3Pos();

    var block3Size = frame.getBlock3Size();
    var currentBlock3Size = Mth.lerp(biggerRectAnim.getCurrentValue(), 0.f, block3Size);
    var block3Rect =
        Rect.makeXYWH(block3Pos.getX(), block3Pos.getY(), block3Size, currentBlock3Size);
    Point gradientStartBlock3 =
        new Point(block3Rect.getRight() - block3Size / 2.f, block3Rect.getTop());
    Point gradientEndBlock3 =
        new Point(block3Rect.getLeft(), block3Rect.getTop() + block1Size / 2.f);
    int[] gradientColorsBlock3 =
        new int[] {
          ColorUtil.replaceAlpha(0xFFF69DB2, grounp2Alpha),
          ColorUtil.replaceAlpha(0XFFFF78BB, grounp2Alpha)
        };
    var shaderBlock3 =
        Shader.makeLinearGradient(gradientStartBlock3, gradientEndBlock3, gradientColorsBlock3);
    var paintBlock3 = new Paint().setShader(shaderBlock3).setAntiAlias(true);
    canvas.drawRectShadow(
        block3Rect,
        -15,
        0,
        30.f,
        ColorUtil.replaceAlpha(0X55000000, Math.round(grounp2Alpha * 0.34f)));
    canvas.save();
    canvas.clipRect(block3Rect);
    canvas.drawRect(block3Rect, paintBlock3);

    SkiaRenderEngine.drawImage(
        canvas,
        BOCCHI_TEXTURE,
        block3Pos.getX(),
        block3Pos.getY(),
        block3Size,
        block3Size,
        406,
        600,
        700,
        700,
        0.07f,
        SkiaCallback.DEFAULT);

    canvas.restore();
    /* canvas.drawRectShadow(
    RRect.makeXYWH(block3Pos.getX(), block3Pos.getY(), block3Size, block3Size),
    0,
    0,
    15,
    color.getRGB());*/

    shader.close();
    paint.close();
    shaderBlock3.close();
    paintBlock3.close();
  }
}
