package com.shouyun.jiahaomode;

import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.state.JiahaoState;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiahaoMode implements ModInitializer {
	public static final String MOD_ID = "jiahao-mode";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		JiahaoState.initialize();
		ModItems.initialize();
		com.shouyun.jiahaomode.state.JiahaoStateManager.initialize();
		com.shouyun.jiahaomode.network.JiahaoTimeNetworking.initialize();
		com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.initialize();
		com.shouyun.jiahaomode.network.JiahaoQuoteNetworking.initialize();
		com.shouyun.jiahaomode.quote.JiahaoQuoteTrigger.initialize();
		com.shouyun.jiahaomode.network.JiahaoDodgeNetworking.initialize();
		com.shouyun.jiahaomode.dodge.JiahaoDodgeManager.initialize();
		com.shouyun.jiahaomode.network.JiahaoMomentNetworking.initialize();
		com.shouyun.jiahaomode.moment.JiahaoMomentManager.initialize();
		LOGGER.info("Jiahao transformation system initialized");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
