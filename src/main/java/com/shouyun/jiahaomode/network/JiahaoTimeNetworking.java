// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class JiahaoTimeNetworking {
	private JiahaoTimeNetworking() { }
	public static void initialize() {
		PayloadTypeRegistry.playC2S().register(JiahaoTimeTogglePayload.ID, JiahaoTimeTogglePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(JiahaoTimeStatePayload.ID, JiahaoTimeStatePayload.CODEC);
		// Fabric's typed receiver already runs on the server thread.
		ServerPlayNetworking.registerGlobalReceiver(JiahaoTimeTogglePayload.ID,
				(payload, context) -> handleToggle(context.player()));
	}
	public static void handleToggle(ServerPlayerEntity player) {
		if (JiahaoTimeStopManager.isOwner(player)) JiahaoTimeStopManager.stopTimeStop(player);
		else JiahaoTimeStopManager.startTimeStop(player);
	}
}
