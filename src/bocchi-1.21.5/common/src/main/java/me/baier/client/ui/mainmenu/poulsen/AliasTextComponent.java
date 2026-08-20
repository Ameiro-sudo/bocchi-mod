package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

public class AliasTextComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> aliasTextBlockAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    aliasTextBlockAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(aliasTextBlockAnim, 0.1f);
    animations.add(aliasTextBlockAnim);
    return aliasTextBlockAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();
    float bgFontSize = frame.getBgFontSize();
    float rect3PosX = rect1PosX + rect1Width;
    var canvas = ctx.canvas();

    var nameFont_alias = FontSet.MEIRYO_BOLD.getFont(bgFontSize * 0.221f);
    var quotationFont = FontSet.MEIRYO_BOLD.getFont((bgFontSize * 0.221f) * 1.25f);
    String aliasText = "ギターヒーロー";
    float aliasTextWidth = nameFont_alias.getStringWidth(aliasText);
    float aliasTextFinalX = (rect1PosX + rect1Width) - nameFont_alias.getHalfHeight();
    float aliasTextFinalY = screenHeight * 0.45f;
    float aliasBlockUntransformedMinX = aliasTextFinalX;
    float aliasBlockUntransformedMinY =
        aliasTextFinalY - nameFont_alias.getHeight() - quotationFont.getHeight();
    float aliasBlockUntransformedMaxX = aliasTextFinalX + aliasTextWidth;
    float aliasBlockUntransformedMaxY = aliasTextFinalY + nameFont_alias.getHeight() * 2;
    float aliasBlockFinalWidth = aliasBlockUntransformedMaxX - aliasBlockUntransformedMinX;
    float aliasBlockFinalHeight = aliasBlockUntransformedMaxY - aliasBlockUntransformedMinY;
    Point aliasBlockStartPos =
        ScreenUtils.calculateStartPosition(
            aliasBlockUntransformedMinX,
            aliasBlockUntransformedMinY,
            aliasBlockFinalWidth,
            aliasBlockFinalHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (aliasTextBlockAnim != null) {
      float groupCurrentX =
          Mth.lerp(
              aliasTextBlockAnim.getCurrentValue(),
              aliasBlockStartPos.getX(),
              aliasBlockUntransformedMinX);
      float groupCurrentY =
          Mth.lerp(
              aliasTextBlockAnim.getCurrentValue(),
              aliasBlockStartPos.getY(),
              aliasBlockUntransformedMinY);
      int groupCurrentAlpha = Math.round(aliasTextBlockAnim.getCurrentValue() * 255.f);

      canvas.save(); // Save before translate & scale and layer
      canvas.translate(
          groupCurrentX - aliasBlockUntransformedMinX, groupCurrentY - aliasBlockUntransformedMinY);
      canvas.saveLayerAlpha(null, groupCurrentAlpha);
      float pivotAlliasX = aliasTextFinalX + aliasTextWidth / 2.0f;
      float pivotAlliasY = aliasTextFinalY;
      canvas.translate(pivotAlliasX, pivotAlliasY);
      canvas.scale(0.85f, 1.f);
      canvas.translate(-pivotAlliasX, -pivotAlliasY);
      quotationFont.drawString(
          "“",
          aliasTextFinalX,
          aliasTextFinalY - nameFont_alias.getHeight() - quotationFont.getHalfHeight() / 2f,
          0XFFFFFFFF);
      nameFont_alias.drawString(aliasText, aliasTextFinalX, aliasTextFinalY, 0XFFFFFFFF);
      quotationFont.drawString(
          "”",
          aliasTextFinalX + aliasTextWidth - nameFont_alias.getHeight(),
          aliasTextFinalY + nameFont_alias.getHeight() * 2 - quotationFont.getHalfHeight() / 2f,
          0XFFFFFFFF);
      canvas.restore(); // Restore internal scale

      canvas.restore(); // Restore layer
    }
  }
}
