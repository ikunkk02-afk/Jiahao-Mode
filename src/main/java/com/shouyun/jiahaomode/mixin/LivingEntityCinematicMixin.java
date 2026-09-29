// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCinematicMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jiahao$noJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks.isLocked(entity)
                || entity instanceof net.minecraft.entity.player.PlayerEntity player
                && com.shouyun.jiahaomode.dodge.JiahaoDodgeManager.isDodging(player)) ci.cancel();
    }
}
