package me.baier.client.ui.mainmenu.misayos.childs;

import io.github.humbleui.skija.*;
import io.github.humbleui.skija.svg.*;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.design.Design;
import me.baier.graphics.SkiaEnvironment;
import me.baier.graphics.font.FontSet;
import me.baier.graphics.font.SkiaFont;
import me.baier.skui.SkComponent;
import me.baier.utils.ResPack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.InputStream;

import static me.baier.utils.ColorUtil.lerpColor;

public class ButtonChild extends SkComponent {
  private SVGDOM dom;
  private String display;
  private Runnable onClick;

  private BezierAnimation<Float> hoverAnimation =
      BezierAnimation.createFloat(0.f, 1.f, 300, BezierControlPoints.CUBIC_OUT);
  private boolean hovered = false;

  public ButtonChild setOnClick(Runnable onClick) {
    this.onClick = onClick;
    return this;
  }

  public ButtonChild(String icon, String buttonText) {

    try (InputStream inputStream =
          ResPack.open(Design.resource("svgs." + icon), "/assets/minecraft/client/svgs/" + icon + ".svg")) {

      byte[] array = inputStream.readAllBytes();
      Data data = Data.makeFromBytes(array, 0, array.length);
      dom = new SVGDOM(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.display = buttonText;
  }

  public ButtonChild() {}

  private Path getButtonPath() {
    Path buttonPath = new Path();
    buttonPath.addRRect(
        RRect.makeXYWH(
            this.getAbsoluteX(),
            this.getAbsoluteY(),
            this.getWidth(),
            getHeight(),
            Mth.lerp(hoverAnimation.getCurrentValue(), 3.5f, 5.5f)));
    return buttonPath;
  }

  public void drawIcon(SkiaEnvironment env, Point pos, Point bounds, SVGDOM svg, int color) {
    var canvas = env.getCanvas();
    try (SVGSVG root = svg.getRoot()) {

      SVGLengthContext lc = new SVGLengthContext(bounds);
      var width = lc.resolve(root.getWidth(), SVGLengthType.HORIZONTAL);
      var height = lc.resolve(root.getHeight(), SVGLengthType.VERTICAL);

      svg.setContainerSize(bounds);
      var scale = Math.min(bounds.getX() / width, bounds.getY() / height);
      canvas.save();
      canvas.translate(pos.getX(), pos.getY());
      canvas.scale(scale, scale);
      // TODO : use path.makeFromSVGString to improve performance.
      var fillColorPaint =
          env.borrowPaint().setColorFilter(ColorFilter.makeBlend(color, BlendMode.SRC_IN));
      canvas.saveLayer(Rect.makeWH(env.getWidth(), env.getHeight()), fillColorPaint);
      svg.render(canvas);
      env.recyclePaint(fillColorPaint);
      canvas.restore();
      canvas.restore();
    }
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    if (isHovered() || isChildHovered() && isValidMousePos()) {
      if (!hovered) {
        // was hovered
        hovered = true;
        hoverAnimation = BezierAnimation.createFloat(0.f, 1.f, 300, BezierControlPoints.CUBIC_OUT);
        hoverAnimation.start();
      }

    } else {

      // was hovered
      if (hovered) {
        // reverse
        hovered = false;
        hoverAnimation = BezierAnimation.createFloat(1.f, 0.f, 600, BezierControlPoints.CUBIC_OUT);
        hoverAnimation.start();
      }
    }
    hoverAnimation.update();
    var canvas = env.getCanvas();
    var paint = env.borrowPaint();
    var path = getButtonPath();
    paint.setColor(lerpColor(0XFF353535, 0XFFFBA0BE, hoverAnimation.getCurrentValue()));
    paint.setMode(PaintMode.FILL);
    paint.setAntiAlias(true);

    canvas.drawPath(path, paint);
    paint.setColor(lerpColor(0xFF5B5B5B, 0xFFFFFFFF, hoverAnimation.getCurrentValue()));
    float iconX, iconY;
    if (display != null) {

      iconX = this.getAbsoluteX() + this.getWidth() / 6;
      iconY = this.getAbsoluteY() + this.getHeight() / 2;
    } else {

      iconX = this.getAbsoluteX() + this.getWidth() / 2;
      iconY = this.getAbsoluteY() + this.getHeight() / 2;
    }

    var circleHeight = this.getHeight() / 3;
    var iconHeight = circleHeight + Mth.lerp(hoverAnimation.getCurrentValue(), 0.f, 2.f);
    canvas.drawCircle(iconX, iconY, circleHeight, paint);
    drawIcon(
        env,
        new Point(iconX - iconHeight / 2, iconY - iconHeight / 2),
        new Point(iconHeight, iconHeight),
        dom,
        lerpColor(0XFFD4D4D4, 0XFFFBA0BE, hoverAnimation.getCurrentValue()));
    if (display != null) {

      var buttonFont =
          FontSet.SH_BOLD.getFont(
              Math.round(Mth.lerp(hoverAnimation.getCurrentValue(), 9.5f, 10.5f) * 2) / 2f);
      var displayWidth = buttonFont.getStringWidth(display);
      buttonFont.drawString(
          this.display,
          this.getAbsoluteX() + this.getWidth() / 2 - displayWidth / 2 + 3.5f,
          iconY - buttonFont.getHalfHeight(),
          0xFFFFFFFF);
    }
    env.recyclePaint(paint);
    path.close();
  }

  public boolean isHovered() {
    Path path = getButtonPath();
    boolean isInside = path.contains(this.getMouseX(), this.getMouseY());
    path.close();
    return isInside;
  }

  @Override
  protected void onMouseClick(int mouseX, int mouseY, int button) {
    if (button == 0 && onClick != null) {
      onClick.run();
    }
  }
}
