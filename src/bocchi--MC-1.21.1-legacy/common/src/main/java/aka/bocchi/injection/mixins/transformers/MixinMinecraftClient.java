package aka.bocchi.injection.mixins.transformers;

import com.mojang.blaze3d.platform.Window;
import me.baier.client.Bocchi;
import me.baier.client.ui.mainmenu.misayos.MainMenuMisayosScreen;
import me.baier.graphics.SkiaContext;
import me.baier.event.impl.ResizeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

  @Shadow @Final private Window window;
  @Shadow public Screen screen;
  @Shadow public ClientLevel level;

  @Inject(at = @At("TAIL"), method = "<init>")
  private void init(CallbackInfo info) {
    Bocchi.INSTANCE.start();
  }

  @Inject(method = "resizeDisplay", at = @At("TAIL"))
  private void hookResize(CallbackInfo info) {
    Bocchi.INSTANCE.getEventManager().forward(new ResizeEvent());
    SkiaContext.get().init((float) this.window.getGuiScale());
  }

  @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, index = 1)
  private Screen hookSetScreen(Screen originalScreen) {
    if (originalScreen instanceof TitleScreen
        || (originalScreen == null && this.level == null)) {
      if (this.screen instanceof MainMenuMisayosScreen) {
        return this.screen;
      }

      return Objects.requireNonNullElseGet(
          MainMenuMisayosScreen.INSTANCE, MainMenuMisayosScreen::new);
    }
    return originalScreen;
  }
}
