// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldTimeStopMixin {
	@Inject(method = {"doRandomBlockDisplayTicks", "setLightningTicksLeft"}, at = @At("HEAD"), cancellable = true)
	private void jiahao$ambientVisuals(CallbackInfo ci) {
		if (JiahaoTimeStopClientState.isTimeStopped((ClientWorld) (Object) this)) ci.cancel();
	}
	@Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
	private void jiahao$sky(CallbackInfo ci) {
		if (JiahaoTimeStopManager.isTimeStopped((ClientWorld) (Object) this)) ci.cancel();
	}
	@Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
	private void jiahao$entity(Entity entity, CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze(entity)) {
			entity.resetPosition();
			ci.cancel();
		}
	}
	@Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
	private void jiahao$passenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze(passenger)) {
			passenger.resetPosition();
			ci.cancel();
		}
	}
}
