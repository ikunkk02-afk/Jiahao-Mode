// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.client.visual.JiahaoWeatherAccess;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Registered only in the client config; logical ServerWorld instances remain unchanged. */
@Mixin(World.class)
public abstract class WorldWeatherTimeStopMixin implements JiahaoWeatherAccess {
	@Shadow protected float rainGradient;
	@Shadow protected float rainGradientPrev;
	@Shadow protected float thunderGradient;
	@Shadow protected float thunderGradientPrev;
	public float jiahao$rawThunder(float delta) { return MathHelper.lerp(delta, thunderGradientPrev, thunderGradient); }
	public void jiahao$setWeatherLevels(float rain, float thunder) {
		rainGradient = rainGradientPrev = rain; thunderGradient = thunderGradientPrev = thunder;
	}
	@Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
	private void jiahao$rain(float delta, CallbackInfoReturnable<Float> cir) {
		if (JiahaoTimeStopClientState.isTimeStopped((World) (Object) this)) cir.setReturnValue(JiahaoTimeStopClientState.frozenRain());
	}
	@Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
	private void jiahao$thunder(float delta, CallbackInfoReturnable<Float> cir) {
		if (JiahaoTimeStopClientState.isTimeStopped((World) (Object) this)) cir.setReturnValue(JiahaoTimeStopClientState.frozenThunder());
	}
	@Inject(method = {"setRainGradient", "setThunderGradient"}, at = @At("HEAD"), cancellable = true)
	private void jiahao$holdWeather(float value, CallbackInfo ci) {
		if (JiahaoTimeStopClientState.isTimeStopped((World) (Object) this)) ci.cancel();
	}
}
