package com.shouyun.jiahaomode.client;

import net.fabricmc.api.ClientModInitializer;

public class JiahaoModeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		com.shouyun.jiahaomode.client.gadget.JiahaoGadgetClient.initialize();
		com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController.initialize();
		JiahaoTimeClientNetworking.initialize();
		JiahaoTimeKeyBindings.initialize();
		JiahaoQuoteClientState.initialize();
		JiahaoQuoteKeyBindings.initialize();
		JiahaoSubtitleRenderer.initialize();
		JiahaoSpeechBubbleRenderer.initialize();
		JiahaoDodgeClientController.initialize();
		JiahaoDodgeKeyBindings.initialize();
		JiahaoMomentClient.initialize();
		HaoClient.initialize();
		com.shouyun.jiahaomode.client.gadget.HaoGadgetClient.initialize();
	}
}
