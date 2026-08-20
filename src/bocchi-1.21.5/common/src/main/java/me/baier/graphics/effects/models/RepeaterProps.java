package me.baier.graphics.effects.models;

import io.github.humbleui.types.Point;

public class RepeaterProps {
  public int copies = 3;
  public Point transformAnchor = new Point(0f, 0f);
  public Point transformPosition = new Point(50f, 0f);
  public Point transformScale = new Point(100f, 100f);
  public float transformRotation = 0f;
  public float startOpacity = 100f;
  public float endOpacity = 100f;

  public RepeaterProps() {}

  public RepeaterProps setCopies(int copies) {
    this.copies = Math.max(0, copies);
    return this;
  }

  public RepeaterProps setTransformAnchor(float x, float y) {
    this.transformAnchor = new Point(x, y);
    return this;
  }

  public RepeaterProps setTransformPosition(float x, float y) {
    this.transformPosition = new Point(x, y);
    return this;
  }

  public RepeaterProps setTransformScale(float sxPercent, float syPercent) {
    this.transformScale = new Point(sxPercent, syPercent);
    return this;
  }

  public RepeaterProps setTransformRotation(float degrees) {
    this.transformRotation = degrees;
    return this;
  }

  public RepeaterProps setOpacityRange(float startPercent, float endPercent) {
    this.startOpacity = startPercent;
    this.endOpacity = endPercent;
    return this;
  }
}
