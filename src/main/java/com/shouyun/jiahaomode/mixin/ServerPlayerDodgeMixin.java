// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDodgeMixin {
    @WrapMethod(method = "travel")
    private void jiahao$dodgeTravel(Vec3d input, Operation<Void> original) {
        var p = (ServerPlayerEntity)(Object)this;
        boolean controlled = JiahaoDodgeManager.beforeTravel(p);
        original.call(JiahaoDodgeManager.isDodging(p) ? Vec3d.ZERO : input);
        if (controlled) JiahaoDodgeManager.afterTravel(p);
    }
}
