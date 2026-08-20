package aka.bocchi.injection.mixins.transformers.mojang;

import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.Request;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashSet;
import java.util.Set;

@Mixin(RealmsClient.class)
public class MixinRealmsClient {
  /**
   * @author baier
   * @reason enhance loading speed
   */
  @Overwrite
  private Set<String> fetchFeatureFlags() {
    // early return since this method always fails to fetch request and throws an exception
    return new HashSet<>();
  }
}
