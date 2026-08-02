package aka.bocchi.injection.mixins.transformers;

import me.baier.graphics.SkiaContext;
import me.baier.client.ui.splash.SplashUI;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public class MixinSplashOverlay {
  @Shadow @Final private Minecraft minecraft;
  @Shadow @Final private ReloadInstance reload;
  @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
  @Shadow @Final private boolean fadeIn;
  @Shadow private float currentProgress;
  @Shadow private long fadeOutStart;
  @Shadow private long fadeInStart;

  @Unique private SplashUI bocchi$splash = new SplashUI();
  @Unique private float bocchi$completedTime = -1.f;

  /**
   * @author
   * @reason
   */
  @Overwrite
  public void render(GuiGraphics p_281839_, int p_282704_, int p_283650_, float p_283394_)
      throws IOException {
    long l = Util.getMillis();
    if (this.fadeIn && this.fadeInStart == -1L) {
      this.fadeInStart = l;
    }

    if (bocchi$completedTime != -1.f) {
      if (this.minecraft.screen != null) {
        this.minecraft.screen.render(p_281839_, p_282704_, p_283650_, p_283394_);
      }
    }

    var ctx = SkiaContext.get();
    ctx.begin();

    float f = this.fadeOutStart > -1L ? (float) (l - this.fadeOutStart) / 1000.0F : -1.0F;
    float g = this.fadeInStart > -1L ? (float) (l - this.fadeInStart) / 500.0F : -1.0F;

    bocchi$splash.render(ctx);

    float f3 = this.reload.getActualProgress();
    this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + f3 * 0.050000012F, 0.0F, 1.f);

    if (f < 1.0F && f < this.currentProgress) {
      bocchi$splash.renderProgress(ctx, f, f);
    } else {
      bocchi$splash.renderProgress(ctx, this.currentProgress, f);
    }

    if (Math.round(this.currentProgress) >= 1f && f > 2.f && this.bocchi$completedTime == -1.f) {
      if (this.bocchi$splash.isOkToFadeOut()) {
        this.bocchi$splash.onOKToFadeOut();
      } else if (!this.bocchi$splash.isWaitingForInput()) {
        this.bocchi$completedTime = f;
      }
    }

    if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || g >= 2.0F)) {
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
    ctx.end();
    if (this.bocchi$completedTime != -1.f && f > this.bocchi$completedTime + 1.f) {
      if (this.bocchi$splash.isEnded()) {
        this.minecraft.setOverlay(null);
      }
    }
  }
}
