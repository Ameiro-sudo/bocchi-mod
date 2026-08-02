package me.baier.client.ui.mainmenu.misayos;

import lombok.Getter;
import me.baier.animation.BezierAnimation;

import me.baier.animation.BezierControlPoints;

import me.baier.client.ui.mainmenu.component.interfaces.IComponent;

import me.baier.client.ui.mainmenu.misayos.childs.ButtonChild;
import me.baier.client.ui.mainmenu.misayos.childs.ExpandArrowChild;

import me.baier.client.ui.mainmenu.misayos.childs.IconButtonChild;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;

import me.baier.graphics.SkiaContext;

import me.baier.graphics.SkiaEnvironment;

import me.baier.graphics.SkiaRenderEngine;

import me.baier.graphics.font.FontSet;
import me.baier.skui.SkComponent;

import me.baier.utils.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static me.baier.utils.RenderUtils.drawTextStrokeBox;

public class GuiComponent extends SkComponent implements IComponent<MainMenuMisayosFrameContext> {

  private final ExpandArrowChild expandChild;
  private BezierAnimation<Float> fadeInAnimation;

  @Getter
  private BezierAnimation<Float> expandAnimation =
      BezierAnimation.createFloat(0.f, 1.f, 500, BezierControlPoints.CIRC_OUT);

  @Getter private boolean expandState = false;

  public GuiComponent() {

    expandChild = new ExpandArrowChild();

    this.addChild(expandChild);
    var singleButton = new ButtonChild("single", "SinglePlayer");
    singleButton.setOnClick(
        () -> Minecraft.getInstance().setScreen(new SelectWorldScreen(MainMenuMisayosScreen.INSTANCE)));
    this.addChild(singleButton);
    var multiButton = new ButtonChild("multi", "MultiPlayer");
    multiButton.setOnClick(
        () ->
            Minecraft.getInstance()
                .setScreen(
                    new JoinMultiplayerScreen(MainMenuMisayosScreen.INSTANCE)));
    this.addChild(multiButton);
    var optionButton = new ButtonChild("option", "Options");
    optionButton.setOnClick(
        () ->
            Minecraft.getInstance()
                .setScreen(
                    new OptionsScreen(
                        MainMenuMisayosScreen.INSTANCE,
                        Minecraft.getInstance().options)));
    this.addChild(optionButton);
    var langIcon = new IconButtonChild("lang");
    langIcon.setOnClick(
        () ->
            Minecraft.getInstance()
                .setScreen(
                    new LanguageSelectScreen(
                        MainMenuMisayosScreen.INSTANCE,
                        Minecraft.getInstance().options,
                        Minecraft.getInstance().getLanguageManager())));
    this.addChild(langIcon);
    var quitIcon = new IconButtonChild("quit");
    quitIcon.setOnClick(() -> Minecraft.getInstance().stop());
    this.addChild(quitIcon);
    this.setLayout(this::onLayout);
  }

  @Override
  protected void onRenderChildren(SkiaEnvironment env, int mouseX, int mouseY) {
    for (SkComponent child : getChildren()) {
      if (!child.isVisibleInHierarchy()) {
        continue;
      }
      child.render(env, mouseX, mouseY);
    }
  }

  @Override
  public BezierAnimation<Float> initAnimations(
      MainMenuMisayosFrameContext frame, BezierAnimation<Float> lastAnimation) {
    fadeInAnimation = BezierAnimation.createFloat(0.f, 1.f, entranceDuration, easeFunc);
    lastAnimation.then(fadeInAnimation, 0.05f);
    return fadeInAnimation;
  }

  private void onLayout(SkComponent parent) {
    List<SkComponent> children = this.getChildren();
    var buttonYOffset = 10.f;
    var buttonWidth = 75.f;
    List<ButtonChild> buttons = new ArrayList<>();
    List<IconButtonChild> icons = new ArrayList<>();
    for (SkComponent child : children) {
      if (child instanceof ButtonChild) {
        if (child instanceof IconButtonChild) {
          icons.add((IconButtonChild) child);
        } else {
          buttons.add((ButtonChild) child);
        }
      }
    }
    for (ButtonChild button : buttons) {
      button.setX(5.f + (this.getWidth() - 5.f - buttonWidth) / 2);
      button.setY(30 + buttonYOffset);
      button.setWidth(buttonWidth);
      button.setHeight(15);
      buttonYOffset += 20.5f;
    }
    var lastButton = buttons.getLast();
    var dOffsetX = 0.f;
    lastButton.setX(lastButton.getX() + dOffsetX);
    var iconXOffset = 1.f;
    var spacing = 1.f;
    // lastButtonWidth  + iconXOffset + 15 + spacing + 15 = buttonWidth - dOffsetY*2
    lastButton.setWidth(
        buttonWidth - dOffsetX * icons.size() - iconXOffset - 15 * icons.size() - spacing);
    for (IconButtonChild icon : icons) {
      icon.setX(lastButton.getX() + lastButton.getWidth() + iconXOffset);
      icon.setY(lastButton.getY());
      icon.setHeight(15);
      icon.setWidth(15);
      iconXOffset += icon.getWidth() + spacing;
    }
  }

  @Override
  public void update(MainMenuMisayosFrameContext frame) {

    expandAnimation.update();
    fadeInAnimation.update();

    var lerp = expandAnimation.getCurrentValue();
    this.setHeight(frame.getBlock3Size() * 0.85f);

    this.setWidth(95);

    this.setX(
        Mth.lerp(
            fadeInAnimation.getCurrentValue(),
            frame.getScaledWidth() + 10.f,
            Mth.lerp(
                lerp,
                frame.getScaledWidth() - this.getWidth() / 10,
                frame.getScaledWidth() - this.getWidth())));

    this.setY((frame.getScaledHeight() - this.getHeight()) / 2);

    expandChild.setY(this.getHeight() / 2);

    if (isHovered() || isChildHovered() && isValidMousePos()) {
      if (!expandState) {
        // was hovered
        expandState = true;
        expandAnimation = BezierAnimation.createFloat(0.f, 1.f, 300, BezierControlPoints.CUBIC_OUT);
        expandAnimation.start();
      }

    } else {

      // was hovered
      if (expandState) {
        // reverse
        expandState = false;
        expandAnimation = BezierAnimation.createFloat(1.f, 0.f, 600, BezierControlPoints.CUBIC_OUT);
        expandAnimation.start();
      }
    }
  }

  @Override
  public void render(SkiaContext ctx, MainMenuMisayosFrameContext frame) {
    SkiaRenderEngine.drawRect(
        0,
        0,
        frame.getScaledWidth(),
        frame.getScaledHeight(),
        ColorUtil.replaceAlpha(
            0XFF000000, Math.round(Mth.lerp(expandAnimation.getCurrentValue(), 0.f, 125))));
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    var canvas = env.getCanvas();
    var borderWidth = 5.f;

    SkiaRenderEngine.drawRect(
        this.getAbsoluteX(), this.getAbsoluteY(), borderWidth, this.getHeight(), 0XFFFFFFFF);

    SkiaRenderEngine.drawRect(
        this.getAbsoluteX() + borderWidth,
        this.getAbsoluteY(),
        this.getWidth() - borderWidth,
        this.getHeight(),
        0xFFF5F5F5);
    var titleFont = FontSet.SH_HEAVY.getFont(21);
    var bocchiWidth = titleFont.getStringWidth("BOCCHI", -0.1f);
    var titlePosX = this.getAbsoluteX() + bocchiWidth / 2;
    var titlePosY = this.getAbsoluteY() + titleFont.getHeight() + 3.f;
    var linePaint = env.borrowPaint().setStroke(true).setStrokeWidth(0.2f).setColor(0X66353535);
    titleFont.drawString("BOCCHI", titlePosX, titlePosY, 0XFF353535, -0.1f);
    canvas.drawLine(
        titlePosX - 2.5f,
        titlePosY + titleFont.getHeight() + 2.5f,
        titlePosX + bocchiWidth + 2.5f,
        titlePosY + titleFont.getHeight() + 2.5f,
        linePaint);
    var versionFont = FontSet.SH_REGULAR.getFont(10);
    drawTextStrokeBox(
        canvas,
        versionFont,
        "1.0",
        titlePosX + bocchiWidth + 5.f,
        titlePosY + titleFont.getHalfHeight() / 2 + versionFont.getHalfHeight() / 2,
        0xFF353535,
        0xFF353535,
        0.f,
        2f,
        1.2f,
        3,
        0.5f);
    var branchFont = FontSet.SH_HEAVY.getFont(11);
    var branchTextPosX = titlePosX;
    var branchTextPosY =
        titlePosY + titleFont.getHeight() + branchFont.getHalfHeight() / 2 + 1.f + 3.f;
    branchFont.drawString("\"ALPHA\"", branchTextPosX, branchTextPosY, 0XFF353535, -0.1f);
    float lineY = this.getAbsoluteY() + this.getHeight() * 0.85f;
    canvas.drawLine(
        this.getAbsoluteX() + borderWidth + 3.f,
        lineY,
        this.getAbsoluteX() + this.getWidth() - 3.f,
        lineY,
        linePaint);
    env.recyclePaint(linePaint);
    var copyRightFont = FontSet.SH_NORMAL.getFont(7);
    var branchVersionWidth = copyRightFont.getStringWidth("Bocchi Client    Version - 1.0");
    copyRightFont.drawString(
        "Bocchi Client    Version - 1.0",
        this.getAbsoluteX() + 5.f + (this.getWidth() - 5.f - branchVersionWidth) / 2,
        this.getAbsoluteY() + this.getHeight() - copyRightFont.getHeight() * 3 - 5.f,
        0xFF353535);
    var copyRightWidth = copyRightFont.getStringWidth("@COPYRIGHT MISAYO");
    copyRightFont.drawString(
        "@COPYRIGHT MISAYO",
        this.getAbsoluteX() + 5.f + (this.getWidth() - 5.f - copyRightWidth) / 2,
        this.getAbsoluteY() + this.getHeight() - copyRightFont.getHeight() * 2 - 2.5f,
        0xFF353535);
  }
}
