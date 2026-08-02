package aka.bocchi.injection.mixins.interfaces;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlBuffer.class)
public interface IAccessorGlBuffer {
    @Accessor("handle")
    int getHandle();
}
