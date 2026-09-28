// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.BiConsumer;

/** Gate only simulation calls, preserving the world, chunk and entity-manager lifecycle. */
@Mixin(ServerWorld.class)
public abstract class ServerWorldSimulationTimeStopMixin {
	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/tick/WorldTickScheduler;tick(JILjava/util/function/BiConsumer;)V"), require = 2)
	private boolean jiahao$scheduledTicks(WorldTickScheduler<?> scheduler, long time, int maximum, BiConsumer<BlockPos, ?> ticker) {
		return !JiahaoTimeStopManager.isTimeStopped((ServerWorld) (Object) this);
	}
	@Inject(method = {"tickWeather", "processSyncedBlockEvents"}, at = @At("HEAD"), cancellable = true)
	private void jiahao$weatherAndBlockEvents(CallbackInfo ci) {
		if (JiahaoTimeStopManager.isTimeStopped((ServerWorld) (Object) this)) ci.cancel();
	}
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/SleepManager;canSkipNight(I)Z"))
	private boolean jiahao$noSleepSkip(SleepManager sleep, int percentage, Operation<Boolean> original) {
		return !JiahaoTimeStopManager.isTimeStopped((ServerWorld) (Object) this) && original.call(sleep, percentage);
	}
	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/village/raid/RaidManager;tick()V"))
	private boolean jiahao$raid(RaidManager raids) {
		return !JiahaoTimeStopManager.isTimeStopped((ServerWorld) (Object) this);
	}
	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonFight;tick()V"))
	private boolean jiahao$dragonFight(EnderDragonFight fight) {
		return !JiahaoTimeStopManager.isTimeStopped((ServerWorld) (Object) this);
	}
}
