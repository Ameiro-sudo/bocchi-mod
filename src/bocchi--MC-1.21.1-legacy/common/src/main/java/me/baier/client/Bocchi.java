package me.baier.client;

import lombok.Getter;
import me.baier.client.cfg.Cfgs;
import me.baier.client.mod.Mods;
import me.baier.client.ui.common.components.BackgroundRenderer;
import me.baier.client.ui.mainmenu.misayos.MainMenuMisayosScreen;
import me.baier.event.EventMonitor;
import me.baier.graphics.media.FrameGrabber;
import me.baier.graphics.media.SKVideoDecoder;
import me.baier.manager.EventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Bocchi implements EventMonitor {
  @Getter private final Logger logger = LoggerFactory.getLogger(Bocchi.class);

  static {
    INSTANCE = new Bocchi();
  }

  public static volatile Bocchi INSTANCE;
  @Getter private final EventManager eventManager;
  private final SKVideoDecoder decoder;

  @Getter private Mods modManager;

  public Bocchi() {

    this.eventManager = new EventManager();
    this.decoder = new SKVideoDecoder();
  }

  public void exit() {
    try {
      decoder.close();
    } catch (FrameGrabber.Exception e) {
      // ignore
    }
  }

  public void start() {
    // TODO : implement a config system to handle these shits.
    try {
      Path targetDir = getBase();
      Files.createDirectories(targetDir);
      Path targetFilePath = targetDir.resolve("bg.mp4");
      try (InputStream inputStream =
          getClass().getResourceAsStream("/assets/minecraft/client/media/default.mp4")) {
        if (inputStream == null) {
          // handle error
          return;
        }
        Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
      }
      decoder.loadUrl(targetFilePath.toString());
    } catch (IOException e) {
      // ignore
    }
    modManager = Mods.INSTANCE.initialize();
    Cfgs.INSTANCE.initialize();
  }

  public void drawBackGround() {
    if (MainMenuMisayosScreen.INSTANCE != null) {
      MainMenuMisayosScreen.INSTANCE.render(true);
      return;
    }
    BackgroundRenderer.renderBackground(decoder);
  }

  @Override
  public EventManager getAssociatedEventManager() {
    return this.eventManager;
  }

  public Path getBase() {
    return Paths.get(System.getProperty("user.home")).resolve(".bocchi");
  }
}
