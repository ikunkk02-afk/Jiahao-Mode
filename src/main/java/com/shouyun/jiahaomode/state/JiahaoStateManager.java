// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.state;

import com.shouyun.jiahaomode.effect.JiahaoTransformationEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Public entry point for future Jiahao abilities; callers never need to access NBT. */
public final class JiahaoStateManager {
	private JiahaoStateManager() {
	}

	/** On clients this reads the server-synchronized copy, which may arrive a little later. */
	public static boolean isJiahao(PlayerEntity player) {
		return player.getAttachedOrElse(JiahaoState.FORM, false);
	}

	/** Changes authority state on the server thread; setting the same value has no effects. */
	public static void setJiahao(ServerPlayerEntity player, boolean enabled) {
		checkServerThread(player);
		if (isJiahao(player) == enabled) {
			return;
		}

		player.setAttached(JiahaoState.FORM, enabled);
		if (!enabled) com.shouyun.jiahaomode.dodge.JiahaoDodgeManager.clear(player, false);
		if (!enabled) com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.stopTimeStop(player);
		JiahaoTransformationEffects.onFormChanged(player, enabled);
		com.shouyun.jiahaomode.quote.JiahaoQuoteManager.formChanged(player, enabled);
	}

	/** Toggles authority state and returns the new value. */
	public static boolean toggleJiahao(ServerPlayerEntity player) {
		checkServerThread(player);
		boolean enabled = !isJiahao(player);
		setJiahao(player, enabled);
		return enabled;
	}

	private static void checkServerThread(ServerPlayerEntity player) {
		if (!player.getServerWorld().getServer().isOnThread()) {
			throw new IllegalStateException("Jiahao state must be changed on the server thread");
		}
	}
}
