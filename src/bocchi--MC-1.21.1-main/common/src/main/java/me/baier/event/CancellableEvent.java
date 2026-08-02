package me.baier.event;

import lombok.Getter;

@Getter
public class CancellableEvent extends Event {

  private boolean cancelled = false;

  public CancellableEvent() {}

  public void cancel() {
    cancelled = true;
  }
}
