// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.dodge.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerDodgeMixin implements JiahaoDodgeNetworkAccess {
    @Shadow public ServerPlayerEntity player;
    @Shadow private Vec3d requestedTeleportPos;
    @Shadow private boolean floating;
    @Shadow private int floatingTicks;
    @Shadow public abstract void syncWithPlayerPosition();
    public boolean jiahao$hasPendingTeleport() { return requestedTeleportPos != null; }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;playerTick()V", shift = At.Shift.AFTER))
    private void jiahao$acceptServerMotion(CallbackInfo ci) {
        if (JiahaoDodgeManager.isDodging(player)) {
            syncWithPlayerPosition(); floating = false; floatingTicks = 0;
        }
    }
    @Inject(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V", shift = At.Shift.AFTER), cancellable = true)
    private void jiahao$ignoreClientPosition(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (!JiahaoDodgeManager.isDodging(player)) return;
        if (!Double.isFinite(packet.getX(0)) || !Double.isFinite(packet.getY(0)) || !Double.isFinite(packet.getZ(0))
                || !Float.isFinite(packet.getYaw(0)) || !Float.isFinite(packet.getPitch(0))) {
            ((ServerPlayNetworkHandler)(Object)this).disconnect(Text.translatable("multiplayer.disconnect.invalid_player_movement"));
        } else {
            player.setYaw(MathHelper.wrapDegrees(packet.getYaw(player.getYaw())));
            player.setPitch(MathHelper.clamp(packet.getPitch(player.getPitch()), -90, 90));
        }
        // Never use position/onGround from this packet. Acknowledgements have their own vanilla handler.
        ci.cancel();
    }
}
