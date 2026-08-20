package me.baier.graphics.effects;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.Point;
import me.baier.graphics.effects.models.RepeaterProps;

public class SkijaEffects {
  public static void drawShapeAlongCurve(
      Canvas canvas,
      Path shapePath,
      Paint basePaint,
      int numCopies,
      Point p0,
      Point p1,
      Point p2,
      float initialRotationDeg,
      float startScale,
      float endScale,
      float startOpacity,
      float endOpacity) {
    if (numCopies <= 0 || shapePath == null || shapePath.isEmpty()) {
      return;
    }

    float initialBaseAlpha = basePaint.getAlphaf();

    for (int i = 0; i < numCopies; i++) {
      float t = (numCopies == 1) ? 0f : (float) i / (numCopies - 1);

      float mt = 1f - t; // (1-t)
      float x = mt * mt * p0.getX() + 2f * mt * t * p1.getX() + t * t * p2.getX();
      float y = mt * mt * p0.getY() + 2f * mt * t * p1.getY() + t * t * p2.getY();

      float currentScale = lerp(startScale, endScale, t);
      float currentOpacityFactor = lerp(startOpacity, endOpacity, t);

      canvas.save();
      canvas.translate(x, y);
      canvas.rotate(initialRotationDeg);
      canvas.scale(currentScale, currentScale);

      try (Paint currentPaint = basePaint.makeClone()) {
        currentPaint.setAlphaf(initialBaseAlpha * currentOpacityFactor);
        canvas.drawPath(shapePath, currentPaint);
      }
      canvas.restore();
    }
  }

  public static void renderRepeater(
      Canvas canvas, Path shapePath, Paint basePaint, RepeaterProps props) {
    if (props.copies <= 0 || shapePath == null || shapePath.isEmpty()) {
      return;
    }

    canvas.save();
    float initialBaseAlpha = basePaint.getAlphaf();
    float startOpacityFactor = props.startOpacity / 100f;
    float endOpacityFactor = props.endOpacity / 100f;

    for (int i = 0; i < props.copies; i++) {
      if (i > 0) {
        canvas.translate(props.transformPosition.getX(), props.transformPosition.getY());

        canvas.translate(props.transformAnchor.getX(), props.transformAnchor.getY());
        canvas.scale(props.transformScale.getX() / 100f, props.transformScale.getY() / 100f);
        canvas.translate(-props.transformAnchor.getX(), -props.transformAnchor.getY());

        canvas.translate(props.transformAnchor.getX(), props.transformAnchor.getY());
        canvas.rotate(props.transformRotation);
        canvas.translate(-props.transformAnchor.getX(), -props.transformAnchor.getY());
      }

      float t = (props.copies <= 1) ? 0f : (float) i / (props.copies - 1); // 插值因子
      float currentOpacityFactor = lerp(startOpacityFactor, endOpacityFactor, t);

      try (Paint currentPaint = basePaint.makeClone()) {
        currentPaint.setAlphaf(initialBaseAlpha * currentOpacityFactor);

        canvas.drawPath(shapePath, currentPaint);
      }
    }

    canvas.restore();
  }

  private static float lerp(float start, float end, float t) {
    return start + t * (end - start);
  }
}
