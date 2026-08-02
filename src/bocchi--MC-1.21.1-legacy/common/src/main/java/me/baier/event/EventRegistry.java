package me.baier.event;

import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class EventRegistry<T> {

  private final EventMonitor monitor;
  private final int priority;
  private final Consumer<T> consumer;
  private final boolean ignoreCondition;

  public EventRegistry(
      EventMonitor monitor, int priority, Consumer<T> handler, boolean ignoreCondition) {
    this.monitor = monitor;
    this.priority = priority;
    this.consumer = handler;
    this.ignoreCondition = ignoreCondition;
  }
}
