// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.client.cinematic.JiahaoPoseController;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRenderCinematicMixin {
    @Shadow protected EntityModel<?> model;
    @WrapMethod(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    private void jiahao$restoreModel(LivingEntity entity, float yaw, float delta, MatrixStack matrices,
                                     VertexConsumerProvider vertices, int light, Operation<Void> original) {
        try { original.call(entity, yaw, delta, matrices, vertices, light); }
        finally { if (model instanceof PlayerEntityModel<?> playerModel) JiahaoPoseController.restore(playerModel); }
    }
    @ModifyVariable(method = "setupTransforms", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float jiahao$stableBodyYaw(float vanilla, @Local(argsOnly = true) LivingEntity entity) {
        return entity instanceof PlayerEntity player && JiahaoCinematicController.isPoseActive(player)
                ? JiahaoCinematicController.yaw() : vanilla;
    }
}
