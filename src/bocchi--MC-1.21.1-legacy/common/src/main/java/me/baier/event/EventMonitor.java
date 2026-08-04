package me.baier.event;

import java.util.function.Consumer;
import me.baier.client.Bocchi;
import me.baier.manager.EventManager;

public interface EventMonitor {

  default boolean handle() {
    return true;
  }

  default <T extends Event> void createMonitor(Class<? extends T> event, Consumer<T> consumer) {
    createMonitor(event, 0, consumer);
  }

  default EventManager getAssociatedEventManager() {
    return Bocchi.INSTANCE.getEventManager();
  }

  default <T extends Event> void createMonitor(
      Class<? extends T> event, int priority, Consumer<T> consumer) {
    EventManager manager = getAssociatedEventManager();
    manager.register(event, this, priority, consumer);
  }

  default void invalidateMonitor() {
    EventManager manager = getAssociatedEventManager();
    manager.invalidateMonitor(this);
  }
}
