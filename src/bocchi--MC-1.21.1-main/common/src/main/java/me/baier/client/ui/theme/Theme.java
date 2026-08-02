package me.baier.client.ui.theme;

import net.minecraft.client.gui.screens.Screen;

/**
 * 主菜单主题. 每个主题是独立的 UI 开发区 (如 misayos / poulsen 包),
 * 通过 {@link ThemeManager} 注册, 由 design.json 的 "menu.theme" 键切换.
 */
public interface Theme {

  /** 主题 id, 对应 design.json "menu.theme" 的取值. */
  String id();

  /** 显示名 (日志/调试用). */
  String name();

  /** 主菜单单例: 已创建返回缓存实例, 否则创建并缓存. */
  Screen getMainMenuScreen();

  /** 主菜单入场动画开始 (TAP TO START 后调用). */
  default void beginFadeIn(Screen menu) {}

  /** 游戏内背景渲染 (HUD / 其他 Screen 的 panorama 背景). */
  void renderBackground();
}
