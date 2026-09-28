// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.timestop.JiahaoTimeView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class JiahaoTimeClientNetworking {
	private JiahaoTimeClientNetworking() { }
	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(JiahaoTimeStatePayload.ID, (payload, context) -> {
			var world = context.client().world;
			if (world == null || !world.getRegistryKey().getValue().equals(payload.dimension())) return;
			world.setAttached(JiahaoTimeView.CLIENT_VIEW, new JiahaoTimeView(payload.active(), payload.owner(), payload.remainingTicks()));
			world.setTime(payload.gameTime());
			world.setTimeOfDay(payload.dayTime());
		});
	}
}
