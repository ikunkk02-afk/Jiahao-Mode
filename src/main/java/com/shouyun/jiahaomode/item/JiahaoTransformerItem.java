// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.item;

import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class JiahaoTransformerItem extends Item {
	public JiahaoTransformerItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient() && user instanceof ServerPlayerEntity player) {
			JiahaoStateManager.toggleJiahao(player);
		}

		// Client success / server consume stops the interaction falling through to the other hand.
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
	}
}
