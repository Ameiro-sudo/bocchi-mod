package me.baier.graphics;

import io.github.humbleui.skija.Paint;

/**
 * @author ByteBreaker create 2024/7/11
 */
public interface SkiaCallback {
  void apply(Paint paint);

  SkiaCallback DEFAULT = paint -> {};
}
