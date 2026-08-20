package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

public class JapaneseNamesComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> gotohNameAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    gotohNameAnim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(gotohNameAnim, 0.1f);
    animations.add(gotohNameAnim);
    return gotohNameAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    var rect1Width = frame.getRect1Width();
    float bgFontSize = frame.getBgFontSize();

    var canvas = ctx.canvas();

    var nameFont_val = FontSet.MEIRYO_BOLD.getFont(bgFontSize * 0.221f);
    var kanaFont_val = FontSet.MEIRYO_BOLD.getFont((bgFontSize * 0.221f) * 0.375f);
    String nameText_val = "後藤 ひとり";
    String kanaText_val = "ご   とう";
    float nameTextWidth_val = nameFont_val.getStringWidth(nameText_val);
    float kanaTextWidth_val = kanaFont_val.getStringWidth(kanaText_val);
    float nameTextFinalDrawX = (rect1PosX * 0.5f) - nameTextWidth_val / 2;
    float nameTextFinalDrawY = (screenHeight * 0.8f) - nameFont_val.getHalfHeight();
    float kanaTextFinalDrawX = nameTextFinalDrawX + nameTextWidth_val * 0.07f;
    float kanaTextFinalDrawY =
        nameTextFinalDrawY - nameFont_val.getHeight() - kanaFont_val.getHeight() * 1.25f;
    float nameBlockMinX = Math.min(nameTextFinalDrawX, kanaTextFinalDrawX);
    float nameBlockMinY = kanaTextFinalDrawY - kanaFont_val.getHeight();
    float nameBlockMaxX =
        Math.max(nameTextFinalDrawX + nameTextWidth_val, kanaTextFinalDrawX + kanaTextWidth_val);
    float nameBlockMaxY = nameTextFinalDrawY + nameFont_val.getFont().getMetrics().getDescent();
    float nameBlockFinalWidth = nameBlockMaxX - nameBlockMinX;
    float nameBlockFinalHeight = nameBlockMaxY - nameBlockMinY;
    Point nameBlockStartPos =
        ScreenUtils.calculateStartPosition(
            nameBlockMinX,
            nameBlockMinY,
            nameBlockFinalWidth,
            nameBlockFinalHeight,
            screenWidth,
            screenHeight,
            animOffset);
    if (gotohNameAnim != null) {
      var nameFont_render = FontSet.MEIRYO_BOLD.getFont(bgFontSize * 0.221f);
      var kanaFont_render = FontSet.MEIRYO_BOLD.getFont((bgFontSize * 0.221f) * 0.375f);
      String nameText_render = "後藤 ひとり";
      String kanaText_render = "ご   とう";
      float nameTextWidth_render = nameFont_render.getStringWidth(nameText_render);
      float groupCurrentX =
          Mth.lerp(gotohNameAnim.getCurrentValue(), nameBlockStartPos.getX(), nameBlockMinX);
      float groupCurrentY =
          Mth.lerp(gotohNameAnim.getCurrentValue(), nameBlockStartPos.getY(), nameBlockMinY);
      int groupCurrentAlpha = Math.round(gotohNameAnim.getCurrentValue() * 255.f);
      float nameTextOrigFinalDrawX = (rect1PosX * 0.5f) - nameTextWidth_render / 2;
      float nameTextOrigFinalDrawY =
          (frame.getScaledHeight() * 0.8f) - nameFont_render.getHalfHeight();
      float kanaTextOrigFinalDrawX = nameTextOrigFinalDrawX + nameTextWidth_render * 0.07f;
      float kanaTextOrigFinalDrawY =
          nameTextOrigFinalDrawY
              - nameFont_render.getHeight()
              - kanaFont_render.getHeight() * 1.25f;
      float nameBlockMinX_final = Math.min(nameTextOrigFinalDrawX, kanaTextOrigFinalDrawX);
      float nameBlockMinY_final = kanaTextOrigFinalDrawY - kanaFont_render.getHeight();

      canvas.save(); // Save before translate and layer
      canvas.translate(groupCurrentX - nameBlockMinX_final, groupCurrentY - nameBlockMinY_final);
      canvas.saveLayerAlpha(null, groupCurrentAlpha);
      nameFont_render.drawString(
          nameText_render, nameTextOrigFinalDrawX, nameTextOrigFinalDrawY, 0XFFE95A9F);
      kanaFont_render.drawString(
          kanaText_render, kanaTextOrigFinalDrawX, kanaTextOrigFinalDrawY, 0xFF000000);
      canvas.restore(); // Restore layer
      canvas.restore(); // Restore translate
    }
  }
}
