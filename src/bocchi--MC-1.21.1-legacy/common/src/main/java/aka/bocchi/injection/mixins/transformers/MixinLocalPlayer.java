package aka.bocchi.injection.mixins.transformers;

import com.mojang.authlib.GameProfile;
import me.baier.event.impl.MotionUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
  public MixinLocalPlayer(ClientLevel clientLevel, GameProfile gameProfile) {
    super(clientLevel, gameProfile);
  }

  @Shadow
  protected abstract void sendIsSprintingIfNeeded();

  @Shadow
  protected abstract boolean isControlledCamera();

  @Shadow private double xLast;
  @Shadow private double yLast;
  @Shadow private double zLast;
  @Shadow private float yRotLast;
  @Shadow private float xRotLast;
  @Shadow private int positionReminder;
  @Shadow @Final public ClientPacketListener connection;
  @Shadow private boolean lastOnGround;
  @Shadow private boolean lastHorizontalCollision;
  @Shadow private boolean autoJumpEnabled;
  @Shadow @Final protected Minecraft minecraft;

  // todo @baieroops: implement MotionUpdateEvent forwarding in other way(like voyage)
  // for compatibility
  /**
   * @author winxpqq955
   * @reason 显而易见
   */
  @Overwrite
  private void sendPosition() {
    this.sendIsSprintingIfNeeded();
    if (this.isControlledCamera()) {
      final var event =
          MotionUpdateEvent.builder()
              .x(this.getX())
              .y(this.getY())
              .z(this.getZ())
              .yaw(this.getYRot())
              .pitch(this.getXRot())
              .onGround(this.onGround())
              .horizontalCollision(this.horizontalCollision)
              .build();
      event.forward();
      if (event.isCancelled()) return;
      double d = event.getX() - this.xLast;
      double e = event.getY() - this.yLast;
      double f = event.getZ() - this.zLast;
      double g = event.getYaw() - this.yRotLast;
      double h = event.getPitch() - this.xRotLast;
      ++this.positionReminder;
      boolean bl = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || this.positionReminder >= 20;
      boolean bl2 = g != (double) 0.0F || h != (double) 0.0F;
      final var pos = new Vec3(event.getX(), event.getY(), event.getZ());
      if (bl && bl2) {
        this.connection.send(
            new ServerboundMovePlayerPacket.PosRot(
                pos.x(),
                pos.y(),
                pos.z(),
                event.getYaw(),
                event.getPitch(),
                event.isOnGround()));
      } else if (bl) {
        this.connection.send(
            new ServerboundMovePlayerPacket.Pos(
                pos.x(), pos.y(), pos.z(), event.isOnGround()));
      } else if (bl2) {
        this.connection.send(
            new ServerboundMovePlayerPacket.Rot(
                event.getYaw(),
                event.getPitch(),
                event.isOnGround()));
      } else if (this.lastOnGround != this.onGround()
          || this.lastHorizontalCollision != event.isHorizontalCollision()) {
        this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(event.isOnGround()));
      }

      if (bl) {
        this.xLast = this.getX();
        this.yLast = this.getY();
        this.zLast = this.getZ();
        this.positionReminder = 0;
      }

      if (bl2) {
        this.yRotLast = event.getYaw();
        this.xRotLast = event.getPitch();
      }

      this.lastOnGround = event.isOnGround();
      this.lastHorizontalCollision = event.isHorizontalCollision();
      this.autoJumpEnabled = this.minecraft.options.autoJump().get();
    }
  }
}
