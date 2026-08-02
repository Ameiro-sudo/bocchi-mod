package me.baier.client.ui.theme;

import me.baier.client.ui.mainmenu.poulsen.MainMenuScreen;
import net.minecraft.client.gui.screens.Screen;

/** poulsen 主题 (后藤独). */
public class PoulsenTheme implements Theme {

  private MainMenuScreen screen;

  @Override
  public String id() {
    return "poulsen";
  }

  @Override
  public String name() {
    return "Poulsen";
  }

  @Override
  public Screen getMainMenuScreen() {
    if (screen == null) {
      screen = MainMenuScreen.INSTANCE != null ? MainMenuScreen.INSTANCE : new MainMenuScreen();
    }
    return screen;
  }

  @Override
  public void beginFadeIn(Screen menu) {
    ((MainMenuScreen) menu).onBeginFadeIn();
  }

  @Override
  public void renderBackground() {
    if (screen != null) {
      screen.render(true);
    }
  }
}
