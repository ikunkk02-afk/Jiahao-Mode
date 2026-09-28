// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.item;

import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
	public static final Item JIAHAO_TRANSFORMER = Registry.register(
			Registries.ITEM,
			JiahaoMode.id("jiahao_transformer"),
			new JiahaoTransformerItem(new Item.Settings().maxCount(1)));

	private ModItems() {
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.add(JIAHAO_TRANSFORMER));
	}
}
