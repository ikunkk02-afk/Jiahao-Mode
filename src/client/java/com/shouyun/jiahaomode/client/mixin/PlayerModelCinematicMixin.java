// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.cinematic.JiahaoPoseController;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerModelCinematicMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void jiahao$reset(CallbackInfo ci) { JiahaoPoseController.restore((PlayerEntityModel<?>) (Object) this); }
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jiahao$pose(LivingEntity entity, float a, float b, float c, float d, float e, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) JiahaoPoseController.apply((PlayerEntityModel<?>) (Object) this, player);
    }
}
