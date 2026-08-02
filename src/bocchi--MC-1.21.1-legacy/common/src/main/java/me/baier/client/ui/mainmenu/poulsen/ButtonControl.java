package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.types.Point;

import me.baier.animation.BezierAnimation;

import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;

import me.baier.client.ui.mainmenu.component.interfaces.IControl;

import me.baier.client.ui.model.MainMenuPoulsenFrameContext;

import me.baier.graphics.SkiaContext;

import me.baier.graphics.font.FontSet;

public class ButtonControl extends AbstractBaseComponent<MainMenuPoulsenFrameContext>
    implements IControl {

  private final OnPress onPress;

  private final OnLayOut onLayOut;

  private final String displayString;

  private float width, height;
  private float posX, posY;

  public ButtonControl(String display, OnPress onPress, OnLayOut onLayOut) {

    this.onPress = onPress;

    this.displayString = display;

    this.onLayOut = onLayOut;

    var pos = onLayOut.onLayOut(this, new MainMenuPoulsenFrameContext());

    posX = pos.getX();

    posY = pos.getY();
  }

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuPoulsenFrameContext frame, BezierAnimation<Float> lastAnimation) {

    return lastAnimation;
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {

    var pos = onLayOut.onLayOut(this, frame);

    posX = pos.getX();

    posY = pos.getY();

    float bgFontSize = frame.getBgFontSize();

    var buttonFont = FontSet.RADIKAL_REGULAR.getFont(bgFontSize * 0.221f * 0.3f);

    buttonFont.drawString(displayString, posX, posY - buttonFont.getHalfHeight() / 2, 0xFF000000);
  }

  public interface OnPress {

    void onPress(ButtonControl button);
  }

  public interface OnLayOut {

    Point onLayOut(ButtonControl button, MainMenuPoulsenFrameContext frame);
  }
}
