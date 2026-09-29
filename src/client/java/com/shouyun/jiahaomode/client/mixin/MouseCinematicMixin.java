// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mouse.class)
public abstract class MouseCinematicMixin {
    // Let vanilla consume mouse deltas and operate GUI cursors, suppress only player rotation.
    @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private boolean jiahao$look(ClientPlayerEntity player, double x, double y) { return !JiahaoCinematicController.locksInput(); }
}
