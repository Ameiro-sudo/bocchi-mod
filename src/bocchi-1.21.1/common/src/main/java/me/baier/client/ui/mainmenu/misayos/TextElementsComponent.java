package me.baier.client.ui.mainmenu.misayos;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.design.Design;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.graphics.font.FontSet;
import me.baier.graphics.font.SkiaFontRenderer;
import me.baier.utils.ColorUtil;
import net.minecraft.util.Mth;

import java.util.List;

import static me.baier.utils.RenderUtils.drawTextBox;

public class TextElementsComponent extends AbstractBaseComponent<MainMenuMisayosFrameContext> {

  private BezierAnimation<Float> mainTextBlockAnim;
  private BezierAnimation<Float> infoTextAnim;
  private BezierAnimation<Float> lineAnim;
  private BezierAnimation<Float> decorateAnim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuMisayosFrameContext frame, BezierAnimation<Float> lastAnimation) {
    mainTextBlockAnim = BezierAnimation.createFloat(0.f, 1.f, 700, easeFunc);
    infoTextAnim = BezierAnimation.createFloat(0.f, 1.f, 700, easeFunc);
    lineAnim = BezierAnimation.createFloat(0.f, 1.f, 700, easeFunc);
    decorateAnim = BezierAnimation.createFloat(0.f, 1.f, 700, easeFunc);
    lastAnimation.then(mainTextBlockAnim, 0.05f);
    mainTextBlockAnim.then(infoTextAnim, 0.05f);
    infoTextAnim.then(lineAnim, 0.05f);
    lineAnim.then(decorateAnim, 0.05f);
    animations = List.of(mainTextBlockAnim, infoTextAnim, lineAnim, decorateAnim);
    return decorateAnim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuMisayosFrameContext frame) {
    var screenWidth = frame.getScaledWidth();
    var screenHeight = frame.getScaledHeight();
    var blockPos1 = frame.getBlock1Pos();
    var blockSize = frame.getBlock1Size();
    var canvas = ctx.canvas();

    var infoFont = FontSet.SH_LIGHT.getFont(6.5f);
    var decorateFont = FontSet.SH_REGULAR.getFont(8.5f);
    var mainTextFont = FontSet.SH_HEAVY.getFont(53);
    var roundBoxFont = FontSet.SH_REGULAR.getFont(13f);
    var textBoxFont = FontSet.SH_NORMAL.getFont(9.5f);
    var infoCurrentSpacing = Mth.lerp(infoTextAnim.getCurrentValue(), 0.5f, -0.15f);
    var infoCurrentColor =
        ColorUtil.replaceAlpha(
            0xFFFFFFFF, Math.round(Mth.lerp(infoTextAnim.getCurrentValue(), 0.f, 255.f)));
    String infoLine1 = Design.value("texts.mInfoLine1", "Goto, nicknamed \"Little Solitude\", ");
    String infoLine2 =
        Design.value("texts.mInfoLine2", "is a girl who always starts her speech with \"Ah...\" ");
    String infoLine3 = Design.value("texts.mInfoLine3", "and is extremely accepting and introverted");
    infoFont.drawString(
        infoLine1,
        blockPos1.getX() + blockSize * 0.15f,
        blockSize * 0.05f - infoFont.getHalfHeight() / 2,
        infoCurrentColor,
        infoCurrentSpacing);
    infoFont.drawString(
        infoLine2,
        blockPos1.getX() + blockSize * 0.15f,
        blockSize * 0.05f - infoFont.getHalfHeight() / 2 + infoFont.getHeight() + 1.f,
        infoCurrentColor,
        Mth.lerp(infoTextAnim.getCurrentValue(), 0.5f, -0.15f));
    infoFont.drawString(
        infoLine3,
        blockPos1.getX() + blockSize * 0.15f,
        blockSize * 0.05f - infoFont.getHalfHeight() / 2 + infoFont.getHeight() * 2 + 2.f,
        infoCurrentColor,
        infoCurrentSpacing);
    var line1CurrentEndX =
        Mth.lerp(lineAnim.getCurrentValue(), blockPos1.getX(), screenWidth * 0.55f);
    var line2CurrentEndX =
        Mth.lerp(lineAnim.getCurrentValue(), 0, blockPos1.getX() + blockSize * 0.735f + 4.5f);
    try (var paint =
        new Paint().setMode(PaintMode.STROKE).setStrokeWidth(0.5f).setColor(0xFFFFFFFF)) {
      canvas.drawLine(
          blockPos1.getX(), blockPos1.getY(), line1CurrentEndX, blockPos1.getY(), paint);
      canvas.drawLine(
          0, blockPos1.getY() + 5.f - 0.2f, line2CurrentEndX, blockPos1.getY() + 5.f - 0.2f, paint);
      paint.reset();
      paint.setColor(0xFFFFFFFF);
      var rectWidth = blockSize * 0.6f;
      var rectHeight = 5.f;
      canvas.drawRect(
          Rect.makeXYWH(
              blockPos1.getX() - blockSize * 0.15f,
              blockPos1.getY() - rectHeight,
              rectWidth,
              Mth.lerp(lineAnim.getCurrentValue(), 0.f, rectHeight)),
          paint);
    }
    var currentDecorateSpacing = Mth.lerp(decorateAnim.getCurrentValue(), 0.5f, 2.4f);
    var currentDecorateColor =
        ColorUtil.replaceAlpha(
            0XFFFFFFFF, Math.round(Mth.lerp(decorateAnim.getCurrentValue(), 0.f, 255.f)));
    String decorateText = Design.value("texts.mPhobia", "SOCIAL  PHOBIA");
    decorateFont.drawString(
        decorateText,
        blockPos1.getX() + blockSize * 0.04f,
        blockPos1.getY() + 0.5f,
        currentDecorateColor,
        currentDecorateSpacing);

    // 大标题组文案可配置 (design.json texts.*); 宽度锚点按实际文案测量
    String bocchiText = Design.value("texts.mBocchi", "BOCCHI");
    String theRockText = Design.value("texts.mRock", "THE ROCK!");
    String gotohBoxText = Design.value("texts.mBoxGotoh", "Gotoh Hitori");
    String reclusiveBoxText = Design.value("texts.mBoxGirl", "A reclusive girl");

    float bocchiTextFinalX = blockPos1.getX() - mainTextFont.getStringWidth(charAt(bocchiText, 0)) * 1.2f;
    float bocchiTextFinalY = blockPos1.getY() + mainTextFont.getHeight() - 5.f; // Baseline
    float bocchiTextWidth = mainTextFont.getStringWidth(bocchiText, -1.5f);

    float theRockFinalPosX = blockPos1.getX() - mainTextFont.getStringWidth(charAt(theRockText, 0)) / 2f;
    float theRockFinalPosY = blockPos1.getY() + mainTextFont.getHeight() * 2.f; // Baseline
    float theRockTextWidth = mainTextFont.getStringWidth(theRockText, -1.5f);

    float gotohTextXArg =
        theRockFinalPosX
            + (mainTextFont.getStringWidth(prefix(theRockText, 6), -1.5f)
                    + mainTextFont.getStringWidth(prefix(theRockText, 7), -1.5f))
                / 2f
            - mainTextFont.getStringWidth(charAt(theRockText, 5)) / 2f;
    float gotohTextYArg = theRockFinalPosY + mainTextFont.getHeight() * 1.25f;

    float reclusiveTextXArg = theRockFinalPosX + mainTextFont.getStringWidth(prefix(theRockText, 7), -1.5f);
    float reclusiveTextYArg = theRockFinalPosY - mainTextFont.getHeight() / 4.f;

    float gotohContentWidth =
        roundBoxFont.getStringWidth(gotohBoxText) + (-0.35f * gotohBoxText.length());
    float gotohBoxRectX = gotohTextXArg - gotohContentWidth * (1.3f - 1) / 2.f;
    float gotohBoxRectY = gotohTextYArg - roundBoxFont.getHeight() * (1.4f - 1) / 2.f;
    float gotohBoxRectWidth = gotohContentWidth * 1.3f;
    float gotohBoxRectHeight = roundBoxFont.getHeight() * 1.4f;

    float reclusiveContentWidth =
        textBoxFont.getStringWidth(reclusiveBoxText) + (-0.35f * reclusiveBoxText.length());
    float reclusiveBoxRectX = reclusiveTextXArg - reclusiveContentWidth * (1.1f - 1) / 2.f;
    float reclusiveBoxRectY = reclusiveTextYArg - textBoxFont.getHeight() * (1.8f - 1) / 2.f;
    float reclusiveBoxRectWidth = reclusiveContentWidth * 1.1f;
    float reclusiveBoxRectHeight = textBoxFont.getHeight() * 1.8f;

    float groupUntransformedMinX =
        Math.min(
            bocchiTextFinalX,
            Math.min(theRockFinalPosX, Math.min(gotohBoxRectX, reclusiveBoxRectX)));
    float groupUntransformedMinY =
        Math.min(
            bocchiTextFinalY + mainTextFont.getFont().getMetrics().getAscent(),
            Math.min(
                theRockFinalPosY + mainTextFont.getFont().getMetrics().getAscent(),
                Math.min(gotohBoxRectY, reclusiveBoxRectY)));

    float groupUntransformedMaxX =
        Math.max(
            bocchiTextFinalX + bocchiTextWidth,
            Math.max(
                theRockFinalPosX + theRockTextWidth,
                Math.max(
                    gotohBoxRectX + gotohBoxRectWidth, reclusiveBoxRectX + reclusiveBoxRectWidth)));
    float groupUntransformedMaxY =
        Math.max(
            bocchiTextFinalY + mainTextFont.getFont().getMetrics().getDescent(),
            Math.max(
                theRockFinalPosY + mainTextFont.getFont().getMetrics().getDescent(),
                Math.max(
                    gotohBoxRectY + gotohBoxRectHeight,
                    reclusiveBoxRectY + reclusiveBoxRectHeight)));

    float groupFinalWidth = groupUntransformedMaxX - groupUntransformedMinX;
    float groupFinalHeight = groupUntransformedMaxY - groupUntransformedMinY;

    float currentAnimValue = mainTextBlockAnim.getCurrentValue();

    Point groupStartPos =
        new Point(groupUntransformedMinX, groupUntransformedMinY + animOffset * 2);

    float groupCurrentX = Mth.lerp(currentAnimValue, groupStartPos.getX(), groupUntransformedMinX);
    float groupCurrentY = Mth.lerp(currentAnimValue, groupStartPos.getY(), groupUntransformedMinY);
    int groupCurrentAlpha = Math.round(currentAnimValue * 255.f);

    canvas.save();

    canvas.translate(
        groupCurrentX - groupUntransformedMinX, groupCurrentY - groupUntransformedMinY);

    canvas.saveLayerAlpha(null, groupCurrentAlpha);

    float scalePivotX = groupUntransformedMinX + groupFinalWidth / 2.0f;
    float scalePivotY = groupUntransformedMinY + groupFinalHeight / 2.0f;
    float currentScale = Mth.lerp(currentAnimValue, 0.85f, 1.f);

    canvas.translate(scalePivotX, scalePivotY);
    canvas.scale(currentScale, currentScale);
    canvas.translate(-scalePivotX, -scalePivotY);

    mainTextFont.drawString(bocchiText, bocchiTextFinalX, bocchiTextFinalY, 0XFFFFFFFF, -1.5f);
    mainTextFont.drawString(theRockText, theRockFinalPosX, theRockFinalPosY, 0XFFFFFFFF, -1.5f);
    drawTextBox(
        canvas,
        roundBoxFont,
        gotohBoxText,
        gotohTextXArg,
        gotohTextYArg,
        0XFFFFFFFF,
        0xFF000000,
        -0.35f,
        1.3f,
        1.4f,
        4.f);
    drawTextBox(
        canvas,
        textBoxFont,
        reclusiveBoxText,
        reclusiveTextXArg,
        reclusiveTextYArg,
        0XFFFFFFFF,
        0xFF000000,
        -0.35f,
        1.1f,
        1.8f,
        0);

    canvas.restore();
    canvas.restore();
  }

  /** 截断到前 n 个字符 (自定义文案比默认短时保证不越界) */
  private static String prefix(String s, int n) {
    return s.substring(0, Math.min(n, s.length()));
  }

  /** 取第 i 个字符; 空串返回空串 */
  private static String charAt(String s, int i) {
    return s.isEmpty() ? "" : String.valueOf(s.charAt(Math.min(i, s.length() - 1)));
  }
}
