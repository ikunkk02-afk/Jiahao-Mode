// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.mojang.authlib.GameProfile;
import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.level.ServerWorldProperties;

import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

/** Runs only in Loom's isolated GameTest server, never in the distributable mod. */
public final class JiahaoTransformationTests implements FabricGameTest {
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
	public void transformationLifecycle(TestContext context) {
		ServerWorld world = context.getWorld();
		MinecraftServer server = world.getServer();
		PlayerManager players = server.getPlayerManager();
		TestPlayerConnection actor = connectTestPlayer(server, world, "JiahaoTestA");
		TestPlayerConnection observer = connectTestPlayer(server, world, "JiahaoTestB");
		ServerPlayerEntity player = actor.player();
		ServerPlayerEntity other = observer.player();
		ServerWorldProperties weather = (ServerWorldProperties) world.getLevelProperties();
		world.setWeather(0, 12000, false, false);
		world.setRainGradient(0.0F);
		world.setThunderGradient(0.0F);

		context.assertTrue(Registries.ITEM.get(JiahaoMode.id("jiahao_transformer")) == ModItems.JIAHAO_TRANSFORMER,
				"The transformer must be registered for /give");
		context.assertTrue(!JiahaoStateManager.isJiahao(player) && !JiahaoStateManager.isJiahao(other),
				"New players must start with independent normal states");
		server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4),
				"give @s jiahao-mode:jiahao_transformer");
		context.assertTrue(player.getInventory().contains(new ItemStack(ModItems.JIAHAO_TRANSFORMER)),
				"The given transformer must be present in the player's inventory");
		ItemStack transformer = new ItemStack(ModItems.JIAHAO_TRANSFORMER);
		player.setStackInHand(Hand.MAIN_HAND, transformer);
		player.setStackInHand(Hand.OFF_HAND, new ItemStack(ModItems.JIAHAO_TRANSFORMER));
		float health = player.getHealth();
		long lightning = lightningCount(world);
		long syncsBefore = actor.syncPacketCount();
		long soundsBefore = actor.soundPacketCount();
		long weatherBefore = actor.weatherPacketCount();

		var result = transformer.use(world, player, Hand.MAIN_HAND);
		context.assertTrue(result.getResult() == ActionResult.CONSUME, "Server use must consume the interaction");
		context.assertTrue(JiahaoStateManager.isJiahao(player), "First use must activate Jiahao form");
		context.assertTrue(actor.syncPacketCount() > syncsBefore, "The server must send the owner's state update");
		context.assertTrue(actor.hasActionBar("message.jiahao-mode.activated"), "Activation must send a translated Action Bar message");
		context.assertTrue(actor.soundPacketCount() == soundsBefore + 1, "Activation must send one sound effect");
		context.assertTrue(!JiahaoStateManager.isJiahao(other), "Transforming one player must not transform another");
		context.assertTrue(result.getValue() == transformer && transformer.getCount() == 1 && !transformer.isDamageable(),
				"The transformer must remain intact and have no durability");
		context.assertTrue(world.isRaining() && !world.isThundering(), "Transformation must immediately start rain without thunder");
		context.assertTrue(actor.weatherPacketCount() == weatherBefore + 3 && observer.weatherPacketCount() >= 3,
				"Rain start, rain strength and thunder strength must be sent to players in this dimension");
		context.assertTrue(weather.getRainTime() == 6000 && weather.getThunderTime() == 6000,
				"Rain and non-thunder timers must last five minutes");
		context.assertTrue(player.getHealth() == health && lightningCount(world) == lightning,
				"Transformation must not create lightning or damage the player");

		// An idempotent setter must not restart weather; disabling must leave it alone.
		world.setWeather(0, 4321, true, false);
		long messagesBefore = actor.actionBarCount();
		soundsBefore = actor.soundPacketCount();
		weatherBefore = actor.weatherPacketCount();
		JiahaoStateManager.setJiahao(player, true);
		context.assertTrue(weather.getRainTime() == 4321, "Setting an existing state must not restart rain");
		context.assertTrue(actor.actionBarCount() == messagesBefore && actor.soundPacketCount() == soundsBefore,
				"Setting the same form must not replay feedback");
		context.assertTrue(actor.weatherPacketCount() == weatherBefore, "Setting the same form must not resend weather");
		transformer.use(world, player, Hand.MAIN_HAND);
		context.assertTrue(!JiahaoStateManager.isJiahao(player), "Second use must deactivate Jiahao form");
		context.assertTrue(actor.hasActionBar("message.jiahao-mode.deactivated"), "Deactivation must send a translated Action Bar message");
		context.assertTrue(weather.isRaining() && weather.getRainTime() == 4321, "Deactivation must preserve weather");
		context.assertTrue(actor.weatherPacketCount() == weatherBefore, "Deactivation must not send a weather reset");
		context.assertTrue(JiahaoStateManager.toggleJiahao(player), "Toggle must return the activated state");

		NbtCompound saved = player.writeNbt(new NbtCompound());
		context.assertTrue(saved.getCompound("fabric:attachments").getBoolean("jiahao-mode:jiahao_state"),
				"The form must be stored in player NBT");
		ServerPlayerEntity restored = new ServerPlayerEntity(server, world, player.getGameProfile(), player.getClientOptions());
		world.setWeather(0, 3456, false, false);
		restored.readNbt(saved);
		context.assertTrue(JiahaoStateManager.isJiahao(restored), "NBT loading must restore form");
		context.assertTrue(!weather.isRaining() && weather.getRainTime() == 3456, "Loading form must not trigger transformation weather");

		players.saveAllPlayerData();
		context.assertTrue(Files.isRegularFile(server.getSavePath(WorldSavePath.PLAYERDATA)
				.resolve(player.getUuidAsString() + ".dat")), "Player state must be saved on disk");
		ServerPlayerEntity reloaded = new ServerPlayerEntity(server, world, player.getGameProfile(), player.getClientOptions());
		context.assertTrue(players.loadPlayerData(reloaded).isPresent() && JiahaoStateManager.isJiahao(reloaded),
				"Disk player data must restore Jiahao form");

		ServerPlayerEntity respawned = players.respawnPlayer(player, false, Entity.RemovalReason.KILLED);
		context.assertTrue(JiahaoStateManager.isJiahao(respawned), "Death respawn must preserve form");
		context.assertTrue(!weather.isRaining(), "Respawn must not restart rain");
		ServerWorld nether = server.getWorld(World.NETHER);
		respawned.teleportTo(new TeleportTarget(nether, new Vec3d(0, 80, 0), Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP));
		context.assertTrue(JiahaoStateManager.isJiahao(respawned), "Changing dimensions must preserve form");
		JiahaoStateManager.setJiahao(respawned, false);
		JiahaoStateManager.setJiahao(respawned, true);
		context.assertTrue(!weather.isRaining(), "Nether transformation must not change Overworld weather");
		respawned.teleportTo(new TeleportTarget(world, new Vec3d(0, 80, 0), Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP));
		context.assertTrue(JiahaoStateManager.isJiahao(respawned), "Returning to the Overworld must preserve form");
		ServerPlayerEntity returned = players.respawnPlayer(respawned, true, Entity.RemovalReason.CHANGED_DIMENSION);
		context.assertTrue(JiahaoStateManager.isJiahao(returned), "Alive respawn used by End return must preserve form");

		context.assertTrue(CompletableFuture.supplyAsync(() -> {
			try {
				JiahaoStateManager.setJiahao(returned, false);
				return false;
			} catch (IllegalStateException expected) {
				return true;
			}
		}).join(), "Off-thread state changes must be rejected");
		context.assertTrue(JiahaoStateManager.isJiahao(returned), "Rejected writes must not alter state");
		players.remove(returned);
		players.remove(other);
		actor.channel().finishAndReleaseAll();
		observer.channel().finishAndReleaseAll();
		JiahaoMode.LOGGER.info("Jiahao integration lifecycle checks passed");
		context.complete();
	}

	private static long lightningCount(ServerWorld world) {
		return StreamSupport.stream(world.iterateEntities().spliterator(), false)
				.filter(LightningEntity.class::isInstance).count();
	}

	private static TestPlayerConnection connectTestPlayer(MinecraftServer server, ServerWorld world, String name) {
		ConnectedClientData data = ConnectedClientData.createDefault(new GameProfile(UUID.randomUUID(), name), false);
		ServerPlayerEntity player = new ServerPlayerEntity(server, world, data.gameProfile(), data.syncedOptions());
		ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		server.getPlayerManager().onPlayerConnect(connection, player, data);
		player.changeGameMode(GameMode.CREATIVE);
		return new TestPlayerConnection(player, channel);
	}

	private record TestPlayerConnection(ServerPlayerEntity player, EmbeddedChannel channel) {
		long syncPacketCount() {
			return channel.outboundMessages().stream()
					.filter(packet -> packet instanceof CustomPayloadS2CPacket custom
							&& custom.payload().getId().id().toString().equals("fabric:attachment_sync_v1"))
					.count();
		}

		long soundPacketCount() {
			return channel.outboundMessages().stream().filter(PlaySoundS2CPacket.class::isInstance).count();
		}

		long actionBarCount() {
			return channel.outboundMessages().stream()
					.filter(packet -> packet instanceof GameMessageS2CPacket message && message.overlay()).count();
		}

		long weatherPacketCount() {
			return channel.outboundMessages().stream()
					.filter(packet -> packet instanceof GameStateChangeS2CPacket change
							&& (change.getReason() == GameStateChangeS2CPacket.RAIN_STARTED
							|| change.getReason() == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED
							|| change.getReason() == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED))
					.count();
		}

		boolean hasActionBar(String key) {
			return channel.outboundMessages().stream()
					.anyMatch(packet -> packet instanceof GameMessageS2CPacket message && message.overlay()
							&& message.content().getSiblings().stream().anyMatch(text ->
							text.getContent() instanceof TranslatableTextContent translated && translated.getKey().equals(key)));
		}
	}
}
