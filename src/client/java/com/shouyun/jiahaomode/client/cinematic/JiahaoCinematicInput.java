// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.cinematic;

import net.minecraft.client.MinecraftClient;

/** Held attack/use must be released after a sequence, rather than firing a queued action. */
public final class JiahaoCinematicInput {
    private static boolean attackHeld, useHeld;
    private JiahaoCinematicInput() { }
    public static void update(MinecraftClient client) {
        boolean locked = JiahaoCinematicController.locksInput()
                || com.shouyun.jiahaomode.client.JiahaoDodgeClientController.locksMovement();
        attackHeld = client.options.attackKey.isPressed() && (locked || attackHeld);
        useHeld = client.options.useKey.isPressed() && (locked || useHeld);
        if (locked || attackHeld) while (client.options.attackKey.wasPressed()) { }
        if (locked || useHeld) while (client.options.useKey.wasPressed()) { }
        if (JiahaoCinematicController.locksInput()) while (client.options.togglePerspectiveKey.wasPressed()) { }
    }
    public static boolean blocksAttack() { return JiahaoCinematicController.locksInput() || com.shouyun.jiahaomode.client.JiahaoDodgeClientController.locksMovement() || attackHeld; }
    public static boolean blocksUse() { return JiahaoCinematicController.locksInput() || com.shouyun.jiahaomode.client.JiahaoDodgeClientController.locksMovement() || useHeld; }
    public static void reset() { attackHeld = useHeld = false; }
}
