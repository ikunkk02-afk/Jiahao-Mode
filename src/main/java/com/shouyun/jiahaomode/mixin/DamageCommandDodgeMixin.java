// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.command.DamageCommand;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageCommand.class)
public abstract class DamageCommandDodgeMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private static boolean jiahao$administrativeDamage(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        var server = ((ServerWorld)target.getWorld()).getServer();
        JiahaoDodgeManager.enterAdministrativeDamage(server);
        try { return original.call(target, source, amount); }
        finally { JiahaoDodgeManager.leaveAdministrativeDamage(server); }
    }
}
