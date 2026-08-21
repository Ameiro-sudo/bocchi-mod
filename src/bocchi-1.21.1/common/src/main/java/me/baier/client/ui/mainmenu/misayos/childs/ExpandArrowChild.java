package me.baier.client.ui.mainmenu.misayos.childs;

import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Path;
import me.baier.graphics.SkiaEnvironment;
import me.baier.graphics.SkiaRenderEngine;
import me.baier.skui.SkComponent;

public class ExpandArrowChild extends SkComponent {

  private Path getArrowPath() {
    Path arrowPath = new Path();
    float ax = this.getAbsoluteX();
    float ay = this.getAbsoluteY();
    float parentHeight = this.getParent().getHeight();

    float arrowTipOffset = 10f;
    float arrowHalfWidth = 5f;
    float arrowHeightRatio = 0.06f;

    arrowPath.moveTo(ax - arrowTipOffset, ay);
    arrowPath.lineTo(ax + arrowHalfWidth, ay + parentHeight * arrowHeightRatio);
    arrowPath.lineTo(ax + arrowHalfWidth, ay - parentHeight * arrowHeightRatio);
    // arrowPath.closePath();
    return arrowPath;
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    var canvas = env.getCanvas();
    var paint = env.borrowPaint();
    // Path 是 native 资源, try-with-resources 用完即还; 原先末尾的 path.close() 是
    // "闭合轮廓"绘图指令而非释放, 曾导致每帧泄漏一个 native Path (修法同 ButtonChild)
    try (Path path = getArrowPath()) {
      paint.setColor(0xFFFFFFFF);
      paint.setMode(PaintMode.FILL);
      paint.setAntiAlias(true);

      canvas.drawPath(path, paint);
      paint.setColor(0xFFEBEBEB);
      canvas.drawCircle(this.getAbsoluteX() + 1.5f, this.getAbsoluteY(), 3.f, paint);
    }
    env.recyclePaint(paint);
  }

  public boolean isHovered() {
    try (Path currentArrowPath = getArrowPath()) {
      return currentArrowPath.contains(this.getMouseX(), this.getMouseY());
    }
  }
}
