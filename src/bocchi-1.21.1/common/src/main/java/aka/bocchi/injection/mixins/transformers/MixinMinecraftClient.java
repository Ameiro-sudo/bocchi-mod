package aka.bocchi.injection.mixins.transformers;

import com.mojang.blaze3d.platform.Window;
import java.util.concurrent.CompletableFuture;
import me.baier.client.Bocchi;
import me.baier.client.ui.theme.Theme;
import me.baier.client.ui.theme.ThemeManager;
import me.baier.design.Design;
import me.baier.graphics.SkiaContext;
import me.baier.graphics.SkiaRenderEngine;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

  @Shadow @Final private Window window;
  @Shadow public Screen screen;
  @Shadow public ClientLevel level;

  @Inject(at = @At("TAIL"), method = "<init>")
  private void init(CallbackInfo info) {
    Bocchi.INSTANCE.start();
  }

  // 带描述符精确匹配无参重载 reloadResourcePacks(), 避免同时命中
  // reloadResourcePacks(boolean, GameLoadCookie) 导致每次重载执行两遍
  @Inject(
      method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",
      at = @At("TAIL"))
  private void hookReloadResourcePacks(CallbackInfoReturnable<CompletableFuture<Void>> info) {
    info.getReturnValue()
        .thenRun(
            () ->
                Minecraft.getInstance()
                    .execute(
                        () -> {
                          Design.reload();
                          SkiaRenderEngine.clearTextureCache();
                        }));
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
      Theme theme = ThemeManager.get();
      Screen menu = theme.getMainMenuScreen();
      if (this.screen == menu) {
        return this.screen;
      }
      return menu;
    }
    return originalScreen;
  }
}
