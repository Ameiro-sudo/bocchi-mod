package me.baier.event;

import me.baier.client.Bocchi;

public class Event {
    public final void forward() {
        Bocchi.INSTANCE.getEventManager().forward(this);
    }
}
