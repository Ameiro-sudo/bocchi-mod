package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;
import me.baier.animation.BezierAnimation;
import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;
import me.baier.client.ui.model.MainMenuPoulsenFrameContext;
import me.baier.design.Design;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.font.FontSet;
import me.baier.utils.ColorUtil;
import me.baier.utils.ScreenUtils;
import net.minecraft.util.Mth;

import java.util.List;

public class BigLastNameComponent extends AbstractBaseComponent<MainMenuPoulsenFrameContext> {
  private BezierAnimation<Float> gotoText1Anim;
  private BezierAnimation<Float> gotoText2Anim;

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {
    gotoText1Anim = BezierAnimation.createFloat(0f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(gotoText1Anim, 0.1f);
    gotoText2Anim = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(gotoText2Anim, 0.05f);
    animations.addAll(List.of(gotoText1Anim, gotoText2Anim));
    return gotoText2Anim;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {
    var screenHeight = frame.getScaledHeight();
    float screenWidth = frame.getScaledWidth();

    float rect1PosX = frame.getRect1PosX();
    float bgFontSize = screenHeight * 0.205f / 0.305f;

    var sharedFontForGoto = FontSet.RADIKAL_BLACK.getFont(bgFontSize);
    String gotoText1 = Design.value("texts.pGoto1", "GOTO");
    String gotoText2 = Design.value("texts.pGoto2", "GOTO");
    float gotoTextWidth = Math.max(sharedFontForGoto.getStringWidth(gotoText1), sharedFontForGoto.getStringWidth(gotoText2));
    float gotoTextHeight = sharedFontForGoto.getHeight();
    float spacingForGoto = screenHeight * 0.045f;
    float finalGoto1X = rect1PosX - sharedFontForGoto.getHalfHeight() / 15f;
    float finalGoto1Y = sharedFontForGoto.getHalfHeight() / 2f + spacingForGoto;
    Point goto1StartPos =
        ScreenUtils.calculateStartPosition(
            finalGoto1X,
            finalGoto1Y - gotoTextHeight,
            gotoTextWidth,
            gotoTextHeight,
            screenWidth,
            screenHeight,
            animOffset);

    float finalGoto2X = finalGoto1X;
    float finalGoto2Y = finalGoto1Y + sharedFontForGoto.getHeight() * 1.25f + spacingForGoto;
    Point goto2StartPos =
        ScreenUtils.calculateStartPosition(
            finalGoto2X,
            finalGoto2Y - gotoTextHeight,
            gotoTextWidth,
            gotoTextHeight,
            screenWidth,
            screenHeight,
            animOffset);

    if (gotoText1Anim != null)
      sharedFontForGoto.drawString(
          gotoText1,
          Mth.lerp(gotoText1Anim.getCurrentValue(), goto1StartPos.getX(), finalGoto1X),
          Mth.lerp(
              gotoText1Anim.getCurrentValue(), goto1StartPos.getY() + gotoTextHeight, finalGoto1Y),
          ColorUtil.replaceAlpha(0XFFFF86C0, Math.round(gotoText1Anim.getCurrentValue() * 255.f)));
    if (gotoText2Anim != null)
      sharedFontForGoto.drawString(
          gotoText2,
          Mth.lerp(gotoText2Anim.getCurrentValue(), goto2StartPos.getX(), finalGoto1X),
          Mth.lerp(
              gotoText2Anim.getCurrentValue(), goto2StartPos.getY() + gotoTextHeight, finalGoto2Y),
          ColorUtil.replaceAlpha(0XFFE95A9F, Math.round(gotoText2Anim.getCurrentValue() * 255.f)));
  }
}
