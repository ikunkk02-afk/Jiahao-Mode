// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.JiahaoDodgeClientController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class ClientPlayerDodgeMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void jiahao$serverSamples(Vec3d input, CallbackInfo ci) {
        PlayerEntity p = (PlayerEntity)(Object)this;
        if (p.getWorld().isClient && p.isMainPlayer() && JiahaoDodgeClientController.locksMovement()) ci.cancel();
    }
}
