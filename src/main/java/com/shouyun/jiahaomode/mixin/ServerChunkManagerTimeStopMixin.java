// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerTimeStopMixin {
	@Shadow @Final private ServerWorld world;
	// Only this call site changes; shared TickManager state is never changed.
	@WrapOperation(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;shouldTick()Z"))
	private boolean jiahao$chunkSimulation(TickManager manager, Operation<Boolean> original) {
		return original.call(manager) && !JiahaoTimeStopManager.isTimeStopped(world);
	}
}
