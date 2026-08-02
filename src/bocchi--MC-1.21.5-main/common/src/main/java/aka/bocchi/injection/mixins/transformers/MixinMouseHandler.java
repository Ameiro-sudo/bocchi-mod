package aka.bocchi.injection.mixins.transformers;

import com.mojang.blaze3d.platform.Window;
import me.baier.client.Bocchi;
import me.baier.event.impl.MouseClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
  @Shadow @Final private Minecraft minecraft;

  @Inject(method = "onPress", at = @At("HEAD"))
  public void onMouseClicked(
      long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
    var window = this.minecraft.getWindow();
    var event =
        MouseClickEvent.builder()
            .posX(getScaledXPos(window))
            .posY(getScaledYPos(window))
            .button(button)
            .build();
    Bocchi.INSTANCE.getEventManager().forward(event);
  }

  @Shadow
  public abstract double getScaledXPos(Window window);

  @Shadow
  public abstract double getScaledYPos(Window window);
}
