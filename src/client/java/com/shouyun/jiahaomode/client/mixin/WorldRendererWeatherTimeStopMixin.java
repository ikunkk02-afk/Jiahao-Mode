// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererWeatherTimeStopMixin {
	@Shadow private ClientWorld world;
	@Shadow private int ticks;
	@Unique private int jiahao$phaseTicks;
	@Unique private boolean jiahao$adjustPhase;
	// Only local weather/cloud arguments and reads change, never the real renderer counter.
	@ModifyVariable(method = {"renderWeather", "renderClouds"}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private float jiahao$animationDelta(float delta) {
		var phase = JiahaoTimeStopClientState.renderPhase(world, ticks, delta);
		jiahao$adjustPhase = JiahaoTimeStopClientState.hasAnimationOffset(world);
		jiahao$phaseTicks = phase.ticks();
		return jiahao$adjustPhase ? phase.tickDelta() : delta;
	}
	@ModifyExpressionValue(method = {"renderWeather", "renderClouds"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;ticks:I"), require = 0)
	private int jiahao$animationTicks(int vanilla) { return jiahao$adjustPhase ? jiahao$phaseTicks : vanilla; }
	@Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
	private void jiahao$noWeatherParticlesOrRandomSounds(CallbackInfo ci) {
		if (JiahaoTimeStopClientState.isTimeStopped(world)) ci.cancel();
	}
	@Inject(method = "setWorld", at = @At("HEAD"))
	private void jiahao$worldChanged(ClientWorld next, CallbackInfo ci) { JiahaoTimeStopClientState.onWorldChanged(next); }
	@ModifyVariable(method = "renderEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private float jiahao$entityDelta(float delta, @Local(argsOnly = true) Entity entity) {
		return JiahaoTimeStopManager.shouldFreeze(entity) ? JiahaoTimeStopClientState.frozenTickDelta() : delta;
	}
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"), require = 0)
	private void jiahao$blockEntityFrame(BlockEntityRenderDispatcher dispatcher, BlockEntity entity, float delta, MatrixStack matrices, VertexConsumerProvider vertices, Operation<Void> original) {
		original.call(dispatcher, entity, JiahaoTimeStopClientState.isTimeStopped(world) ? JiahaoTimeStopClientState.frozenTickDelta() : delta, matrices, vertices);
	}
}
