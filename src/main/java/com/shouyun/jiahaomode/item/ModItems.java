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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

public final class ModItems {
	public static final Item MARKET_VIEWER = Registry.register(Registries.ITEM, JiahaoMode.id("market_viewer"), new JiahaoGadgetItem());
	public static final Item JIAHAO_CODE_EDITOR = Registry.register(Registries.ITEM, JiahaoMode.id("jiahao_code_editor"), new JiahaoGadgetItem());
	public static final Item MUSIC_DISC_JIAHAO_MARCH = musicDisc("jiahao_march");
	public static final Item MUSIC_DISC_NEVADA = musicDisc("nevada");
	public static final Item MUSIC_DISC_SPECTRE = musicDisc("spectre");
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

	private static Item musicDisc(String song) {
		return Registry.register(Registries.ITEM, JiahaoMode.id("music_disc_" + song), new Item(
				new Item.Settings().maxCount(1).rarity(Rarity.RARE)
						.jukeboxPlayable(RegistryKey.of(RegistryKeys.JUKEBOX_SONG, JiahaoMode.id(song)))));
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
				.register(entries -> {
					entries.add(JIAHAO_TRANSFORMER); entries.add(MARKET_VIEWER); entries.add(JIAHAO_CODE_EDITOR);
					entries.add(MUSIC_DISC_JIAHAO_MARCH); entries.add(MUSIC_DISC_NEVADA); entries.add(MUSIC_DISC_SPECTRE);
				});
	}
}
