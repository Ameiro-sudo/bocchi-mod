package me.baier.animation;

import java.util.Objects;
import lombok.Getter;

@Getter
public class BezierControlPoints {
  private final float p1x;
  private final float p1y;
  private final float p2x;
  private final float p2y;

  public BezierControlPoints(double p1x, double p1y, double p2x, double p2y) {
    this.p1x = (float) p1x;
    this.p1y = (float) p1y;
    this.p2x = (float) p2x;
    this.p2y = (float) p2y;
  }

  public BezierControlPoints(float p1x, float p1y, float p2x, float p2y) {
    this.p1x = p1x;
    this.p1y = p1y;
    this.p2x = p2x;
    this.p2y = p2y;
  }

  public static final BezierControlPoints EASE_IN_OUT =
      new BezierControlPoints(0.42f, 0f, 0.58f, 1f);
  public static final BezierControlPoints EASE_OUT = new BezierControlPoints(0f, 0f, 0.58f, 1f);
  public static final BezierControlPoints EASE_IN = new BezierControlPoints(0.42f, 0f, 1f, 1f);
  public static final BezierControlPoints LINEAR = new BezierControlPoints(0f, 0f, 1f, 1f);
  public static final BezierControlPoints BOUNCE = new BezierControlPoints(0.25f, 0.1f, 0.25f, 1f);
  public static final BezierControlPoints ELASTIC = new BezierControlPoints(0.7f, 0.0f, 0.3f, 1.0f);
  public static final BezierControlPoints CIRC_IN = new BezierControlPoints(0.55f, 0f, 1f, 0.45f);
  public static final BezierControlPoints CIRC_OUT = new BezierControlPoints(0f, 0.55f, 0.45f, 1f);
  public static final BezierControlPoints CIRC_IN_OUT =
      new BezierControlPoints(0.85f, 0f, 0.15f, 1f);
  public static final BezierControlPoints CUBIC_IN =
      new BezierControlPoints(0.55f, 0.055f, 0.675f, 0.19f);
  public static final BezierControlPoints CUBIC_OUT =
      new BezierControlPoints(0.215f, 0.61f, 0.355f, 1f);
  public static final BezierControlPoints CUBIC_IN_OUT =
      new BezierControlPoints(0.645f, 0.045f, 0.355f, 1f);
  public static final BezierControlPoints BACK_IN =
      new BezierControlPoints(0.6f, -0.28f, 0.735f, 0.045f);
  public static final BezierControlPoints BACK_OUT =
      new BezierControlPoints(0.175f, 0.885f, 0.32f, 1.275f);
  public static final BezierControlPoints BACK_IN_OUT =
      new BezierControlPoints(0.68f, -0.55f, 0.265f, 1.55f);
  public static final BezierControlPoints EXPO_IN =
      new BezierControlPoints(0.95f, 0.05f, 0.795f, 0.035f);
  public static final BezierControlPoints EXPO_OUT = new BezierControlPoints(0.19f, 1f, 0.22f, 1f);
  public static final BezierControlPoints EXPO_IN_OUT = new BezierControlPoints(1f, 0f, 0f, 1f);
  public static final BezierControlPoints QUAD_IN =
      new BezierControlPoints(0.55f, 0.085f, 0.68f, 0.53f);
  public static final BezierControlPoints QUAD_OUT =
      new BezierControlPoints(0.25f, 0.46f, 0.45f, 0.94f);
  public static final BezierControlPoints QUAD_IN_OUT =
      new BezierControlPoints(0.455f, 0.03f, 0.515f, 0.955f);
  public static final BezierControlPoints QUART_IN =
      new BezierControlPoints(0.895f, 0.03f, 0.685f, 0.22f);
  public static final BezierControlPoints QUART_OUT =
      new BezierControlPoints(0.165f, 0.84f, 0.44f, 1f);
  public static final BezierControlPoints QUART_IN_OUT =
      new BezierControlPoints(0.77f, 0f, 0.175f, 1f);
  public static final BezierControlPoints QUINT_IN =
      new BezierControlPoints(0.755f, 0.05f, 0.855f, 0.06f);
  public static final BezierControlPoints QUINT_OUT = new BezierControlPoints(0.23f, 1f, 0.32f, 1f);
  public static final BezierControlPoints QUINT_IN_OUT =
      new BezierControlPoints(0.86f, 0f, 0.07f, 1f);
  public static final BezierControlPoints SINE_IN =
      new BezierControlPoints(0.47f, 0f, 0.745f, 0.715f);
  public static final BezierControlPoints SINE_OUT =
      new BezierControlPoints(0.39f, 0.575f, 0.565f, 1f);
  public static final BezierControlPoints SINE_IN_OUT =
      new BezierControlPoints(0.445f, 0.05f, 0.55f, 0.95f);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BezierControlPoints that = (BezierControlPoints) o;
    return Float.compare(that.p1x, p1x) == 0
        && Float.compare(that.p1y, p1y) == 0
        && Float.compare(that.p2x, p2x) == 0
        && Float.compare(that.p2y, p2y) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(p1x, p1y, p2x, p2y);
  }
}
