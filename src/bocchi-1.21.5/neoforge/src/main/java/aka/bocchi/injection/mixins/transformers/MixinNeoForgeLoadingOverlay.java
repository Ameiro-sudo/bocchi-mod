package aka.bocchi.injection.mixins.transformers;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import me.baier.client.ui.splash.SplashUI;
import me.baier.graphics.SkiaContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * NeoForge 用自己的 {@link NeoForgeLoadingOverlay} 完全替换了原版 LoadingOverlay 的
 * {@code render}, 导致 common 的 {@link MixinSplashOverlay} 失效. 这里对
 * NeoForgeLoadingOverlay 本身做同样的 {@code @Overwrite}, 让 bocchi 的
 * SplashUI (bocchi_loading + TAP TO START) 在 neoforge 下也生效.
 */
@Mixin(NeoForgeLoadingOverlay.class)
public class MixinNeoForgeLoadingOverlay {
  private static final Logger LOGGER = LoggerFactory.getLogger("bocchi/splash");
  @Shadow @Final private Minecraft minecraft;
  @Shadow @Final private ReloadInstance reload;
  @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
  @Shadow private float currentProgress;
  @Shadow private long fadeOutStart;
  @Shadow @Final private DisplayWindow displayWindow;
  @Shadow @Final private ProgressMeter progressMeter;

  // 与 MixinSplashOverlay 共享单例 SplashUI, 避免重复注册 MouseClickEvent monitor
  @Unique private SplashUI bocchi$splash = SplashUI.getInstance();
  @Unique private float bocchi$completedTime = -1.f;

  /**
   * @author bocchi
   * @reason neoforge 替换了原版 LoadingOverlay.render, 这里接回 bocchi 的启动画面
   */
  @Overwrite
  public void render(GuiGraphics p_281839_, int p_282704_, int p_283650_, float p_283394_)
      throws IOException {
    long l = Util.getMillis();

    if (bocchi$completedTime != -1.f) {
      if (this.minecraft.screen != null) {
        this.minecraft.screen.render(p_281839_, p_282704_, p_283650_, p_283394_);
      }
    }

    var ctx = SkiaContext.get();
    if (!ctx.canRender()) {
      return; // 窗口最小化 (0 像素): 跳过本帧, 避免以非法尺寸重建 surface
    }
    ctx.begin();
    float f = this.fadeOutStart > -1L ? (float) (l - this.fadeOutStart) / 1000.0F : -1.0F;
    try {

      bocchi$splash.render(ctx);

      float f3 = this.reload.getActualProgress();
      this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + f3 * 0.050000012F, 0.0F, 1.f);
      this.progressMeter.setAbsolute(Mth.ceil(this.currentProgress * 1000));

      try {
        if (f < 1.0F && f < this.currentProgress) {
          bocchi$splash.renderProgress(ctx, f, f);
        } else {
          bocchi$splash.renderProgress(ctx, this.currentProgress, f);
        }
      } catch (IOException e) {
        LOGGER.error("Splash 渲染失败 (已跳过本帧)", e);
      }

      if (Math.round(this.currentProgress) >= 1f && f > 2.f && this.bocchi$completedTime == -1.f) {
        if (this.bocchi$splash.isOkToFadeOut()) {
          this.bocchi$splash.onOKToFadeOut();
        } else if (!this.bocchi$splash.isWaitingForInput()) {
          this.bocchi$completedTime = f;
        }
      }

      if (this.fadeOutStart == -1L && this.reload.isDone()) {
        try {
          this.reload.checkExceptions();
          this.onFinish.accept(Optional.empty());
        } catch (Throwable throwable) {
          this.onFinish.accept(Optional.of(throwable));
        }

        this.fadeOutStart = Util.getMillis();
        if (this.minecraft.screen != null) {
          this.minecraft.screen.init(this.minecraft, p_281839_.guiWidth(), p_281839_.guiHeight());
        }
      }
    } finally {
      ctx.end();
    }
    if (this.bocchi$completedTime != -1.f && f > this.bocchi$completedTime + 1.f) {
      if (this.bocchi$splash.isEnded()) {
        this.minecraft.setOverlay(null);
        this.progressMeter.complete();
        this.displayWindow.close();
      }
    }
  }
}
