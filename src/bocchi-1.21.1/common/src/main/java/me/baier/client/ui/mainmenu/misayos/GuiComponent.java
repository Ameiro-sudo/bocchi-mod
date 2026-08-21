package me.baier.client.ui.mainmenu.misayos;

import lombok.Getter;
import me.baier.animation.BezierAnimation;

import me.baier.animation.BezierControlPoints;

import me.baier.client.ui.mainmenu.component.interfaces.IComponent;

import me.baier.client.ui.mainmenu.misayos.childs.ButtonChild;
import me.baier.client.ui.mainmenu.misayos.childs.ExpandArrowChild;

import me.baier.client.ui.mainmenu.misayos.childs.IconButtonChild;
import me.baier.client.ui.settings.SettingsScreen;
import me.baier.client.ui.model.MainMenuMisayosFrameContext;
import me.baier.design.Design;

import me.baier.graphics.SkiaContext;

import me.baier.graphics.SkiaEnvironment;

import me.baier.graphics.SkiaRenderEngine;

import me.baier.graphics.font.FontSet;
import me.baier.client.ui.theme.ThemeManager;
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
import java.util.Locale;

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
    // Cfgs 设置面板入口 (图标复用 option 齿轮, 不新增资产)
    var cfgsButton = new ButtonChild("option", "Cfgs");
    cfgsButton.setOnClick(
        () -> Minecraft.getInstance().setScreen(new SettingsScreen(MainMenuMisayosScreen.INSTANCE)));
    this.addChild(cfgsButton);
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
    // 主题按钮: 文案与目标动态取自 ThemeManager (新增主题无需改这里)
    var themeButton = new ButtonChild("theme", ThemeManager.nextId().toUpperCase(Locale.ROOT));
    themeButton.setOnClick(
        () -> {
          ThemeManager.toggle();
          Minecraft.getInstance().setScreen(ThemeManager.get().getMainMenuScreen());
        });
    this.addChild(themeButton);
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
    // 与 update() 同源: 基准窗口 870x519 (面板高 167.6), 所有元素同倍率缩放
    float scale = this.getHeight() / 167.6f;
    var buttonYOffset = 10.f * scale;
    var buttonWidth = 75.f * scale;
    float buttonHeight = 15.f * scale;
    float buttonStartY = 30.f * scale;
    float iconSize = 15.f * scale;
    float iconSpacing = 1.f * scale;
    float gap = 20.5f * scale;
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
    // 前 N-1 个文字按钮; 图标跟最后一个主按钮同行
    int mainCount = Math.max(0, buttons.size() - 1);
    for (int i = 0; i < mainCount; i++) {
      ButtonChild button = buttons.get(i);
      button.setX(5.f * scale + (this.getWidth() - 5.f * scale - buttonWidth) / 2);
      button.setY(buttonStartY + buttonYOffset);
      button.setWidth(buttonWidth);
      button.setHeight(buttonHeight);
      buttonYOffset += gap;
    }
    if (mainCount > 0) {
      ButtonChild anchor = buttons.get(mainCount - 1);
      var iconXOffset = iconSpacing;
      anchor.setWidth(buttonWidth - iconXOffset - iconSize * icons.size() - iconSpacing);
      for (IconButtonChild icon : icons) {
        icon.setX(anchor.getX() + anchor.getWidth() + iconXOffset);
        icon.setY(anchor.getY());
        icon.setHeight(iconSize);
        icon.setWidth(iconSize);
        iconXOffset += icon.getWidth() + iconSpacing;
      }
    }
    // 主题切换按钮独立放面板底部
    if (!buttons.isEmpty()) {
      ButtonChild theme = buttons.getLast();
      theme.setX(5.f * scale + (this.getWidth() - 5.f * scale - buttonWidth) / 2);
      theme.setY(this.getHeight() * 0.85f - buttonHeight - 5.5f * scale);
      theme.setWidth(buttonWidth);
      theme.setHeight(buttonHeight);
    }
  }

  @Override
  public void update(MainMenuMisayosFrameContext frame) {

    expandAnimation.update();
    if (fadeInAnimation == null) {
      // 兜底: 动画链未接线时按已完成状态补齐, 宁可面板直接可见也不让主菜单 NPE 崩游戏
      fadeInAnimation = BezierAnimation.createFloat(1.f, 1.f, entranceDuration, easeFunc);
    }
    fadeInAnimation.update();

    var lerp = expandAnimation.getCurrentValue();
    // 整个侧栏按 block3 等比例缩放 (基准窗口 870x519: block3=197.1, 面板 95x167.6)
    // 该窗口下 scale=1 即原版样子; 全屏时面板/按钮/间距/图标全部同倍率放大
    float scale = frame.getBlock3Size() / 197.1f;
    this.setHeight(167.6f * scale);
    this.setWidth(95.f * scale);

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

    if (isHovered() || (isChildHovered() && isValidMousePos())) {
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
    // 面板文案可配置 (design.json texts.*)
    String panelTitle = Design.value("texts.pTitle", "BOCCHI");
    String versionText = Design.value("texts.pVer", "1.0");
    String branchText = Design.value("texts.pBranch", "\"ALPHA\"");
    String copyright1 = Design.value("texts.pCopy1", "Bocchi Client    Version - 1.0");
    String copyright2 = Design.value("texts.pCopy2", "@COPYRIGHT MISAYO");
    var bocchiWidth = titleFont.getStringWidth(panelTitle, -0.1f);
    var titlePosX = this.getAbsoluteX() + bocchiWidth / 2;
    var titlePosY = this.getAbsoluteY() + titleFont.getHeight() + 3.f;
    var linePaint = env.borrowPaint().setStroke(true).setStrokeWidth(0.2f).setColor(0X66353535);
    titleFont.drawString(panelTitle, titlePosX, titlePosY, 0XFF353535, -0.1f);
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
        versionText,
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
    branchFont.drawString(branchText, branchTextPosX, branchTextPosY, 0XFF353535, -0.1f);
    float lineY = this.getAbsoluteY() + this.getHeight() * 0.85f;
    canvas.drawLine(
        this.getAbsoluteX() + borderWidth + 3.f,
        lineY,
        this.getAbsoluteX() + this.getWidth() - 3.f,
        lineY,
        linePaint);
    env.recyclePaint(linePaint);
    var copyRightFont = FontSet.SH_NORMAL.getFont(7);
    var branchVersionWidth = copyRightFont.getStringWidth(copyright1);
    copyRightFont.drawString(
        copyright1,
        this.getAbsoluteX() + 5.f + (this.getWidth() - 5.f - branchVersionWidth) / 2,
        this.getAbsoluteY() + this.getHeight() - copyRightFont.getHeight() * 3 - 5.f,
        0xFF353535);
    var copyRightWidth = copyRightFont.getStringWidth(copyright2);
    copyRightFont.drawString(
        copyright2,
        this.getAbsoluteX() + 5.f + (this.getWidth() - 5.f - copyRightWidth) / 2,
        this.getAbsoluteY() + this.getHeight() - copyRightFont.getHeight() * 2 - 2.5f,
        0xFF353535);
  }
}
