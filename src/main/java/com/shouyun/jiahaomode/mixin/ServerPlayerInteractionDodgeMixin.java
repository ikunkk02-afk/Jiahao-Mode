// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;

import com.shouyun.jiahaomode.dodge.JiahaoDodgeInteractionAccess;
import net.minecraft.server.network.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionDodgeMixin implements JiahaoDodgeInteractionAccess {
    @Shadow protected ServerWorld world;
    @Shadow @Final protected ServerPlayerEntity player;
    @Shadow private boolean mining;
    @Shadow private boolean failedToMine;
    @Shadow private BlockPos miningPos;
    @Shadow private BlockPos failedMiningPos;
    public void jiahao$cancelMining() {
        if (mining) world.setBlockBreakingInfo(player.getId(), miningPos, -1);
        if (failedToMine) world.setBlockBreakingInfo(player.getId(), failedMiningPos, -1);
        mining = failedToMine = false;
    }
}
