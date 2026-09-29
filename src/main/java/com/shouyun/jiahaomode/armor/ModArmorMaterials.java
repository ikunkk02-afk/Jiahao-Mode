// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.armor;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import java.util.List;
import java.util.Map;

public final class ModArmorMaterials {
    public static final int DURABILITY_MULTIPLIER = 37;
    public static final RegistryEntry<ArmorMaterial> JIAHAO = Registry.registerReference(
            Registries.ARMOR_MATERIAL, JiahaoMode.id("jiahao"), new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                            ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 4),
                    12, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, () -> Ingredient.ofItems(Items.DIAMOND),
                    List.of(new ArmorMaterial.Layer(JiahaoMode.id("jiahao"))), 2.5F, 0.05F));

    private ModArmorMaterials() { }
}
