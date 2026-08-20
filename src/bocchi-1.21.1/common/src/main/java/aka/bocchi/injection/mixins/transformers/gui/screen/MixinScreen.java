package aka.bocchi.injection.mixins.transformers.gui.screen;

import me.baier.client.Bocchi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
  /** 所有 Screen 的 panorama 背景统一替换为 bocchi 背景. */
  @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
  private void bocchi$renderPanorama(GuiGraphics context, float delta, CallbackInfo ci) {
    Bocchi.INSTANCE.drawBackGround();
    ci.cancel();
  }
}
