// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import com.shouyun.jiahaomode.network.JiahaoTimeTogglePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class JiahaoTimeKeyBindings {
	private JiahaoTimeKeyBindings() { }
	public static void initialize() {
		KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jiahao-mode.jiahao_time",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.jiahao-mode"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean pressed = false;
			while (binding.wasPressed()) pressed = true;
			if (pressed && client.player != null && client.currentScreen == null
					&& ClientPlayNetworking.canSend(JiahaoTimeTogglePayload.ID)) {
				ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE);
			}
		});
	}
}
