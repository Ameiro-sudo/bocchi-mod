package aka.bocchi.injection.mixins.transformers;

import me.baier.graphics.pipeline.post.PostEffectRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.BEFORE))
    private void onPostEffectPre(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PostEffectRenderer.beginDraw();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void onPostEffectAfter(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PostEffectRenderer.postDraw();
        PostEffectRenderer.render();
    }
}
