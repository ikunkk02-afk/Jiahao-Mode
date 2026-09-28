// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerTimeStopMixin {
	@Inject(method = "playerTick", at = @At("HEAD"), cancellable = true)
	private void jiahao$playerSimulation(CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze((ServerPlayerEntity) (Object) this)) ci.cancel();
	}
}
