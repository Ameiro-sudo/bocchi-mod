package me.baier.client.ui.splash;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import java.io.IOException;
import me.baier.graphics.SkiaCallback;
import me.baier.graphics.SkiaRenderEngine;
import net.minecraft.resources.ResourceLocation;

public class LoadingAnimateRenderer {
  private int currentFrame = 0;
  private final int TOTAL_FRAMES = 20;
  private final ResourceLocation texture;
  private long lastFrameTime = 0;
  private final long frameInterval;

  public LoadingAnimateRenderer(ResourceLocation texture, int targetFPS) {
    this.texture = texture;
    this.frameInterval = 1000 / targetFPS;
    this.lastFrameTime = System.currentTimeMillis();
  }

  public void computeFrame() {
    long currentTime = System.currentTimeMillis();
    long elapsed = currentTime - lastFrameTime;

    if (elapsed >= frameInterval) {
      currentFrame++;
      if (currentFrame >= TOTAL_FRAMES) {
        currentFrame = 0;
      }
      lastFrameTime = currentTime;
    }
  }

  public void render(
      Canvas canvas, float x, float y, float w, float h, float alpha, SkiaCallback callback)
      throws IOException {

    SkiaRenderEngine.drawImage(
        canvas, texture, x, y, w, h, currentFrame * 90, 0, 90, 90, alpha, callback);
  }
}
