package me.baier.client.ui.settings;

import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import java.util.Comparator;
import me.baier.animation.BezierAnimation;
import me.baier.animation.BezierControlPoints;
import me.baier.client.Bocchi;
import me.baier.client.cfg.Cfgs;
import me.baier.client.mod.Mod;
import me.baier.client.mod.setting.Setting;
import me.baier.client.mod.setting.impl.BooleanSetting;
import me.baier.client.mod.setting.impl.EnumSetting;
import me.baier.client.mod.setting.impl.NumberSetting;
import me.baier.client.mod.setting.impl.RangedSetting;
import me.baier.design.Design;
import me.baier.graphics.SkiaEnvironment;
import me.baier.graphics.font.FontSet;
import me.baier.skui.SkComponent;
import me.baier.skui.impl.SkContainer;
import me.baier.skui.layout.impl.FlowVerticalLayout;
import net.minecraft.util.Mth;

import static me.baier.utils.ColorUtil.lerpColor;

/**
 * Cfgs 面板卡片: 标题区 + 可滚动的设置行列表 + DONE 按钮.
 * 行内容来自 Mods 注册表 (逐 mod -> 逐分组 -> 逐设置), 另附主题切换行.
 * 每次改动即时回写 Cfgs (对称 save 路径).
 */
public class SettingsPanel extends SkComponent {
  private static final float CARD_WIDTH = 200.f;
  private static final float CARD_HEIGHT = 190.f;
  private static final float TITLE_SIZE = 12.f;

  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(SettingsPanel.class);

  private final ScrollList list;
  private final Runnable onClose;

  public SettingsPanel(Runnable onClose) {
    this.onClose = onClose;
    setWidth(CARD_WIDTH);
    setHeight(CARD_HEIGHT);
    // root 即全屏布局坐标系, 直接按 480x270 居中
    setX((480.f - CARD_WIDTH) / 2);
    setY((270.f - CARD_HEIGHT) / 2);

    this.list = new ScrollList(8, 26, CARD_WIDTH - 16, CARD_HEIGHT - 52);
    addChild(list);
    buildRows();

    var doneButton = new DoneButton(onClose);
    doneButton.setX((CARD_WIDTH - doneButton.getWidth()) / 2);
    doneButton.setY(CARD_HEIGHT - 18);
    addChild(doneButton);
  }

  /** 从注册表构建行: mod 头 -> 分组头(仅具名分组) -> 设置行; 末尾附 THEME 区块. */
  private void buildRows() {
    var modManager = Bocchi.INSTANCE.getModManager();
    if (modManager == null) {
      LOGGER.warn("bocchi: settings panel opened before Mods initialized");
      return;
    }
    Runnable persist = Cfgs.INSTANCE::save;
    modManager.getMods().stream()
        .sorted(Comparator.comparing(Mod::getLabel))
        .forEach(
            mod -> {
              list.addRow(new SettingRows.HeaderRow(mod.getLabel()));
              for (var group : mod.groups) {
                boolean named = !"default".equals(group.getLabel());
                if (named && !group.getSettings().isEmpty()) {
                  list.addRow(new SettingRows.HeaderRow(group.getLabel()));
                }
                for (Setting<?> setting : group.getSettings()) {
                  SettingRows.Row row = rowFor(setting, persist);
                  if (row != null) {
                    list.addRow(row);
                  } else {
                    LOGGER.debug("bocchi: no widget for setting {} (skipped)", setting.getLabel());
                  }
                }
              }
            });
    list.addRow(new SettingRows.HeaderRow("Theme"));
    list.addRow(new SettingRows.ThemeRow());
  }

  /** 四种内置设置类型 -> 对应控件; 未知类型跳过 (与加载侧的容错策略一致). */
  private static SettingRows.Row rowFor(Setting<?> setting, Runnable persist) {
    if (setting instanceof BooleanSetting bool) {
      return new SettingRows.ToggleRow(bool, persist);
    }
    if (setting instanceof EnumSetting<? extends Enum<?>> en) {
      return new SettingRows.CycleRow(en, persist);
    }
    if (setting instanceof NumberSetting<?> || setting instanceof RangedSetting<?>) {
      // 通配符捕获无法直接证明 <? extends Number>, 经运行时类型分支后安全收窄
      @SuppressWarnings("unchecked")
      Setting<? extends Number> numeric = (Setting<? extends Number>) setting;
      return new SettingRows.StepperRow(numeric, persist);
    }
    return null;
  }

  @Override
  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
    var canvas = env.getCanvas();
    float x = getAbsoluteX();
    float y = getAbsoluteY();
    float w = getWidth();
    float h = getHeight();

    // 卡面 (与侧栏面板同色系) + 左缘粉色饰条
    var fill = env.borrowPaint();
    fill.setAntiAlias(true);
    fill.setMode(PaintMode.FILL);
    fill.setColor(0xFFF5F5F5);
    try (Path card = new Path()) {
      card.addRRect(RRect.makeXYWH(x, y, w, h, 4.f));
      canvas.drawPath(card, fill);
    }
    fill.setColor(0xFFFBA0BE);
    canvas.drawRect(Rect.makeXYWH(x, y, 3.f, h), fill);
    env.recyclePaint(fill);

    // 标题 (design.json texts.sTitle 可覆盖)
    var titleFont = FontSet.SH_HEAVY.getFont(TITLE_SIZE);
    String title = Design.value("texts.sTitle", "CFGS");
    titleFont.drawString(title, x + 10, y + 8 + TITLE_SIZE / 2 - titleFont.getHeight() / 2, 0xFF353535);
    // 标题下划线 (侧栏标题横线同款)
    var linePaint = env.borrowPaint().setStroke(true).setStrokeWidth(0.2f).setColor(0x66353535);
    canvas.drawLine(x + 10, y + 22, x + w - 10, y + 22, linePaint);
    env.recyclePaint(linePaint);
  }

  /**
   * 纵向滚动列表. skui 的 SkContainer 只带了滚动条/滚轮计数的半成品
   * (内容子组件不渲染、滚动值不落位、layoutHeight 无人计算), 这里补齐胶水:
   * FlowVerticalLayout 在 content 子容器内做内容坐标排版, 滚动时整体平移 content,
   * 命中测试走绝对坐标因此天然随偏移对齐; 渲染期裁剪到列表矩形.
   */
  public static class ScrollList extends SkContainer {
    private static final float PADDING = 4.f;
    private static final float SPACING = 4.f;
    private static final float SCROLL_STEP = 60.f;

    private final SkComponent content = new SkComponent();
    private final FlowVerticalLayout flowLayout = new FlowVerticalLayout(); // padding/spacing 经 setter 设置 (无带参构造器)

    ScrollList(float x, float y, float width, float height) {
      super(x, y, width, height);
      this.flowLayout.setPadding(PADDING);
      this.flowLayout.setSpacing(SPACING);
      addChild(content);
    }

    void addRow(SkComponent row) {
      row.setWidth(getWidth() - 12); // 左右留白, 右侧再给滚动条让位
      content.addChild(row);
    }

    /** 排版 + 滚动落位 + layoutHeight 计算; 输入事件前也调用以保证命中对齐. */
    private void syncContent() {
      flowLayout.applyLayout(content);
      var rows = content.getChildren();
      float total = PADDING * 2;
      for (int i = 0; i < rows.size(); i++) {
        var row = rows.get(i);
        total += row.getHeight() + row.getMarginTop() + row.getMarginBottom();
        if (i > 0) {
          total += SPACING;
        }
      }
      content.setY(getScroller().getCurrentValue());
      // 内容不足一屏时钳为容器高, 让滚动条自然隐藏
      setLayoutHeight(Math.max(total, getHeight()));
    }

    @Override
    protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
      syncContent();
      super.onRender(env, mouseX, mouseY); // scroller.update + 动画未完成期间 invalidateLayout
    }

    @Override
    protected void onRenderChildren(SkiaEnvironment env, int mouseX, int mouseY) {
      var canvas = env.getCanvas();
      canvas.save();
      canvas.clipRect(Rect.makeXYWH(getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight()));
      content.render(env, mouseX, mouseY);
      scrollbar.render(env, mouseX, mouseY);
      canvas.restore();
    }

    @Override
    protected boolean onMouseScroll(int mouseX, int mouseY, int scroll) {
      syncContent(); // 先同步再判定可滚动量
      return super.onMouseScroll(mouseX, mouseY, scroll);
    }

    @Override
    public void handleMouseMove(int mouseX, int mouseY) {
      syncContent();
      super.handleMouseMove(mouseX, mouseY);
    }

    @Override
    public boolean handleMouseClick(int mouseX, int mouseY, int button) {
      syncContent();
      return super.handleMouseClick(mouseX, mouseY, button);
    }

    @Override
    public boolean handleMouseRelease(int mouseX, int mouseY, int button) {
      syncContent();
      return super.handleMouseRelease(mouseX, mouseY, button);
    }
  }

  /** DONE 按钮: 主菜单 ButtonChild 同款配色反转用法 (深灰底 hover 变粉). */
  private static class DoneButton extends SkComponent {
    private final Runnable onClose;
    private boolean animHot = false;
    private BezierAnimation<Float> hoverAnimation =
        BezierAnimation.createFloat(0.f, 1.f, 300, BezierControlPoints.CUBIC_OUT);

    DoneButton(Runnable onClose) {
      this.onClose = onClose;
      setWidth(60.f);
      setHeight(12.f);
    }

    @Override
    protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {
      if (isHovered()) {
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

      var canvas = env.getCanvas();
      var paint = env.borrowPaint();
      paint.setAntiAlias(true);
      paint.setMode(PaintMode.FILL);
      paint.setColor(lerpColor(0xFF353535, 0xFFFBA0BE, hoverAnimation.getCurrentValue()));
      try (Path path = new Path()) {
        path.addRRect(
            RRect.makeXYWH(
                getAbsoluteX(),
                getAbsoluteY(),
                getWidth(),
                getHeight(),
                Mth.lerp(hoverAnimation.getCurrentValue(), 3.5f, 5.5f)));
        canvas.drawPath(path, paint);
      }
      env.recyclePaint(paint);

      var font = FontSet.SH_BOLD.getFont(8.5f);
      String text = Design.value("texts.sDone", "DONE");
      float textWidth = font.getStringWidth(text);
      font.drawString(
          text,
          getAbsoluteX() + getWidth() / 2 - textWidth / 2,
          getAbsoluteY() + getHeight() / 2 - font.getHeight() / 2,
          0xFFFFFFFF);
    }

    @Override
    protected void onMouseClick(int mouseX, int mouseY, int button) {
      if (button == 0) {
        onClose.run();
      }
    }
  }
}