// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPushTimeStopMixin {
	@Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
	private void jiahao$noAccumulatedPush(Entity other, CallbackInfo ci) {
		if (JiahaoTimeStopManager.shouldFreeze((Entity) (Object) this) || JiahaoTimeStopManager.shouldFreeze(other)) ci.cancel();
	}
}
