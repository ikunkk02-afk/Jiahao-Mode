// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldEntityTimeStopMixin {
	// Yarn's entity-list callback also checks despawning before tickEntity.
	@WrapWithCondition(method = "method_31420", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;checkDespawn()V"))
	private boolean jiahao$keepFrozenEntity(Entity entity) {
		return !JiahaoTimeStopManager.shouldFreeze(entity);
	}
	@Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
	private void jiahao$freezeEntity(Entity entity, CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze(entity)) ci.cancel();
	}
	@Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
	private void jiahao$freezePassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze(passenger)) ci.cancel();
	}
}
