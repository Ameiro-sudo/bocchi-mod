package me.baier.event.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.baier.event.Event;

@Builder
public class MouseClickEvent extends Event {
  @Getter @Setter private int button;
  @Getter @Setter private double posX, posY;
}
