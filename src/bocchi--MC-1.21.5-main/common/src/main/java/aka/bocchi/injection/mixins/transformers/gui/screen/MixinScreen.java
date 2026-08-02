package aka.bocchi.injection.mixins.transformers.gui.screen;

import me.baier.client.Bocchi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screen.class)
public abstract class MixinScreen {
  /**
   * @author
   * @reason
   */
  @Overwrite
  public void renderPanorama(GuiGraphics context, float delta) {
    Bocchi.INSTANCE.drawBackGround();
  }
}
