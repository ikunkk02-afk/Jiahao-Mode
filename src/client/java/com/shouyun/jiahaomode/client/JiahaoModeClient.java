package com.shouyun.jiahaomode.client;

import net.fabricmc.api.ClientModInitializer;

public class JiahaoModeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		JiahaoTimeClientNetworking.initialize();
		JiahaoTimeKeyBindings.initialize();
	}
}
