// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.client.mixin.WorldRendererTimeAccess;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.network.JiahaoTimeTogglePayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/** Optional real client/renderer test, run only with scripts/weather-visual-smoke.gradle. */
public final class WeatherVisualSmoke implements ClientModInitializer {
	private int stage, ticks, stoppedTicks, cycles, startRendererTicks;
	private ProbeParticle ordinary, immune;
	private double frozenTime;
	private float rain, thunder, sky;
	private int ordinaryTicks, flash;
	@Override public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(this::tick); }
	private void tick(MinecraftClient client) {
		try {
			ticks++;
			if (ticks > 2400) throw new AssertionError("Client smoke watchdog expired");
			if (stage == 0) {
				if (client.world == null || client.player == null || client.getServer() == null || ticks < 100) return;
				client.options.getCloudRenderMode().setValue(CloudRenderMode.FANCY);
				client.options.getHideLightningFlashes().setValue(false);
				client.options.pauseOnLostFocus = false;
				client.setScreen(null);
				configure(client, false); stage = 1; ticks = 0; return;
			}
			if (stage == 1) {
				if (ticks < 100 || !JiahaoStateManager.isJiahao(client.player)) return;
				Biome.Precipitation expected = cycles == 0 ? Biome.Precipitation.RAIN : Biome.Precipitation.SNOW;
				check(client.world.getBiome(client.player.getBlockPos()).value().getPrecipitation(client.player.getBlockPos()) == expected, "Actual client biome precipitation must match " + expected);
				client.world.setRainGradient(0.72F); client.world.setThunderGradient(0.4F); client.world.setLightningTicksLeft(2);
				ordinary = new ProbeParticle(client.world, client.player.getX(), client.player.getY(), client.player.getZ());
				immune = JiahaoTimeStopClientState.markTimeStopImmune(new ProbeParticle(client.world, client.player.getX(), client.player.getY(), client.player.getZ()));
				client.particleManager.addParticle(ordinary); client.particleManager.addParticle(immune);
				ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE); stage = 2; ticks = 0; return;
			}
			if (stage == 2) {
				if (!JiahaoTimeStopClientState.isTimeStopped(client.world)) { check(ticks < 80, "S2C start must reach client"); return; }
				frozenTime = JiahaoTimeStopClientState.getWorldAnimationTime();
				rain = client.world.getRainGradient(0); thunder = client.world.getThunderGradient(0); sky = client.world.getSkyAngle(0);
				ordinaryTicks = ordinary.ticks; flash = client.world.getLightningTicksLeft();
				startRendererTicks = ((WorldRendererTimeAccess) client.worldRenderer).jiahao$getRendererTicks();
				stoppedTicks = 0; stage = 3; return;
			}
			if (stage == 3) {
				if (JiahaoTimeStopClientState.isTimeStopped(client.world)) {
					stoppedTicks++;
					near(JiahaoTimeStopClientState.getWorldAnimationTime(), frozenTime, "Rain/snow/cloud clock must freeze");
					near(client.world.getRainGradient(0.9F), rain, "Rain gradient is independent of frame delta");
					near(client.world.getThunderGradient(0.9F), thunder, "Thunder gradient must freeze");
					near(client.world.getSkyAngle(0.9F), sky, "Sun/moon/stars angle must freeze");
					check(ordinary.ticks == ordinaryTicks, "Existing ordinary particle simulation must freeze");
					check(client.world.getLightningTicksLeft() == flash, "Lightning flash countdown must freeze");
					if (ordinary.frames > 0) near(ordinary.lastDelta, JiahaoTimeStopClientState.frozenTickDelta(), "Particle geometry must receive frozen interpolation delta");
					if (stoppedTicks == 25) { client.player.setYaw(client.player.getYaw() + 45); client.player.setPos(client.player.getX() + 0.2, client.player.getY(), client.player.getZ()); }
					if (stoppedTicks == 40) { client.world.setRainGradient(0.95F); client.world.setThunderGradient(0.95F); }
					return;
				}
				check(stoppedTicks > 120, "Full automatic stop must survive real client ticks");
				check(((WorldRendererTimeAccess) client.worldRenderer).jiahao$getRendererTicks() > startRendererTicks + 120, "Real renderer ticks continue");
				check(immune.ticks > ordinaryTicks + 100, "Immune skill particle keeps moving");
				check(ordinary.frames > 20 && immune.frames > 20, "Particle render path remains active");
				check(Math.abs(JiahaoTimeStopClientState.getWorldAnimationTime() - frozenTime) < 2, "Release cannot jump 8 seconds");
				stage = 4; ticks = 0; return;
			}
			if (stage == 4 && ticks >= 80) {
				check(ordinary.ticks > ordinaryTicks, "Particles resume after release");
				check(JiahaoTimeStopClientState.getWorldAnimationTime() > frozenTime + 40, "Visual animation resumes advancing");
				if (cycles++ == 0) { configure(client, true); stage = 1; ticks = 0; return; }
				// The vanilla disconnect screen waits for the integrated server to stop.
				// A test-driven exit must request this explicitly before entering that screen.
				stage = 5;
				client.getServer().stop(false);
				client.disconnect();
				check(!JiahaoTimeStopClientState.hasAnimationOffset(client.world), "Disconnect clears visual state");
				JiahaoMode.LOGGER.info("WEATHER VISUAL SMOKE PASSED: real rain/snow biomes, Fancy clouds clock, gradients, sky, particles/immunity, camera/player movement, 8-second resume, disconnect cleanup");
				client.scheduleStop();
			}
		} catch (Throwable failure) {
			JiahaoMode.LOGGER.error("WEATHER VISUAL SMOKE FAILED at stage {}", stage, failure);
			if (client.getServer() != null) client.getServer().stop(false);
			client.disconnect(); client.scheduleStop(); stage = 5;
		}
	}
	private void configure(MinecraftClient client, boolean snow) {
		var server = client.getServer(); var uuid = client.player.getUuid();
		BlockPos position = client.player.getBlockPos();
		server.execute(() -> {
			var player = server.getPlayerManager().getPlayer(uuid); var world = player.getServerWorld();
			equipArmor(player); JiahaoStateManager.setJiahao(player, true); world.setTimeOfDay(12000); world.setWeather(0, 10000, true, false);
			String biome = snow ? "snowy_plains" : "plains";
			String command = "fillbiome " + (position.getX() - 12) + " " + (position.getY() - 16) + " " + (position.getZ() - 12) + " " + (position.getX() + 12) + " " + (position.getY() + 16) + " " + (position.getZ() + 12) + " minecraft:" + biome;
			server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4), command);
		});
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
	private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < 0.00001, message + ": " + actual + " != " + expected); }
	private static final class ProbeParticle extends Particle {
		int ticks, frames; float lastDelta;
		ProbeParticle(ClientWorld world, double x, double y, double z) { super(world, x, y, z); setMaxAge(1200); setVelocity(0.001, 0.001, 0); }
		@Override public void tick() { ticks++; super.tick(); }
		@Override public void buildGeometry(VertexConsumer vertices, Camera camera, float delta) { frames++; lastDelta = delta; }
		@Override public ParticleTextureSheet getType() { return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT; }
	}

 private static void equipArmor(net.minecraft.server.network.ServerPlayerEntity p) {
  p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_HELMET));
  p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_CHESTPLATE));
  p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_LEGGINGS));
  p.equipStack(net.minecraft.entity.EquipmentSlot.FEET,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_BOOTS));
 }
}
