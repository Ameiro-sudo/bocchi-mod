package me.baier.graphics.util;

import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import me.baier.client.ClientInstance;
import me.baier.event.EventMonitor;
import me.baier.event.impl.ResizeEvent;

public class FramebufferFactory implements EventMonitor, ClientInstance {
  public static FramebufferFactory INSTANCE = new FramebufferFactory();
  public RenderTargetDescriptor factory;

  public FramebufferFactory() {
    createMonitor(
        ResizeEvent.class,
        resizeEvent -> {
          factory =
              new RenderTargetDescriptor(
                  mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, 0);
        });
  }
}
