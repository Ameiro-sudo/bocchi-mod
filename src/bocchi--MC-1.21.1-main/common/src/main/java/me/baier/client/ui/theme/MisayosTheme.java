package me.baier.client.ui.theme;

import me.baier.client.ui.mainmenu.misayos.MainMenuMisayosScreen;
import net.minecraft.client.gui.screens.Screen;

/** 默认主题: misayos (喜多郁代). */
public class MisayosTheme implements Theme {

  private MainMenuMisayosScreen screen;

  @Override
  public String id() {
    return "misayos";
  }

  @Override
  public String name() {
    return "Misayos";
  }

  @Override
  public Screen getMainMenuScreen() {
    if (screen == null) {
      screen = MainMenuMisayosScreen.INSTANCE != null ? MainMenuMisayosScreen.INSTANCE : new MainMenuMisayosScreen();
    }
    return screen;
  }

  @Override
  public void beginFadeIn(Screen menu) {
    if (menu instanceof MainMenuMisayosScreen ms) {
      ms.onBeginFadeIn();
    }
  }

  @Override
  public void renderBackground() {
    if (screen != null) {
      screen.render(true);
    }
  }
}
