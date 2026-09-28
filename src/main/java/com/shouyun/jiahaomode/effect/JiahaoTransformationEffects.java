// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.effect;

import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** One-shot server effects; loading or copying saved attachments never invokes these. */
public final class JiahaoTransformationEffects {
	private static final int RAIN_DURATION_TICKS = 5 * 60 * 20;

	private JiahaoTransformationEffects() {
	}

	public static void onFormChanged(ServerPlayerEntity player, boolean enabled) {
		ServerWorld world = player.getServerWorld();
		if (enabled) {
			startRain(world);
		}

		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				enabled ? SoundEvents.BLOCK_BEACON_ACTIVATE : SoundEvents.BLOCK_BEACON_DEACTIVATE,
				SoundCategory.PLAYERS, enabled ? 1.0F : 0.5F, 1.0F);

		player.sendMessage(Text.empty()
				.append(Text.literal("[").formatted(Formatting.GRAY))
				.append(Text.translatable("message.jiahao-mode.prefix").formatted(Formatting.WHITE))
				.append(Text.literal("] ").formatted(Formatting.GRAY))
				.append(Text.translatable(enabled
						? "message.jiahao-mode.activated"
						: "message.jiahao-mode.deactivated")
						.formatted(enabled ? Formatting.WHITE : Formatting.GRAY)), true);
	}

	private static void startRain(ServerWorld world) {
		// Nether / End cannot display rain. Do not redirect their transformations to the Overworld.
		if (!world.getDimension().hasSkyLight()) {
			return;
		}

		world.setWeather(0, RAIN_DURATION_TICKS, true, false);
		// Apply visible rain immediately instead of waiting for vanilla's gradual weather transition.
		world.setRainGradient(1.0F);
		world.setThunderGradient(0.0F);

		// Both setters also update the previous gradients, so vanilla's next weather
		// tick sees no change to broadcast. Send vanilla packets once to this dimension.
		var players = world.getServer().getPlayerManager();
		players.sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0.0F), world.getRegistryKey());
		players.sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1.0F), world.getRegistryKey());
		players.sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0.0F), world.getRegistryKey());
	}
}
