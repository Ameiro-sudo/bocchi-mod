package me.baier.client.ui.settings;

import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import java.util.Locale;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.impl.BooleanSetting;
import me.baier.client.mod.setting.impl.EnumSetting;
import me.baier.client.mod.setting.impl.NumberSetting;
import me.baier.client.mod.setting.impl.RangedSetting;
import me.baier.client.ui.theme.ThemeManager;
import me.baier.graphics.SkiaEnvironment;
import me.baier.graphics.font.FontSet;
import me.baier.graphics.font.SkiaFontRenderer;
import me.baier.skui.SkComponent;

import static me.baier.utils.ColorUtil.lerpColor;

/**
 * 设置行控件族. 视觉语言与主菜单侧栏一致:
 * 浅灰卡面 (#F5F5F5) + 深灰文字 (#353535) + hover 粉色强调 (E95A9F/FBA0BE 族).
 * 行高 13 (480x270 布局坐标), 事件走 skui 组件树.
 */
public final class SettingRows {

  private static final int TEXT = 0xFF353535;
  private static final int TEXT_DIM = 0xFF8A8A8A;
  private static final int ACCENT = 0xFFE95A9F;

  private SettingRows() {}

  /** 行通用底座: hover 动画 + 左侧标签绘制, 控件区交给子类. */
  abstract static class Row extends SkComponent {
    protected final Runnable onChange;
    private boolean animHot = false;
    private BezierAnimation<Float> hoverAnimation =
        BezierAnimation.createFloat(0.f, 1.f, 300, BezierControlPoints.CUBIC_OUT);
    private String label = "";

    protected Row(Runnable onChange) {
      this.onChange = onChange;
      setHeight(13.f);
    }

    Row label(String label) {
      this.label = label;
      return this;
    }

    /** hover 动画状态机 (与 ButtonChild 同款: 翻转时重建动画再 start). */
    protected void updateHover() {
      if (isHovered() || (isChildHovered() && isValidMousePos())) {
        if (!animHot) {
          animHot = true;
          hoverAnimation =
              BezierAnimation.createFloat(0.f, 1.f, 300, BezierControlPoints.CUBIC_OUT);
          hoverAnimation.start();
        }
      } else {
        if (animHot) {
          animHot = false;
          hoverAnimation =
              BezierAnimation.createFloat(1.f, 0.f, 600, BezierControlPoints.CUBIC_OUT);
          hoverAnimation.start();
        }
      }
      hoverAnimation.update();
    }

    protected void drawRowBackground(SkiaEnvironment env) {
      float v = hoverAnimation.getCurrentValue();
      if (v <= 0.f) {
        return;
      }
      var canvas = env.getCanvas();
      var paint = env.borrowPaint();
      paint.setMode(PaintMode.FILL);
      paint.setAntiAlias(true);
      // lerpColor 走 ARGB.lerp, alpha 通道一并插值: 从全透明粉到 15% 粉
      paint.setColor(lerpColor(0x00E95A9F, 0x26E95A9F, v));
      try (Path path = new Path()) {
        path.addRRect(
            RRect.makeXYWH(getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight(), 3.5f));
        canvas.drawPath(path, paint);
      }
      env.recyclePaint(paint);
    }

    protected void drawLabel(SkiaEnvironment env) {
      var font = FontSet.SH_NORMAL.getFont(8);
      font.drawString(
          label,
          getAbsoluteX() + 4,
          getAbsoluteY() + getHeight() / 2 - font.getHeight() / 2,
          TEXT);
    }

    protected void drawValueText(SkiaEnvironment env, String text, int color) {
      var font = FontSet.SH_BOLD.getFont(8);
      float valueWidth = font.getStringWidth(text);
      font.drawString(
          text,
          getAbsoluteX() + getWidth() - 6 - valueWidth,
          getAbsoluteY() + getHeight() / 2 - font.getHeight() / 2,
          color);
    }

    @Override
    protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
      updateHover();
      drawRowBackground(env);
      drawLabel(env);
      drawControl(env);
    }

    protected abstract void drawControl(SkiaEnvironment env);

    protected void onPress() {}

    @Override
    protected void onMouseClick(int mouseX, int mouseY, int button) {
      if (button == 0 && isEnabledInHierarchy()) {
        onPress();
      }
    }
  }

  /** 布尔开关: 整行点击翻转, ON/OFF 右对齐. */
  static class ToggleRow extends Row {
    private final BooleanSetting setting;

    ToggleRow(BooleanSetting setting, Runnable onChange) {
      super(onChange);
      this.setting = setting;
      label(setting.getLabel());
    }

    @Override
    protected void onPress() {
      setting.setValue(!setting.getValue());
      onChange.run();
    }

    @Override
    protected void drawControl(SkiaEnvironment env) {
      boolean on = Boolean.TRUE.equals(setting.getValue());
      drawValueText(env, on ? "ON" : "OFF", on ? ACCENT : TEXT_DIM);
    }
  }

  /** 枚举循环: 整行点击切到下一个常量 (EnumSetting.increment 自带环绕). */
  static class CycleRow extends Row {
    private final EnumSetting<? extends Enum<?>> setting;

    CycleRow(EnumSetting<? extends Enum<?>> setting, Runnable onChange) {
      super(onChange);
      this.setting = setting;
      label(setting.getLabel());
    }

    @Override
    protected void onPress() {
      setting.increment();
      onChange.run();
    }

    @Override
    protected void drawControl(SkiaEnvironment env) {
      String name = String.valueOf(setting.getValue());
      drawValueText(env, name, isHovered() ? ACCENT : TEXT_DIM);
    }
  }

  /**
   * 数值步进行: 右侧 ‹ › 两个点击区, 按 increment 步进并夹在 [min,max].
   * NumberSetting / RangedSetting 共用 (两者暴露同名的 min/max/increment 访问器).
   */
  static class StepperRow extends Row {
    private static final float BOX = 9.f;
    private static final float VALUE_GAP = 18.f;
    private final Setting<? extends Number> setting;

    StepperRow(Setting<? extends Number> setting, Runnable onChange) {
      super(onChange);
      this.setting = setting;
      label(setting.getLabel());
    }

    private double minimum() {
      if (setting instanceof NumberSetting<?> n) return n.getMinimum().doubleValue();
      if (setting instanceof RangedSetting<?> r) return r.getMinimum().doubleValue();
      return -1024;
    }

    private double maximum() {
      if (setting instanceof NumberSetting<?> n) return n.getMaximum().doubleValue();
      if (setting instanceof RangedSetting<?> r) return r.getMaximum().doubleValue();
      return 1024;
    }

    private double increment() {
      Number inc = null;
      if (setting instanceof NumberSetting<?> n) inc = n.getIncrement();
      else if (setting instanceof RangedSetting<?> r) inc = r.getIncrement();
      return inc != null && inc.doubleValue() > 0 ? inc.doubleValue() : 1;
    }

    private float leftBoxX() {
      return getAbsoluteX() + getWidth() - 6 - BOX - VALUE_GAP - BOX;
    }

    private float rightBoxX() {
      return getAbsoluteX() + getWidth() - 6 - BOX;
    }

    private float boxTop() {
      return getAbsoluteY() + (getHeight() - BOX) / 2;
    }

    private boolean inBox(int mx, int my, float boxX) {
      return mx >= boxX && mx <= boxX + BOX && my >= boxTop() && my <= boxTop() + BOX;
    }

    private void step(int direction) {
      Number current = setting.getValue();
      if (current == null) {
        return;
      }
      double next =
          Math.clamp(current.doubleValue() + direction * increment(), minimum(), maximum());
      Object typed;
      if (current instanceof Integer) typed = (int) Math.round(next);
      else if (current instanceof Long) typed = (long) next;
      else if (current instanceof Float) typed = (float) next;
      else typed = next;
      // setValue 泛型为捕获类型; 经上面的类型分支重建后以裸类型回写 (与 load() 同策略)
      @SuppressWarnings({"unchecked", "rawtypes"})
      Setting raw = setting;
      raw.setValue(typed);
      onChange.run();
    }

    @Override
    protected void onPress() {
      // 整行点击不改值; 只响应两侧步进盒 (见 onMouseClick)
    }

    @Override
    protected void onMouseClick(int mouseX, int mouseY, int button) {
      if (button != 0 || !isEnabledInHierarchy()) {
        return;
      }
      if (inBox(mouseX, mouseY, leftBoxX())) {
        step(-1);
      } else if (inBox(mouseX, mouseY, rightBoxX())) {
        step(1);
      }
    }

    @Override
    protected void drawControl(SkiaEnvironment env) {
      var glyphFont = FontSet.SH_HEAVY.getFont(7);
      drawBox(env, leftBoxX(), "‹", glyphFont);
      drawBox(env, rightBoxX(), "›", glyphFont);

      String text = format(setting.getValue());
      float centerX = (leftBoxX() + BOX + rightBoxX()) / 2;
      float textWidth = glyphFont.getStringWidth(text);
      glyphFont.drawString(
          text,
          centerX - textWidth / 2,
          getAbsoluteY() + getHeight() / 2 - glyphFont.getHeight() / 2,
          isHovered() ? ACCENT : TEXT);
    }

    private void drawBox(SkiaEnvironment env, float x, String glyph, SkiaFontRenderer font) {
      var canvas = env.getCanvas();
      var paint = env.borrowPaint();
      paint.setAntiAlias(true);
      paint.setMode(PaintMode.FILL);
      paint.setColor(inBox(getMouseX(), getMouseY(), x) ? 0x40E95A9F : 0x14353535);
      try (Path path = new Path()) {
        path.addRRect(RRect.makeXYWH(x, boxTop(), BOX, BOX, 2.5f));
        canvas.drawPath(path, paint);
      }
      env.recyclePaint(paint);
      float glyphWidth = font.getStringWidth(glyph);
      font.drawString(
          glyph,
          x + BOX / 2 - glyphWidth / 2,
          getAbsoluteY() + getHeight() / 2 - font.getHeight() / 2,
          TEXT);
    }

    private static String format(Number value) {
      if (value == null) {
        return "-";
      }
      double d = value.doubleValue();
      if (d == Math.floor(d) && !Double.isInfinite(d)) {
        return String.valueOf(value.longValue());
      }
      return String.valueOf(d);
    }
  }

  /** 分组标题行 (不可交互). */
  static class HeaderRow extends SkComponent {
    private final String label;

    HeaderRow(String label) {
      this.label = label;
      setHeight(15.f);
      setEnabled(false); // 不参与命中/hover 链, 仅展示
    }

    @Override
    protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
      var font = FontSet.SH_HEAVY.getFont(9);
      String text = label.toUpperCase(Locale.ROOT);
      font.drawString(
          text,
          getAbsoluteX() + 4,
          getAbsoluteY() + getHeight() / 2 - font.getHeight() / 2,
          TEXT);
      // 标题下的粉色短划线 (侧栏标题横线的小号呼应)
      var canvas = env.getCanvas();
      var paint = env.borrowPaint();
      paint.setStroke(true);
      paint.setStrokeWidth(1.2f);
      paint.setColor(ACCENT);
      canvas.drawLine(
          getAbsoluteX() + 4,
          getAbsoluteY() + getHeight() - 3,
          getAbsoluteX() + 16,
          getAbsoluteY() + getHeight() - 3,
          paint);
      env.recyclePaint(paint);
    }
  }

  /** 主题切换行: 点击循环注册表内主题 (ThemeManager 自带 theme.json 持久化). */
  static class ThemeRow extends Row {
    ThemeRow() {
      super(() -> {});
      label("Theme");
    }

    @Override
    protected void onPress() {
      ThemeManager.toggle();
    }

    @Override
    protected void drawControl(SkiaEnvironment env) {
      String id = ThemeManager.currentId().toUpperCase(Locale.ROOT);
      drawValueText(env, id, isHovered() ? ACCENT : TEXT_DIM);
    }
  }
}