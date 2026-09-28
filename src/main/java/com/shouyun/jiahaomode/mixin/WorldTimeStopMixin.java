// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldTimeStopMixin {
	@Inject(method = "getTime", at = @At("RETURN"), cancellable = true)
	private void jiahao$gameTime(CallbackInfoReturnable<Long> cir) {
		if ((Object) this instanceof ServerWorld world)
			cir.setReturnValue(JiahaoTimeStopManager.effectiveTime(world, cir.getReturnValue(), false));
	}
	@Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
	private void jiahao$dayTime(CallbackInfoReturnable<Long> cir) {
		if ((Object) this instanceof ServerWorld world)
			cir.setReturnValue(JiahaoTimeStopManager.effectiveTime(world, cir.getReturnValue(), true));
	}
	@WrapWithCondition(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/BlockEntityTickInvoker;tick()V"))
	private boolean jiahao$blockEntity(BlockEntityTickInvoker ticker) {
		return !JiahaoTimeStopManager.isTimeStopped((World) (Object) this);
	}
}
