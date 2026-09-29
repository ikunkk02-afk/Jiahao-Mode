// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerDodgeLedgeMixin {
    @Inject(method = "adjustMovementForSneaking", at = @At("HEAD"), cancellable = true)
    private void jiahao$allowCliffRisk(Vec3d movement, MovementType type, CallbackInfoReturnable<Vec3d> cir) {
        // Only bypass sneaking's ledge clipping; Entity.move still performs every collision and fall check.
        if (JiahaoDodgeManager.isDodging((PlayerEntity)(Object)this)) cir.setReturnValue(movement);
    }
}
