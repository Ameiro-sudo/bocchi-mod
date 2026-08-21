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
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static me.baier.utils.ColorUtil.lerpColor;

public class ButtonChild extends SkComponent {
  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(ButtonChild.class);

  /** 资源缺失时的占位图标 (透明圆), 避免主菜单因单个图标缺失直接崩溃. */
  private static final String FALLBACK_SVG =
      "<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'>"
          + "<circle cx='8' cy='8' r='6' fill='#000000' fill-opacity='0'/></svg>";

  private SVGDOM dom;
  /** SVG 预录制缓存: 矢量指令只展开一次, 之后每帧 drawPicture 重放 (见 getOrRecordIcon). */
  private Picture iconPicture;
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
      // 图标资源缺失不崩溃: 回退空白占位
      dom = new SVGDOM(Data.makeFromBytes(FALLBACK_SVG.getBytes(StandardCharsets.UTF_8)));
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

  /** 把 SVG 矢量指令录制成 Picture (按图标固有尺寸), 只做一次. */
  private Picture getOrRecordIcon(SVGDOM svg) {
    if (iconPicture != null) {
      return iconPicture;
    }
    try (SVGSVG root = svg.getRoot()) {
      SVGLengthContext lc = new SVGLengthContext(new Point(0, 0));
      float width = lc.resolve(root.getWidth(), SVGLengthType.HORIZONTAL);
      float height = lc.resolve(root.getHeight(), SVGLengthType.VERTICAL);
      svg.setContainerSize(new Point(width, height));
      try (PictureRecorder recorder = new PictureRecorder()) {
        svg.render(recorder.beginRecording(Rect.makeWH(width, height)));
        iconPicture = recorder.finishRecordingAsPicture();
      }
    } catch (Exception e) {
      // 录制失败不崩溃: 返回 null, 本帧跳过图标 (与资源缺失占位同策略)
      LOGGER.warn("bocchi: failed to record icon picture", e);
      return null;
    }
    return iconPicture;
  }

  public void drawIcon(SkiaEnvironment env, Point pos, Point bounds, SVGDOM svg, int color) {
    if (svg == null) {
      return;
    }
    Picture picture = getOrRecordIcon(svg);
    if (picture == null) {
      return;
    }
    var canvas = env.getCanvas();
    try (SVGSVG root = svg.getRoot()) {

      SVGLengthContext lc = new SVGLengthContext(bounds);
      var width = lc.resolve(root.getWidth(), SVGLengthType.HORIZONTAL);
      var height = lc.resolve(root.getHeight(), SVGLengthType.VERTICAL);

      var scale = Math.min(bounds.getX() / width, bounds.getY() / height);
      canvas.save();
      canvas.translate(pos.getX(), pos.getY());
      canvas.scale(scale, scale);
      // 染色层只覆盖图标包围盒 (旧实现是整屏离屏层 x 按钮数, 白白多付全屏带宽):
      // SRC_IN 需要与下层内容隔离才能按 SVG 自身 alpha 染色, 所以仍需 saveLayer, 但面积缩到最小
      try (var colorFilter = ColorFilter.makeBlend(color, BlendMode.SRC_IN)) {
        var fillColorPaint = env.borrowPaint().setColorFilter(colorFilter);
        canvas.saveLayer(Rect.makeWH(width, height), fillColorPaint);
        canvas.drawPicture(picture);
        env.recyclePaint(fillColorPaint);
        canvas.restore();
      }
      canvas.restore();
    }
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    if (isHovered() || (isChildHovered() && isValidMousePos())) {
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
    paint.setColor(lerpColor(0XFF353535, 0XFFFBA0BE, hoverAnimation.getCurrentValue()));
    paint.setMode(PaintMode.FILL);
    paint.setAntiAlias(true);

    // Path 是 native 资源, 用完即还; (旧代码在 drawPath 后调 path.close(),
    // 那是"闭合轮廓"绘图指令而非释放, 纯属误用)
    try (Path path = getButtonPath()) {
      canvas.drawPath(path, paint);
    }
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
  }

  public boolean isHovered() {
    try (Path path = getButtonPath()) {
      return path.contains(this.getMouseX(), this.getMouseY());
    }
  }

  @Override
  protected void onMouseClick(int mouseX, int mouseY, int button) {
    if (button == 0 && onClick != null) {
      onClick.run();
    }
  }
}
