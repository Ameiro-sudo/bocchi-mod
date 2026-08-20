package me.baier.event.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.baier.event.CancellableEvent;

@Builder
@Getter
@Setter
public class MotionUpdateEvent extends CancellableEvent {
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;
    private boolean horizontalCollision;
}
