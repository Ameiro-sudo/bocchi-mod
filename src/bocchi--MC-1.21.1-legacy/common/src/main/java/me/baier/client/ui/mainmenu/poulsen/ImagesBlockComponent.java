package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ScreenUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ImagesBlockComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> imageBlockAnim;
  private final ResourceLocation IMAGE_1 =
      ResourceLocation.parse("client/textures/gotoh_image_1.png");
  private final ResourceLocation IMAGE_2 =
      ResourceLocation.parse("client/textures/gotoh_image_2.png");

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    imageBlockAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(imageBlockAnim, 0.1f);
    animations.add(imageBlockAnim);

    return imageBlockAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();
    float bgFontSize = frame.getBgFontSize();

    var canvas = ctx.canvas();
    var sharedFontForGoto = FontSet.RADIKAL_BLACK.getFont(bgFontSize);
    float spacingForGoto = screenHeight * 0.045f;

    float rect3Height_val = screenHeight * 0.086f;
    float fontPosY_val = sharedFontForGoto.getHalfHeight() / 2f + spacingForGoto;
    float rect4FinalWidth = sharedFontForGoto.getHeight() * 1.25f;
    float rect4FinalX =
        rect1PosX + sharedFontForGoto.getStringWidth("GOTO") * 0.86f - rect4FinalWidth / 2;
    float rect4FinalY = fontPosY_val + sharedFontForGoto.getHeight();
    float rec4ActualHeight_val = rect3Height_val / 2f;
    float imagesRectSquareSize_val = screenHeight * 0.117f;
    float imagesBlockFinalX = rect4FinalX;
    float imagesBlockFinalY = fontPosY_val;
    float imagesBlockFinalWidth =
        (rect4FinalX + rect4FinalWidth + imagesRectSquareSize_val) - rect4FinalX;
    float imagesBlockFinalHeight = (rect4FinalY + rec4ActualHeight_val) - fontPosY_val;
    Point imgBlockStartPos =
        ScreenUtils.calculateStartPosition(
            imagesBlockFinalX,
            imagesBlockFinalY,
            imagesBlockFinalWidth,
            imagesBlockFinalHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (imageBlockAnim != null) {
      float groupCurrentX =
          Mth.lerp(imageBlockAnim.getCurrentValue(), imgBlockStartPos.getX(), imagesBlockFinalX);
      float groupCurrentY =
          Mth.lerp(imageBlockAnim.getCurrentValue(), imgBlockStartPos.getY(), imagesBlockFinalY);
      int groupCurrentAlpha = Math.round(imageBlockAnim.getCurrentValue() * 255.f);
      float fontPosY_val_render =
          sharedFontForGoto.getHalfHeight() / 2f + (frame.getScaledHeight() * 0.045f);
      float rect4FinalWidth_render = sharedFontForGoto.getHeight() * 1.25f;
      float rect4FinalX_render =
          rect1PosX + sharedFontForGoto.getStringWidth("GOTO") * 0.86f - rect4FinalWidth_render / 2;
      float imagesBlockOrigFinalX = rect4FinalX_render;
      float imagesBlockOrigFinalY = fontPosY_val_render;

      canvas.save(); // Save before translate and layer
      canvas.translate(
          groupCurrentX - imagesBlockOrigFinalX, groupCurrentY - imagesBlockOrigFinalY);
      canvas.saveLayerAlpha(null, groupCurrentAlpha);

      float rect4DrawPosY_render = fontPosY_val_render + sharedFontForGoto.getHeight();
      float rec4ActualHeight_render = rect3Height_val / 2f;
      SkiaRenderEngine.drawRect(
          rect4FinalX_render,
          rect4DrawPosY_render,
          rect4FinalWidth_render,
          rec4ActualHeight_render,
          0xFFE95A9F);
      float imagesDrawPosX_render = rect4FinalX_render + rect4FinalWidth_render;
      float imagesRectSquareSize_render = frame.getScaledHeight() * 0.117f;
      SkiaRenderEngine.drawRect(
          imagesDrawPosX_render,
          fontPosY_val_render,
          imagesRectSquareSize_render,
          imagesRectSquareSize_render,
          0XFFFFFFFF);
      float imagesActualSquareSize_render = imagesRectSquareSize_render * 0.85f;
      SkiaRenderEngine.drawImage(
          IMAGE_1,
          imagesDrawPosX_render + (imagesRectSquareSize_render - imagesActualSquareSize_render) / 2,
          fontPosY_val_render + (imagesRectSquareSize_render - imagesActualSquareSize_render) / 2,
          imagesActualSquareSize_render,
          imagesActualSquareSize_render,
          1);
      float images2DrawPosY_render =
          rect4DrawPosY_render + rec4ActualHeight_render - imagesRectSquareSize_render;
      SkiaRenderEngine.drawRect(
          imagesDrawPosX_render,
          images2DrawPosY_render,
          imagesRectSquareSize_render,
          imagesRectSquareSize_render,
          0XFFFFFFFF);
      SkiaRenderEngine.drawImage(
          IMAGE_2,
          imagesDrawPosX_render + (imagesRectSquareSize_render - imagesActualSquareSize_render) / 2,
          images2DrawPosY_render
              + (imagesRectSquareSize_render - imagesActualSquareSize_render) / 2,
          imagesActualSquareSize_render,
          imagesActualSquareSize_render,
          1);

      canvas.restore(); // Restore layer
      canvas.restore(); // Restore translate
    }
  }
}
