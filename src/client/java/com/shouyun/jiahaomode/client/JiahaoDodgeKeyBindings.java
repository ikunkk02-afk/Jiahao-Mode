// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.network.JiahaoDodgeRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class JiahaoDodgeKeyBindings {
    public static KeyBinding binding;
    private JiahaoDodgeKeyBindings() { }
    public static void initialize() {
        binding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jiahao-mode.dodge",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.jiahao-mode"));
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            boolean pressed = false; while (binding.wasPressed()) pressed = true;
            if (!pressed || c.player == null || c.currentScreen != null || JiahaoCinematicController.locksInput()
                    || JiahaoDodgeClientController.locksMovement() || !ClientPlayNetworking.canSend(JiahaoDodgeRequestPayload.ID)) return;
            int mask = (c.options.forwardKey.isPressed() ? 1 : 0) | (c.options.backKey.isPressed() ? 2 : 0)
                    | (c.options.leftKey.isPressed() ? 4 : 0) | (c.options.rightKey.isPressed() ? 8 : 0);
            ClientPlayNetworking.send(new JiahaoDodgeRequestPayload(mask));
        });
    }
}
