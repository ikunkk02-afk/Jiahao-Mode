// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.item;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.armor.ModArmorMaterials;
import net.minecraft.item.ArmorItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
	public static final Item JIAHAO_HELMET = armor("jiahao_helmet", ArmorItem.Type.HELMET);
	public static final Item JIAHAO_CHESTPLATE = armor("jiahao_chestplate", ArmorItem.Type.CHESTPLATE);
	public static final Item JIAHAO_LEGGINGS = armor("jiahao_leggings", ArmorItem.Type.LEGGINGS);
	public static final Item JIAHAO_BOOTS = armor("jiahao_boots", ArmorItem.Type.BOOTS);
	public static final Item JIAHAO_TRANSFORMER = Registry.register(
			Registries.ITEM,
			JiahaoMode.id("jiahao_transformer"),
			new JiahaoTransformerItem(new Item.Settings().maxCount(1)));

	private ModItems() {
	}

	private static Item armor(String id, ArmorItem.Type type) {
		return Registry.register(Registries.ITEM, JiahaoMode.id(id), new ArmorItem(
				ModArmorMaterials.JIAHAO, type, new Item.Settings()
						.maxDamage(type.getMaxDamage(ModArmorMaterials.DURABILITY_MULTIPLIER))));
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.add(JIAHAO_HELMET); entries.add(JIAHAO_CHESTPLATE);
			entries.add(JIAHAO_LEGGINGS); entries.add(JIAHAO_BOOTS);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.add(JIAHAO_TRANSFORMER));
	}
}
