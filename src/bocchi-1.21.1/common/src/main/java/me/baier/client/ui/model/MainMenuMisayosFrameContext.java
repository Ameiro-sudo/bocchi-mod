package me.baier.client.ui.model;

import io.github.humbleui.types.Point;
import lombok.Getter;
import me.baier.design.Design;

@Getter
public class MainMenuMisayosFrameContext extends FrameContext {
  protected final Point block1Pos;
  protected final float block1Size;
  protected final Point block3Pos;
  protected final float block3Size;

  /**
   * block1 的位置与尺寸可被 design.json 的 layout 段覆盖 (Bocchi Designer 的「misayos 布局微调」)。
   * design.json 里存的是相对宽/高的归一化比例而非像素。尺寸键写绝对比例, Offset 键写相对内置
   * 位置的偏移比例 (缺省 0), 所以不覆盖时行为与之前完全一致。
   *
   * <p>注意: 下面的赋值必须各自写成单行 —— sync/check-layout.py 逐行求值, 折行会让
   * block1Pos/block1Size 这类绑定整个消失, 连带十几个下游 fact 误报漂移。
   */
  public MainMenuMisayosFrameContext() {
    super();
    float block1X = 0.095f + Design.num("layout.misayos.block1XOffset", 0f);
    float block1Y = 0.4f + Design.num("layout.misayos.block1YOffset", 0f);
    block1Pos = new Point(scaledWidth * block1X, scaledHeight * block1Y);
    float block1SizeW = Design.num("layout.misayos.block1SizeW", 0.2265625f);
    float block1SizeH = Design.num("layout.misayos.block1SizeH", 0.4027777777777778f);
    block1Size = Math.min(scaledWidth * block1SizeW, scaledHeight * block1SizeH);
    block3Pos = new Point(scaledWidth * 0.263125f, scaledHeight * 0.0972222222222222f);
    block3Size = Math.min(scaledWidth * 0.4166666666666667f, scaledHeight * 0.7407407407407407f);
  }
}
