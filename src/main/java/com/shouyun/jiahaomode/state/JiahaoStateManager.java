// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.state;

import com.shouyun.jiahaomode.effect.JiahaoTransformationEffects;
import com.shouyun.jiahaomode.armor.JiahaoArmorUtil;
import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Public entry point for future Jiahao abilities; callers never need to access NBT. */
public final class JiahaoStateManager {
	private JiahaoStateManager() {
	}

	/** On clients this reads the server-synchronized copy, which may arrive a little later. */
	public static boolean isJiahao(PlayerEntity player) {
		return storedForm(player) && JiahaoArmorUtil.isWearingFullJiahaoArmor(player);
	}

	private static boolean storedForm(PlayerEntity player) {
		return player.getAttachedOrElse(JiahaoState.FORM, false);
	}

	/** Registered before the time-stop clock; this runs even when player/world ticks are frozen. */
	public static void initialize() {
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (var player : server.getPlayerManager().getPlayerList()) validateArmor(player);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> validateArmor(handler.player));
		// Fabric transfers copy-on-death attachments in the default phase. Validate the resulting flag afterwards.
		var phase = JiahaoMode.id("armor_validation");
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, phase);
		ServerPlayerEvents.AFTER_RESPAWN.register(phase, (oldPlayer, player, alive) -> {
			validateArmor(player);
			com.shouyun.jiahaomode.quote.JiahaoQuoteManager.track(player);
		});
	}

	private static void validateArmor(ServerPlayerEntity player) {
		if (storedForm(player) && !JiahaoArmorUtil.isWearingFullJiahaoArmor(player)) setJiahao(player, false);
	}

	/** Changes authority state on the server thread; setting the same value has no effects. */
	public static void setJiahao(ServerPlayerEntity player, boolean enabled) {
		checkServerThread(player);
		if (enabled && !JiahaoArmorUtil.isWearingFullJiahaoArmor(player)) {
			validateArmor(player);
			return;
		}
		if (storedForm(player) == enabled) {
			return;
		}

		player.setAttached(JiahaoState.FORM, enabled);
		if (!enabled) com.shouyun.jiahaomode.dodge.JiahaoDodgeManager.clear(player, false);
		if (!enabled) com.shouyun.jiahaomode.moment.JiahaoMomentManager.clear(player, false);
		if (!enabled) com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.stopTimeStop(player);
		JiahaoTransformationEffects.onFormChanged(player, enabled);
		com.shouyun.jiahaomode.quote.JiahaoQuoteManager.formChanged(player, enabled);
	}

	/** Toggles authority state and returns the new value. */
	public static boolean toggleJiahao(ServerPlayerEntity player) {
		checkServerThread(player);
		boolean enabled = !storedForm(player);
		if (enabled && !JiahaoArmorUtil.isWearingFullJiahaoArmor(player)) {
			player.sendMessage(Text.translatable("message.jiahao-mode.armor_required"), true);
			return false;
		}
		setJiahao(player, enabled);
		return isJiahao(player);
	}

	private static void checkServerThread(ServerPlayerEntity player) {
		if (!player.getServerWorld().getServer().isOnThread()) {
			throw new IllegalStateException("Jiahao state must be changed on the server thread");
		}
	}
}
