package me.baier.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Consumer;
import me.baier.event.Event;
import me.baier.event.EventMonitor;
import me.baier.event.EventRegistry;

public class EventManager {

  private final HashMap<Class<?>, ArrayList<EventRegistry<?>>> registry = new HashMap<>();

  public <T extends Event> void register(
      Class<? extends T> event, EventMonitor monitor, int priority, Consumer<T> handler) {
    if (!registry.containsKey(event)) {
      registry.put(event, new ArrayList<>());
    }

    ArrayList<EventRegistry<?>> handlers = registry.get(event);
    EventRegistry<T> eventRegistry = new EventRegistry<>(monitor, priority, handler, false);
    handlers.add(eventRegistry);
    handlers.sort(Comparator.comparingInt(EventRegistry::getPriority));
  }

  public void invalidateMonitor(EventMonitor monitor) {
    ArrayList<Class<?>> emptyEventClasses = new ArrayList<>();

    for (Class<?> eventClass : registry.keySet()) {
      ArrayList<EventRegistry<?>> handlers = registry.get(eventClass);

      handlers.removeIf(handler -> handler.getMonitor() == monitor);

      if (handlers.isEmpty()) {
        emptyEventClasses.add(eventClass);
      }
    }

    for (Class<?> eventClass : emptyEventClasses) {
      registry.remove(eventClass);
    }
  }

  public <T extends Event> T forward(T event) {
    ArrayList<EventRegistry<?>> handlers = registry.get(event.getClass());
    if (handlers == null) {
      return event;
    }

    ArrayList<EventRegistry<?>> handlersCopy = new ArrayList<>(handlers);
    for (EventRegistry<?> handler : handlersCopy) {
      boolean shouldExecute = handler.isIgnoreCondition() || handler.getMonitor().handle();
      if (shouldExecute) {
        @SuppressWarnings("unchecked")
        EventRegistry<T> typedHandler = (EventRegistry<T>) handler;
        typedHandler.getConsumer().accept(event);
      }
    }
    return event;
  }
}
