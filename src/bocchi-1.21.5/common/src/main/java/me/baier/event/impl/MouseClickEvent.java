package me.baier.event.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.baier.event.Event;

@Builder
public class MouseClickEvent extends Event {
  @Getter @Setter private int button;
  /** GLFW 动作: GLFW_PRESS=1 / GLFW_RELEASE=0. */
  @Getter @Setter private int action;
  @Getter @Setter private double posX, posY;
}
