// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Never cancels connection ticks, acknowledgements or keep-alives. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerTimeStopMixin {
	@Shadow public ServerPlayerEntity player;
	@Shadow private Vec3d requestedTeleportPos;
	@Shadow private boolean floating;
	@Shadow private int floatingTicks;
	@Shadow private boolean vehicleFloating;
	@Shadow private int vehicleFloatingTicks;
	@Unique private static final String JIAHAO_MAIN_THREAD = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V";

	@Inject(method = "tick", at = @At("HEAD"))
	private void jiahao$keepConnectionAlive(CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze(player) || JiahaoTimeStopManager.isCinematicLocked(player)) {
			floating = false; floatingTicks = 0; vehicleFloating = false; vehicleFloatingTicks = 0;
		}
	}
	@Inject(method = "onPlayerMove", at = @At(value = "INVOKE", target = JIAHAO_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void jiahao$lockMovement(PlayerMoveC2SPacket packet, CallbackInfo ci) {
		if (!JiahaoTimeStopManager.shouldFreeze(player) && !JiahaoTimeStopManager.isCinematicLocked(player)) return;
		if (JiahaoTimeStopManager.isCinematicLocked(player)) { ci.cancel(); return; }
		if (requestedTeleportPos == null) {
			var lock = JiahaoTimeStopManager.movementCorrection(player);
			if (lock != null) player.networkHandler.requestTeleport(lock.position.x, lock.position.y, lock.position.z, lock.yaw, lock.pitch);
		}
		ci.cancel();
	}
	@Inject(method = "onPlayerAction", at = @At(value = "INVOKE", target = JIAHAO_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void jiahao$actions(PlayerActionC2SPacket packet, CallbackInfo ci) {
		if (!JiahaoTimeStopManager.shouldFreeze(player) && !JiahaoTimeStopManager.isCinematicLocked(player)) return;
		player.networkHandler.updateSequence(packet.getSequence());
		player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getServerWorld(), packet.getPos()));
		player.currentScreenHandler.syncState();
		ci.cancel();
	}
	@Inject(method = "onPlayerInteractBlock", at = @At(value = "INVOKE", target = JIAHAO_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void jiahao$useBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
		if (!JiahaoTimeStopManager.shouldFreeze(player) && !JiahaoTimeStopManager.isCinematicLocked(player)) return;
		player.networkHandler.updateSequence(packet.getSequence());
		var hit = packet.getBlockHitResult();
		player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getServerWorld(), hit.getBlockPos()));
		player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getServerWorld(), hit.getBlockPos().offset(hit.getSide())));
		player.currentScreenHandler.syncState();
		ci.cancel();
	}
	@Inject(method = "onPlayerInteractItem", at = @At(value = "INVOKE", target = JIAHAO_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void jiahao$useItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
		if (!JiahaoTimeStopManager.shouldFreeze(player) && !JiahaoTimeStopManager.isCinematicLocked(player)) return;
		player.networkHandler.updateSequence(packet.getSequence());
		player.currentScreenHandler.syncState();
		ci.cancel();
	}
	@Inject(method = {"onPlayerInteractEntity", "onVehicleMove", "onPlayerInput", "onBoatPaddleState", "onClientCommand",
			"onClickSlot", "onCreativeInventoryAction", "onCraftRequest", "onButtonClick", "onUpdateSign", "onUpdateBeacon",
			"onUpdateCommandBlock", "onUpdateCommandBlockMinecart", "onUpdateStructureBlock", "onUpdateJigsaw", "onRenameItem", "onSlotChangedState"},
			at = @At(value = "INVOKE", target = JIAHAO_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void jiahao$worldMutation(CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze(player)) {
			player.currentScreenHandler.syncState();
			ci.cancel();
		}
	}
	@Inject(method = {"onPlayerInteractEntity", "onPlayerInput", "onClientCommand"},
			at = @At(value = "INVOKE", target = JIAHAO_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void jiahao$cinematicActions(CallbackInfo ci) {
		if (JiahaoTimeStopManager.isCinematicLocked(player)) ci.cancel();
	}
}
