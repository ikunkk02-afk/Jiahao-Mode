// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops travel only: damage, death, networking and player lifecycle continue normally. */
@Mixin(Entity.class)
public abstract class EntityCinematicMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void jiahao$holdPosition(MovementType type, Vec3d movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (JiahaoTimeStopManager.isCinematicLocked(entity)) {
            entity.setVelocity(Vec3d.ZERO);
            ci.cancel();
        }
    }
}
