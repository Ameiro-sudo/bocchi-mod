package me.baier.graphics;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import java.util.ArrayList;
import java.util.Stack;
import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class SkiaEnvironment implements AutoCloseable {

  private final Canvas canvas;

  @Getter private static SkiaEnvironment current = null;

  private final ArrayList<Paint> paintPool = new ArrayList<>();
  private final Stack<Float> alphaStack = new Stack<>();
  private final Stack<BlendMode> blendStack = new Stack<>();

  @Getter private final int width;
  @Getter private final int height;

  public SkiaEnvironment(SkiaContext context) {
    this.canvas = context.canvas();
    this.alphaStack.push(1.0f);
    this.blendStack.push(BlendMode.SRC_OVER);
    this.width = context.surface().getWidth();
    this.height = context.surface().getHeight();
  }

  public float getCurrentAlpha() {
    return alphaStack.peek();
  }

  public BlendMode getCurrentBlendMode() {
    return blendStack.peek();
  }

  public void pushAlpha(float alpha) {
    alphaStack.push(alphaStack.peek() * alpha);
  }

  public void popAlpha() {
    if (alphaStack.size() > 1) {
      alphaStack.pop();
    } else {
      throw new RuntimeException("Cannot pop initial alpha");
    }
  }

  public void pushBlendMode(BlendMode mode) {
    blendStack.push(mode);
  }

  public void popBlendMode() {
    if (blendStack.size() > 1) {
      blendStack.pop();
    } else {
      throw new RuntimeException("Cannot pop initial blend mode");
    }
  }

  public Paint borrowPaint() {
    Paint paint;

    if (paintPool.isEmpty()) {
      paint = new Paint();
    } else {
      paint = paintPool.remove(paintPool.size() - 1);
    }

    paint.reset();
    paint.setBlendMode(getCurrentBlendMode());
    paint.setAntiAlias(true);
    return paint;
  }

  public void recyclePaint(Paint paint) {
    paintPool.add(paint);
  }

  @Deprecated
  public void allocatePaint(Consumer<Paint> block) {
    Paint paint = borrowPaint();
    try {
      block.accept(paint);
    } finally {
      recyclePaint(paint);
    }
  }

  public void applyGlobalAlpha(Paint paint) {
    SkiaEnvironment.applyGlobalAlpha(paint, 255, this);
  }

  public void applyGlobalAlpha(Paint paint, int origin) {
    SkiaEnvironment.applyGlobalAlpha(paint, origin, this);
  }

  public static void applyGlobalAlpha(Paint paint, int origin, SkiaEnvironment env) {
    paint.setAlpha((int) (origin * env.getCurrentAlpha()));
  }

  public static void run(Consumer<SkiaEnvironment> runnable) {
    if (SkiaEnvironment.current != null) {
      runnable.accept(SkiaEnvironment.current);
    }
  }

  public static void run(SkiaContext context, Consumer<SkiaEnvironment> runnable) {
    try {
      if (SkiaEnvironment.current != null) {
        runnable.accept(SkiaEnvironment.current);
        return;
      }

      SkiaEnvironment.current = new SkiaEnvironment(context);

      context.begin();
      runnable.accept(SkiaEnvironment.current);
      context.end();

      SkiaEnvironment.current.close();
      SkiaEnvironment.current = null;
    } catch (Exception e) {
      System.out.println("Error running environment: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    for (Paint paint : paintPool) {
      paint.close();
    }
    paintPool.clear();
    alphaStack.clear();
    blendStack.clear();
  }
}
