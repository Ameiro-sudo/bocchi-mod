package me.baier.client.ui.mainmenu.poulsen;

import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;

import me.baier.animation.BezierAnimation;

import me.baier.client.ui.mainmenu.component.api.AbstractBaseComponent;

import me.baier.client.ui.mainmenu.component.interfaces.IControl;

import me.baier.client.ui.model.MainMenuPoulsenFrameContext;

import me.baier.graphics.SkiaContext;

import me.baier.graphics.font.FontSet;

import me.baier.graphics.font.SkiaFontRenderer;

public class ButtonControl extends AbstractBaseComponent<MainMenuPoulsenFrameContext>
    implements IControl {

  private static final float HITBOX_PAD_X = 10.f;
  private static final float HITBOX_PAD_TOP = 6.f;
  private static final float HITBOX_PAD_BOTTOM = 8.f;

  private final OnPress onPress;

  private final OnLayOut onLayOut;

  private final String displayString;

  private float posX, posY;
  private float hitboxW, hitboxH;
  private boolean hovered;

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

  public void setHovered(boolean hovered) {
    this.hovered = hovered;
  }

  public boolean contains(int mouseX, int mouseY) {
    return mouseX >= posX - HITBOX_PAD_X
        && mouseX <= posX + hitboxW + HITBOX_PAD_X
        && mouseY >= posY - HITBOX_PAD_TOP
        && mouseY <= posY + HITBOX_PAD_BOTTOM;
  }

  public void press() {
    if (onPress != null) {
      onPress.onPress(this);
    }
  }

  private SkiaFontRenderer getButtonFont(MainMenuPoulsenFrameContext frame) {
    return FontSet.RADIKAL_REGULAR.getFont(frame.getBgFontSize() * 0.221f * 0.3f);
  }

  @Override
  public void render(SkiaContext ctx, MainMenuPoulsenFrameContext frame) {

    var pos = onLayOut.onLayOut(this, frame);

    posX = pos.getX();

    posY = pos.getY();

    var buttonFont = getButtonFont(frame);

    hitboxW = buttonFont.getStringWidth(displayString);
    hitboxH = buttonFont.getHeight();

    int color = hovered ? 0xFFE95A9F : 0xFF000000;

    buttonFont.drawString(
        displayString, posX, posY - buttonFont.getHalfHeight() / 2, color);

    if (hovered) {
      var canvas = ctx.canvas();
      try (Paint paint = new Paint()) {
        paint.setAntiAlias(true);
        paint.setColor(0x30E95A9F);
        canvas.drawRRect(
            RRect.makeXYWH(
                posX - HITBOX_PAD_X,
                posY - buttonFont.getHalfHeight() / 2 - HITBOX_PAD_TOP,
                hitboxW + HITBOX_PAD_X * 2,
                hitboxH + HITBOX_PAD_TOP + HITBOX_PAD_BOTTOM,
                8.f),
            paint);
      }
    }
  }

  public interface OnPress {

    void onPress(ButtonControl button);
  }

  public interface OnLayOut {

    Point onLayOut(ButtonControl button, MainMenuPoulsenFrameContext frame);
  }
}
