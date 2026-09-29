// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.armor;

import com.shouyun.jiahaomode.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/** Exact item and slot checks shared by every Jiahao ability. */
public final class JiahaoArmorUtil {
    public static boolean isWearingFullJiahaoArmor(LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.JIAHAO_HELMET)
                && entity.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.JIAHAO_CHESTPLATE)
                && entity.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.JIAHAO_LEGGINGS)
                && entity.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.JIAHAO_BOOTS);
    }

    public static boolean isJiahaoArmorPiece(ItemStack stack) {
        return stack.isOf(ModItems.JIAHAO_HELMET) || stack.isOf(ModItems.JIAHAO_CHESTPLATE)
                || stack.isOf(ModItems.JIAHAO_LEGGINGS) || stack.isOf(ModItems.JIAHAO_BOOTS);
    }

    private JiahaoArmorUtil() { }
}
