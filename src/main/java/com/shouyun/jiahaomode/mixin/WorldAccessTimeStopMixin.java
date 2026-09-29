// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scheduled ticks must use the same effective time as simulation and chunk serialization. */
@Mixin(WorldAccess.class)
public interface WorldAccessTimeStopMixin {
    @WrapOperation(method = {
            "createOrderedTick(Lnet/minecraft/util/math/BlockPos;Ljava/lang/Object;ILnet/minecraft/world/tick/TickPriority;)Lnet/minecraft/world/tick/OrderedTick;",
            "createOrderedTick(Lnet/minecraft/util/math/BlockPos;Ljava/lang/Object;I)Lnet/minecraft/world/tick/OrderedTick;"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProperties;getTime()J"))
    private long jiahao$scheduledTime(WorldProperties properties, Operation<Long> original) {
        return (Object)this instanceof ServerWorld world ? world.getTime() : original.call(properties);
    }
}
