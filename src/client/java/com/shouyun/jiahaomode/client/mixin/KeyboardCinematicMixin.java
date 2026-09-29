// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardCinematicMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void jiahao$input(CallbackInfo ci) {
        if (!JiahaoCinematicController.locksInput()
                && !com.shouyun.jiahaomode.client.JiahaoDodgeClientController.locksMovement()) return;
        movementForward = movementSideways = 0;
        pressingForward = pressingBack = pressingLeft = pressingRight = jumping = sneaking = false;
    }
}
