package me.baier.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ResPack {
  private ResPack() {}

  public static byte[] readBytes(ResourceLocation res, String classpathFallback) throws IOException {
    try (InputStream in = open(res, classpathFallback)) {
      return in.readAllBytes();
    }
  }

  public static InputStream open(ResourceLocation res, String classpathFallback) throws IOException {
    ResourceManager manager = getManager();
    if (manager != null) {
      Optional<Resource> optional = manager.getResource(res);
      if (optional.isPresent()) {
        return optional.get().open();
      }
    }
    InputStream in = ResPack.class.getResourceAsStream(classpathFallback);
    if (in == null) {
      throw new IOException("missing resource: " + res + " / " + classpathFallback);
    }
    return in;
  }

  private static ResourceManager getManager() {
    try {
      Minecraft mc = Minecraft.getInstance();
      return mc == null ? null : mc.getResourceManager();
    } catch (Throwable t) {
      return null;
    }
  }
}
