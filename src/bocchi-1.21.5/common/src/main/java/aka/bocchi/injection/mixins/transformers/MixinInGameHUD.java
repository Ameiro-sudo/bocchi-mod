package aka.bocchi.injection.mixins.transformers;

import com.mojang.blaze3d.systems.RenderSystem;
import me.baier.graphics.pipeline.RenderPass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinInGameHUD {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "HEAD"))
    private void hookRenderEvent(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderPass.uploadMatrix(RenderSystem.getProjectionMatrix());
    }
}
