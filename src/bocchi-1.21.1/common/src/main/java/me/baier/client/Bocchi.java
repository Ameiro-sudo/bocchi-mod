package me.baier.client;

import lombok.Getter;
import me.baier.client.cfg.Cfgs;
import me.baier.client.mod.Mods;
import me.baier.client.ui.theme.ThemeManager;
import me.baier.event.EventMonitor;
import me.baier.manager.EventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Bocchi implements EventMonitor {
  @Getter private final Logger logger = LoggerFactory.getLogger(Bocchi.class);

  static {
    INSTANCE = new Bocchi();
  }

  public static volatile Bocchi INSTANCE;
  @Getter private final EventManager eventManager;

  @Getter private Mods modManager;

  public Bocchi() {
    this.eventManager = new EventManager();
  }

  public void start() {
    // TODO : implement a config system to handle these shits.
    modManager = Mods.INSTANCE.initialize();
    Cfgs.INSTANCE.initialize();
  }

  public void drawBackGround() {
    ThemeManager.get().renderBackground();
  }

  @Override
  public EventManager getAssociatedEventManager() {
    return this.eventManager;
  }

  public Path getBase() {
    return Paths.get(System.getProperty("user.home")).resolve(".bocchi");
  }
}
