package me.baier.skui.baking;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.Rect;
import me.baier.graphics.SkiaEnvironment;

public class SkShadowBaking implements AutoCloseable {

  private float inflate = 0.0F;
  private Image shadow = null;

  public void bake(float width, float height, float sigma, int color) {
    if (shadow != null) {
      shadow.close();
    }

    float inflate = sigma * 2;

    Rect bounds = Rect.makeWH(width, height);
    Rect shadow = bounds.inflate(inflate);
    Surface surface =
        Surface.makeRaster(ImageInfo.makeA8((int) shadow.getWidth(), (int) shadow.getHeight()));

    MaskFilter filter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma);
    Canvas canvas = surface.getCanvas();
    Paint paint = new Paint().setColor(color).setMaskFilter(filter);

    canvas.translate(-shadow.getLeft(), -shadow.getTop());
    canvas.drawRect(bounds, paint);

    this.inflate = inflate;
    this.shadow = surface.makeImageSnapshot();

    filter.close();
    surface.close();
  }

  public void render(SkiaEnvironment env, float x, float y, Paint paint) {
    if (shadow == null) {
      throw new RuntimeException("Shadow is null");
    }
    Canvas canvas = env.getCanvas();
    canvas.drawImage(shadow, x - inflate, y - inflate, paint);
  }

  @Override
  public void close() throws Exception {
    if (shadow != null) {
      shadow.close();
    }
  }
}
