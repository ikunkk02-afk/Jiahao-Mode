// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerTimeStopMixin {
	@Shadow protected ClientWorld world;
	@Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
	private void jiahao$particle(Particle particle, CallbackInfo ci) {
		if (JiahaoTimeStopClientState.shouldFreezeParticle(world, particle)) ci.cancel();
	}
	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EmitterParticle;tick()V"), require = 0)
	private boolean jiahao$emitter(EmitterParticle particle) { return !JiahaoTimeStopClientState.shouldFreezeParticle(world, particle); }
	@WrapOperation(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"), require = 0)
	private void jiahao$particleFrame(Particle particle, VertexConsumer vertices, Camera camera, float delta, Operation<Void> original) {
		original.call(particle, vertices, camera, JiahaoTimeStopClientState.shouldFreezeParticle(world, particle) ? JiahaoTimeStopClientState.frozenTickDelta() : delta);
	}
}
