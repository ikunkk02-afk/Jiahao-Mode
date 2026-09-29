// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.timestop;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.quote.JiahaoQuoteManager;
import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** All writes are server-thread-only; no player/world references or persistent active state. */
public final class JiahaoTimeStopManager {
	public static final int MAX_DURATION_TICKS = 300;
	public static final int COOLDOWN_TICKS = 160;
	public static final int CINEMATIC_TICKS = 100;
	private static final UUID NO_SESSION = new UUID(0, 0);
	private static final Map<MinecraftServer, ServerRuntime> SERVERS = Collections.synchronizedMap(new WeakHashMap<>());

	private JiahaoTimeStopManager() { }

	public static void initialize() {
		// Register the non-persistent client attachment before any world loads.
		JiahaoTimeView.initialize();
		ServerTickEvents.START_SERVER_TICK.register(JiahaoTimeStopManager::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity player) stopOwned(player, "owner died");
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> stopOwned(handler.player, "owner disconnected"));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncPlayer(handler.player));
		S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
			if (channels.contains(JiahaoTimeStatePayload.ID.id())) syncPlayer(handler.player);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			stopOwned(player, "owner changed dimension");
			syncPlayer(player);
		});
		ServerWorldEvents.UNLOAD.register((server, world) -> stop(world, "world unloaded"));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerWorld world : server.getWorlds()) stop(world, "server stopping");
		});
		// Keep clock offsets through the final chunk save so scheduled delays serialize correctly.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVERS.remove(server));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
				isTimeStopped(world) && shouldFreeze(entity) ? ActionResult.FAIL : ActionResult.PASS);
	}

	public static boolean isTimeStopped(World world) {
		if (world instanceof ServerWorld serverWorld) return isTimeStopped(serverWorld);
		return world.isClient() && world.getAttachedOrElse(JiahaoTimeView.CLIENT_VIEW, JiahaoTimeView.INACTIVE).active();
	}
	public static boolean isTimeStopped(ServerWorld world) {
		DimensionRuntime dimension = dimensionOrNull(world);
		return dimension != null && dimension.active != null;
	}
	public static UUID getOwner(World world) {
		if (world instanceof ServerWorld serverWorld) return getOwner(serverWorld);
		return world.getAttachedOrElse(JiahaoTimeView.CLIENT_VIEW, JiahaoTimeView.INACTIVE).owner();
	}
	public static UUID getOwner(ServerWorld world) {
		DimensionRuntime dimension = dimensionOrNull(world);
		return dimension == null || dimension.active == null ? null : dimension.active.owner;
	}
	public static boolean isOwner(ServerPlayerEntity player) {
		return player.getUuid().equals(getOwner(player.getServerWorld()));
	}
	public static boolean shouldFreeze(Entity entity) {
		World world = entity.getWorld();
		return isTimeStopped(world) && !entity.getUuid().equals(getOwner(world));
	}
	public static int getRemainingTicks(ServerWorld world) {
		DimensionRuntime dimension = dimensionOrNull(world);
		return dimension == null || dimension.active == null ? 0
				: (int) Math.max(0, dimension.active.endTick - runtime(world.getServer()).tick);
	}
	public static long getServerTick(MinecraftServer server) { return runtime(server).tick; }
	public static boolean isCinematicLocked(Entity entity) {
		if (entity.getWorld() instanceof ServerWorld world) {
			DimensionRuntime dimension = dimensionOrNull(world);
			TimeStopState state = dimension == null ? null : dimension.active;
			return state != null && state.cinematic && state.owner.equals(entity.getUuid())
					&& getServerTick(world.getServer()) - state.startTick < CINEMATIC_TICKS;
		}
		JiahaoTimeView view = entity.getWorld().getAttachedOrElse(JiahaoTimeView.CLIENT_VIEW, JiahaoTimeView.INACTIVE);
		return view.active() && view.cinematicLocked() && entity.getUuid().equals(view.owner());
	}
	public static boolean isStableForCinematic(ServerPlayerEntity player) {
		return player.isAlive() && !player.isRemoved() && player.isOnGround() && !player.hasVehicle()
				&& player.getPose() == net.minecraft.entity.EntityPose.STANDING && !player.isTouchingWater()
				&& !player.isInLava() && !player.isFallFlying() && !player.getAbilities().flying && !player.isSpectator()
				&& Math.abs(player.getVelocity().y) < 0.12
				&& !player.getWorld().isSpaceEmpty(player, player.getBoundingBox().offset(0, -0.05, 0));
	}

	public static boolean startTimeStop(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		checkThread(world.getServer());
		if (!JiahaoStateManager.isJiahao(player)) {
			message(player, "not_in_form");
			return false;
		}
		if (!player.isAlive() || player.isRemoved()) return false;
		ServerRuntime runtime = runtime(world.getServer());
		DimensionRuntime dimension = runtime.dimensions.computeIfAbsent(world.getRegistryKey(), key -> new DimensionRuntime());
		if (dimension.active != null) {
			message(player, "already_stopped");
			return false;
		}
		if (runtime.cooldowns.getOrDefault(player.getUuid(), 0L) > runtime.tick) {
			message(player, "cooldown");
			return false;
		}
		player.stopRiding();
		com.shouyun.jiahaomode.moment.JiahaoMomentManager.cancelWorld(world);
		var pose = com.shouyun.jiahaomode.cinematic.JiahaoPoseType.timeStop(runtime.lastPoses.get(player.getUuid()), bound -> player.getRandom().nextInt(bound));
		runtime.lastPoses.put(player.getUuid(), pose);
		dimension.active = new TimeStopState(player.getUuid(), runtime.tick, runtime.tick + MAX_DURATION_TICKS,
				world.getTime(), world.getTimeOfDay(), isStableForCinematic(player), player.getPos(), player.getYaw(), pose);
		if (dimension.active.cinematic) {
			player.setVelocity(Vec3d.ZERO); player.velocityModified = true;
			player.stopUsingItem(); player.setSprinting(false);
			// One initial reconciliation, never a per-tick teleport loop.
			player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
		}
		for (ServerPlayerEntity other : world.getPlayers()) {
			if (shouldFreeze(other)) lockPlayer(other, dimension);
			message(other, "started");
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
				SoundCategory.PLAYERS, 0.8F, 0.65F);
		syncWorld(world);
		JiahaoQuoteManager.timeStarted(player, dimension.active.session, dimension.active.cinematic);
		JiahaoMode.LOGGER.info("Jiahao time stop started by {} in {}", player.getGameProfile().getName(), world.getRegistryKey().getValue());
		return true;
	}

	public static void stopTimeStop(ServerPlayerEntity player) { stopOwned(player, "owner toggled or left form"); }
	public static void stopTimeStop(ServerWorld world) { stop(world, "requested"); }

	private static void stopOwned(ServerPlayerEntity player, String reason) {
		MinecraftServer server = player.getServerWorld().getServer();
		checkThread(server);
		ServerRuntime runtime = SERVERS.get(server);
		if (runtime == null) return;
		for (Map.Entry<RegistryKey<World>, DimensionRuntime> entry : runtime.dimensions.entrySet()) {
			TimeStopState active = entry.getValue().active;
			if (active != null && active.owner.equals(player.getUuid())) {
				ServerWorld world = server.getWorld(entry.getKey());
				if (world != null) stop(world, reason);
				else clearMissingWorld(runtime, entry.getValue());
			}
		}
	}
	private static void stop(ServerWorld world, String reason) {
		checkThread(world.getServer());
		DimensionRuntime dimension = dimensionOrNull(world);
		if (dimension == null || dimension.active == null) return;
		TimeStopState active = dimension.active;
		// Raw shared properties keep ticking. Each dimension resumes at its own frozen instant.
		dimension.gameOffset = world.getLevelProperties().getTime() - active.gameTime;
		dimension.dayOffset = world.getLevelProperties().getTimeOfDay() - active.dayTime;
		dimension.active = null;
		dimension.players.clear();
		runtime(world.getServer()).cooldowns.put(active.owner, runtime(world.getServer()).tick + COOLDOWN_TICKS);
		for (ServerPlayerEntity player : world.getPlayers()) message(player, "ended");
		ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(active.owner);
		if (owner != null) {
			JiahaoQuoteManager.timeEnded(owner, active.session, (reason.equals("duration elapsed") || reason.equals("owner toggled or left form")) && JiahaoStateManager.isJiahao(owner));
			if (owner.getServerWorld() != world) message(owner, "ended");
			world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
					SoundCategory.PLAYERS, 0.4F, 1.3F);
		}
		syncWorld(world);
		JiahaoMode.LOGGER.info("Jiahao time stop ended in {}: {}", world.getRegistryKey().getValue(), reason);
	}

	private static void tick(MinecraftServer server) {
		ServerRuntime runtime = runtime(server);
		runtime.tick++;
		for (Map.Entry<RegistryKey<World>, DimensionRuntime> entry : new ArrayList<>(runtime.dimensions.entrySet())) {
			TimeStopState active = entry.getValue().active;
			if (active == null) continue;
			ServerWorld world = server.getWorld(entry.getKey());
			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(active.owner);
			if (world == null) { clearMissingWorld(runtime, entry.getValue()); continue; }
			if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.getServerWorld() != world
					|| !JiahaoStateManager.isJiahao(owner)) {
				stop(world, "owner no longer valid");
			} else if (runtime.tick >= active.endTick) {
				stop(world, "duration elapsed");
			} else {
				if (active.cinematic && (runtime.tick - active.startTick >= CINEMATIC_TICKS
						|| !isStableForCinematic(owner) || owner.getPos().squaredDistanceTo(active.origin) > 0.0025)) {
					active.cinematic = false;
					JiahaoQuoteManager.cinematicStopped(owner, active.session);
					syncWorld(world);
				}
				if (active.cinematic) owner.setVelocity(Vec3d.ZERO);
				for (ServerPlayerEntity player : world.getPlayers()) if (shouldFreeze(player)) lockPlayer(player, entry.getValue());
				if ((runtime.tick - active.startTick) % 20 == 0) syncWorld(world);
			}
		}
		if (runtime.tick % 20 == 0) runtime.cooldowns.entrySet().removeIf(entry -> entry.getValue() <= runtime.tick);
	}
	private static void clearMissingWorld(ServerRuntime runtime, DimensionRuntime dimension) {
		if (dimension.active != null) runtime.cooldowns.put(dimension.active.owner, runtime.tick + COOLDOWN_TICKS);
		dimension.active = null;
		dimension.players.clear();
	}

	/** Returns effective dimension time without recursively reading World.getTime(). */
	public static long effectiveTime(ServerWorld world, long rawTime, boolean day) {
		DimensionRuntime dimension = dimensionOrNull(world);
		if (dimension == null) return rawTime;
		TimeStopState active = dimension.active;
		return active == null ? rawTime - (day ? dimension.dayOffset : dimension.gameOffset)
				: (day ? active.dayTime : active.gameTime);
	}

	private static FrozenPlayer lockPlayer(ServerPlayerEntity player, DimensionRuntime dimension) {
		return dimension.players.computeIfAbsent(player.getUuid(), key -> {
			player.networkHandler.syncWithPlayerPosition();
			return new FrozenPlayer(player.getPos(), player.getYaw(), player.getPitch());
		});
	}
	/** Null means no correction; movement packets can never move a frozen player. */
	public static FrozenPlayer movementCorrection(ServerPlayerEntity player) {
		if (!shouldFreeze(player)) return null;
		DimensionRuntime dimension = dimensionOrNull(player.getServerWorld());
		FrozenPlayer lock = lockPlayer(player, dimension);
		long tick = getServerTick(player.getServerWorld().getServer());
		if (lock.lastCorrection == tick) return null;
		lock.lastCorrection = tick;
		return lock;
	}
	public static void syncPlayer(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		if (shouldFreeze(player)) lockPlayer(player, dimensionOrNull(world));
		if (ServerPlayNetworking.canSend(player, JiahaoTimeStatePayload.ID)) {
			DimensionRuntime dimension = dimensionOrNull(world);
			TimeStopState state = dimension == null ? null : dimension.active;
			ServerPlayNetworking.send(player, new JiahaoTimeStatePayload(world.getRegistryKey().getValue(), isTimeStopped(world),
					getOwner(world), getRemainingTicks(world), world.getTime(), world.getTimeOfDay(),
					state == null ? NO_SESSION : state.session,
					state == null ? 0 : (int) (getServerTick(world.getServer()) - state.startTick),
					state != null && state.cinematic, state == null ? Vec3d.ZERO : state.origin, state == null ? 0 : state.yaw,
					state == null ? com.shouyun.jiahaomode.cinematic.JiahaoPoseType.DEFAULT : state.pose));
		}
		player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(),
				world.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE)));
	}
	private static void syncWorld(ServerWorld world) { for (ServerPlayerEntity player : world.getPlayers()) syncPlayer(player); }
	private static void message(ServerPlayerEntity player, String suffix) {
		Text body = Text.translatable("message.jiahao-mode.time." + suffix);
		player.sendMessage(suffix.equals("started") || suffix.equals("ended")
				? Text.literal("[").append(Text.translatable("message.jiahao-mode.prefix")).append("] ").append(body) : body, true);
	}
	private static ServerRuntime runtime(MinecraftServer server) {
		synchronized (SERVERS) { return SERVERS.computeIfAbsent(server, key -> new ServerRuntime()); }
	}
	private static DimensionRuntime dimensionOrNull(ServerWorld world) {
		ServerRuntime runtime = SERVERS.get(world.getServer());
		return runtime == null ? null : runtime.dimensions.get(world.getRegistryKey());
	}
	private static void checkThread(MinecraftServer server) {
		if (!server.isOnThread()) throw new IllegalStateException("Jiahao time stop must change on the server thread");
	}
	private static final class ServerRuntime {
		volatile long tick;
		final Map<RegistryKey<World>, DimensionRuntime> dimensions = new ConcurrentHashMap<>();
		final Map<UUID, Long> cooldowns = new HashMap<>();
		final Map<UUID, com.shouyun.jiahaomode.cinematic.JiahaoPoseType> lastPoses = new HashMap<>();
	}
	private static final class DimensionRuntime {
		volatile TimeStopState active;
		volatile long gameOffset;
		volatile long dayOffset;
		final Map<UUID, FrozenPlayer> players = new HashMap<>();
	}
	private static final class TimeStopState {
		final UUID owner, session = UUID.randomUUID();
		final long startTick, endTick, gameTime, dayTime;
		final Vec3d origin;
		final float yaw;
		final com.shouyun.jiahaomode.cinematic.JiahaoPoseType pose;
		boolean cinematic;
		TimeStopState(UUID owner, long startTick, long endTick, long gameTime, long dayTime, boolean cinematic, Vec3d origin, float yaw, com.shouyun.jiahaomode.cinematic.JiahaoPoseType pose) {
			this.owner = owner; this.startTick = startTick; this.endTick = endTick;
			this.gameTime = gameTime; this.dayTime = dayTime; this.cinematic = cinematic; this.origin = origin; this.yaw = yaw; this.pose = pose;
		}
	}
	public static final class FrozenPlayer {
		public final Vec3d position;
		public final float yaw;
		public final float pitch;
		private long lastCorrection = Long.MIN_VALUE;
		private FrozenPlayer(Vec3d position, float yaw, float pitch) { this.position = position; this.yaw = yaw; this.pitch = pitch; }
	}
}
