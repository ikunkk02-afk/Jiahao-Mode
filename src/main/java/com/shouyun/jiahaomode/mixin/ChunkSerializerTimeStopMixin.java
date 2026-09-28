// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerTimeStopMixin {
	// Vanilla reads the shared properties here instead of the dimension's World.getTime().
	@WrapOperation(method = "serializeTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProperties;getTime()J"))
	private static long jiahao$scheduledSaveTime(WorldProperties properties, Operation<Long> original,
			@Local(argsOnly = true) ServerWorld world) {
		return world.getTime();
	}
}
