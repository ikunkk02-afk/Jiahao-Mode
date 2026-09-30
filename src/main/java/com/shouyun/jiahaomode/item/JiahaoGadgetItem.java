// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Common-side marker. Opening a screen is handled exclusively in the client source set. */
public final class JiahaoGadgetItem extends Item {
    public JiahaoGadgetItem() { super(new Settings().maxCount(1)); }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(user instanceof net.minecraft.server.network.ServerPlayerEntity p)
            com.shouyun.jiahaomode.hao.HaoGadgetManager.open(p,user.getStackInHand(hand).isOf(ModItems.JIAHAO_CODE_EDITOR),hand);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }
}
