package me.baier.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

  /** 返回该资源在资源包栈中的全部版本 (低优先级在前, 高优先级在后), 栈为空时返回空列表. */
  public static List<Resource> getAll(ResourceLocation res) {
    ResourceManager manager = getManager();
    return manager != null ? manager.getResourceStack(res) : List.of();
  }

  public static InputStream open(ResourceLocation res, String classpathFallback) throws IOException {
    ResourceManager manager = getManager();
    if (manager != null && res != null) {
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
