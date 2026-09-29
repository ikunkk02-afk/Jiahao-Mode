// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.network.JiahaoTimeTogglePayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.GameRules;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.storage.StorageKey;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Real world simulation and packet handlers; the clock never uses frozen TestContext ticks. */
public final class JiahaoTimeStopTests implements FabricGameTest {
	private static final List<Scenario> RUNNING = new ArrayList<>();
	static {
		ServerTickEvents.END_SERVER_TICK.register(server -> RUNNING.removeIf(scenario -> scenario.server == server && scenario.tick()));
	}
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "jiahao_time_stop", tickLimit = 2400)
	public void timeStopLifecycle(TestContext context) {
		RUNNING.add(new Scenario(context));
	}
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "zz_shutdown_canary", tickLimit = 400)
	public void shutdownCanary(TestContext context) {
		var actor = JiahaoTransformationTests.connectTestPlayer(context.getWorld().getServer(), context.getWorld(), "StopCanary");
		JiahaoStateManager.setJiahao(actor.player(), true);
		context.assertTrue(JiahaoTimeStopManager.startTimeStop(actor.player()), "Shutdown canary must be active before actual server shutdown");
		context.assertTrue(!actor.player().writeNbt(new NbtCompound()).getCompound("fabric:attachments").contains("jiahao-mode:time_stop_view"),
				"Active time stop must never be saved in player attachments");
		JiahaoMode.LOGGER.info("Time-stop shutdown canary active; real SERVER_STOPPING must release it");
		context.complete();
	}

	private static final class Scenario {
		final TestContext context;
		final ServerWorld world;
		final MinecraftServer server;
		final JiahaoTransformationTests.TestPlayerConnection actor, observer, remote;
		final ServerPlayerEntity owner, other;
		final List<Entity> entities = new ArrayList<>();
		final List<ChunkPos> forcedChunks = new ArrayList<>();
		final Map<UUID, Integer> confirmedTeleports = new HashMap<>();
		final Map<Entity, Vec3d> frozenPositions = new HashMap<>();
		final Map<Entity, Integer> frozenAges = new HashMap<>();
		final BlockPos lamp, fluid, furnacePos, hopperPos, piston;
		final long randomTicks;
		JiahaoTransformationTests.TestPlayerConnection lateJoin;
		final ArrowEntity arrow;
		final TntEntity tnt;
		final ZombieEntity zombie;
		final ItemEntity item;
		final FurnaceBlockEntity furnace;
		final HopperBlockEntity hopper;
		final Vec3d arrowVelocity;
		Vec3d arrowPosition, itemPosition;
		long start, frozenTime, frozenDay, netherTime, endTime;
		int age, otherAge, ownerAge, itemAge, rainTime, stage, waits;

		Scenario(TestContext context) {
			this.context = context;
			world = context.getWorld(); server = world.getServer();
			world.getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(3, server);
			world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(true, server);
			context.assertTrue(!JiahaoTimeStopManager.isTimeStopped(world), "New test session must not inherit an active stop");
			actor = connect("TimeOwner"); observer = connect("TimeOther"); remote = connect("TimeRemote");
			owner = actor.player(); other = observer.player();
			ChunkPos first = new ChunkPos(pos(0, 0, 0)), last = new ChunkPos(pos(13, 0, 13));
			for (int x = first.x; x <= last.x; x++) for (int z = first.z; z <= last.z; z++) {
				ChunkPos chunk = new ChunkPos(x, z);
				if (!world.getForcedChunks().contains(chunk.toLong())) { world.setChunkForced(x, z, true); forcedChunks.add(chunk); }
			}
			// The empty 3x3 template does not clear terrain around our larger fixture.
			for (int x = 0; x < 14; x++) for (int z = 0; z < 14; z++) {
				world.setBlockState(pos(x, 0, z), Blocks.STONE.getDefaultState());
				for (int y = 1; y < 28; y++) world.setBlockState(pos(x, y, z), Blocks.AIR.getDefaultState(), 2);
			}
			place(actor, pos(4, 3, 4)); place(observer, pos(5, 3, 4));
			place(remote, pos(3, 3, 4));
			var probeChunk = (WorldChunk) world.getChunk(pos(4, 3, 4));
			int probeY = Math.floorDiv(pos(0, 32, 0).getY(), 16) * 16;
			for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
				world.setBlockState(new BlockPos(probeChunk.getPos().getStartX() + x, probeY + y, probeChunk.getPos().getStartZ() + z), JiahaoTimeTestBlocks.PROBE.getDefaultState(), 2);
			}
			randomTicks = JiahaoTimeTestBlocks.randomTicks;
			request(owner);
			check(!JiahaoTimeStopManager.isTimeStopped(world) && actor.hasActionBar("message.jiahao-mode.time.not_in_form"), "Normal R request must be refused");
			JiahaoStateManager.setJiahao(owner, true); JiahaoStateManager.setJiahao(other, true); JiahaoStateManager.setJiahao(remote.player(), true);
			ServerWorld nether = server.getWorld(World.NETHER);
			remote.player().teleportTo(new TeleportTarget(nether, new Vec3d(0, 90, 0), Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP));
			ack(remote);
			world.setTimeOfDay(12000);
			lamp = pos(10, 1, 10); fluid = pos(11, 1, 11); furnacePos = pos(2, 1, 2); hopperPos = pos(3, 1, 2);
			world.setBlockState(lamp, Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, true));
			world.scheduleBlockTick(lamp, Blocks.REDSTONE_LAMP, 20);
			world.setBlockState(fluid, Blocks.WATER.getDefaultState());
			world.setBlockState(furnacePos, Blocks.FURNACE.getDefaultState());
			furnace = (FurnaceBlockEntity) world.getBlockEntity(furnacePos);
			furnace.setStack(0, new ItemStack(Items.IRON_ORE)); furnace.setStack(1, new ItemStack(Items.COAL));
			world.setBlockState(hopperPos.down(), Blocks.CHEST.getDefaultState());
			world.setBlockState(hopperPos, Blocks.HOPPER.getDefaultState());
			hopper = (HopperBlockEntity) world.getBlockEntity(hopperPos); hopper.setStack(0, new ItemStack(Items.DIAMOND, 5));
			piston = pos(1, 1, 12);
			world.setBlockState(piston, Blocks.PISTON.getDefaultState().with(Properties.FACING, Direction.EAST));
			world.setBlockState(piston.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
			arrow = spawn(EntityType.ARROW, pos(7, 9, 7)); arrowVelocity = new Vec3d(0.15, 0.1, 0); arrow.setVelocity(arrowVelocity);
			check(world.getBlockState(arrow.getBlockPos()).isAir(), "Flight fixture must be in open air");
			tnt = spawn(EntityType.TNT, pos(8, 20, 8)); tnt.setFuse(20);
			zombie = spawn(EntityType.ZOMBIE, pos(6, 2, 6));
			item = new ItemEntity(world, pos(7, 5, 6).getX(), pos(7, 5, 6).getY(), pos(7, 5, 6).getZ(), new ItemStack(Items.DIAMOND));
			world.spawnEntity(item); entities.add(item); item.setVelocity(0.1, 0.2, 0);
			spawn(EntityType.EXPERIENCE_ORB, pos(8, 3, 7)); spawn(EntityType.BOAT, pos(9, 3, 7));
			spawn(EntityType.MINECART, pos(9, 3, 8)); spawn(EntityType.ARMOR_STAND, pos(10, 3, 7));
			spawn(EntityType.SNOWBALL, pos(7, 8, 7)); spawn(EntityType.TRIDENT, pos(8, 8, 7));
			owner.getInventory().setStack(0, new ItemStack(Items.DIAMOND_SWORD)); owner.getInventory().selectedSlot = 0;
			Entity boat = entities.stream().filter(e -> e.getType() == EntityType.BOAT).findFirst().orElseThrow();
			owner.startRiding(boat, true);
			request(owner);
			check(JiahaoTimeStopManager.isOwner(owner) && !owner.hasVehicle(), "Jiahao starts stop and automatically dismounts");
			place(actor, pos(4, 3, 4));
			check(actor.hasActionBar("message.jiahao-mode.time.started") && hasSnapshot(actor, true), "Start must send translated feedback and S2C state");
			start = JiahaoTimeStopManager.getServerTick(server);
			frozenTime = world.getTime(); frozenDay = world.getTimeOfDay(); netherTime = nether.getTime();
			endTime = server.getWorld(World.END).getTime();
			for (Entity entity : entities) { frozenPositions.put(entity, entity.getPos()); frozenAges.put(entity, entity.age); }
			arrowPosition = arrow.getPos(); itemPosition = item.getPos(); age = arrow.age; itemAge = item.getItemAge(); otherAge = other.age; ownerAge = owner.age;
			rainTime = ((ServerWorldProperties) world.getLevelProperties()).getRainTime();
			request(other);
			check(JiahaoTimeStopManager.isOwner(owner) && observer.hasActionBar("message.jiahao-mode.time.already_stopped"), "Second owner cannot replace the first");
			check(JiahaoTimeStopManager.startTimeStop(remote.player()), "Different dimensions can stop independently");
			check(JiahaoTimeStopManager.isTimeStopped(nether), "Nether has its own stop");
			JiahaoTimeStopManager.stopTimeStop(nether);
			check(JiahaoTimeStopManager.isTimeStopped(world) && !JiahaoTimeStopManager.isTimeStopped(nether), "Stopping Nether must not stop Overworld's owner");
			packetRestrictions();
			checkSavedScheduledDelay();
			codecRoundTrip();
		}

		boolean tick() {
			try {
				waits++;
				check(waits < 2000, "Scenario server-tick watchdog exceeded");
				long elapsed = JiahaoTimeStopManager.getServerTick(server) - start;
				if (stage == 0) {
					if (elapsed < 160) {
						check(JiahaoTimeStopManager.isTimeStopped(world), "Stop must remain active before deadline");
						check(world.getTime() == frozenTime && world.getTimeOfDay() == frozenDay, "Dimension game and sky times must be frozen");
						check(arrow.getPos().equals(arrowPosition) && arrow.getVelocity().equals(arrowVelocity) && arrow.age == age, "Flying arrow must retain position, velocity and age");
						check(tnt.getFuse() == 20 && !tnt.isRemoved(), "TNT fuse must remain 20");
						check(item.getPos().equals(itemPosition) && item.getItemAge() == itemAge, "Item movement and age must freeze");
						for (Entity entity : entities) check(entity.getPos().equals(frozenPositions.get(entity)) && entity.age == frozenAges.get(entity), "Entity tick must freeze: " + entity.getType());
						check(other.age == otherAge, "Other player simulation must freeze");
						check(world.isRaining() && ((ServerWorldProperties) world.getLevelProperties()).getRainTime() == rainTime, "Rain state and countdown must remain");
						check(furnace.getStack(1).getCount() == 1 && hopper.getStack(0).getCount() == 5, "Furnace and hopper must not tick");
						check(world.getBlockState(lamp).get(Properties.LIT), "Scheduled redstone tick must not run");
						check(world.getBlockState(fluid.east()).isAir(), "Water must not flow");
						check(!world.getBlockState(piston).get(Properties.EXTENDED), "Piston block events must freeze");
						check(JiahaoTimeTestBlocks.randomTicks == randomTicks, "Real chunk random ticks must freeze");
						check(world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE), "Daylight gamerule must remain unchanged");
						if (elapsed == 30) {
							check(owner.age > ownerAge && server.getWorld(World.NETHER).getTime() > netherTime && server.getWorld(World.END).getTime() > endTime, "Owner, Nether and End must continue ticking");
							check(actor.channel().outboundMessages().stream().anyMatch(packet -> packet instanceof CustomPayloadS2CPacket custom && custom.payload() instanceof JiahaoTimeStatePayload state && state.remainingTicks() == 140), "Remaining time synchronizes every 20 ticks");
							ack(actor); owner.networkHandler.syncWithPlayerPosition();
							Vec3d before = owner.getPos(); owner.jump();
							check(owner.getVelocity().y > 0, "Owner can jump");
							owner.networkHandler.onPlayerMove(new PlayerMoveC2SPacket.Full(before.x + 0.2, before.y + 0.3, before.z, 30, 10, false));
							check(owner.getX() > before.x && owner.getY() > before.y && owner.getYaw() == 30, "Owner movement/look: " + before + " -> " + owner.getPos() + " yaw=" + owner.getYaw());
							float health = zombie.getHealth(); owner.attack(zombie);
							check(zombie.getHealth() < health, "Owner damage is applied immediately");
							owner.networkHandler.onUpdateSelectedSlot(new UpdateSelectedSlotC2SPacket(1));
							check(owner.getInventory().selectedSlot == 1, "Owner can switch items");
							checkSavedScheduledDelay();
							lateJoin = connect("TimeLateJoin");
							check(hasSnapshot(lateJoin, true) && JiahaoTimeStopManager.shouldFreeze(lateJoin.player()), "Joining during a stop receives state and freezes");
						}
						return false;
					}
					check(elapsed == 160 && !JiahaoTimeStopManager.isTimeStopped(world), "Stop ends at exactly 160 server ticks");
					check(hasSnapshot(actor, false) && actor.hasActionBar("message.jiahao-mode.time.ended"), "Automatic end sync and feedback are sent");
					check(!arrow.getPos().equals(arrowPosition) && arrow.getVelocity().x > 0 && tnt.getFuse() == 19, "Arrow/TNT resume: position=" + arrow.getPos() + ", original=" + arrowPosition + ", velocity=" + arrow.getVelocity() + ", age=" + arrow.age + ", fuse=" + tnt.getFuse());
					for (Entity entity : entities) check(entity.age > frozenAges.get(entity), "Entity resumes ticking: " + entity.getType());
					check(((ServerWorldProperties) world.getLevelProperties()).getRainTime() < rainTime, "Weather countdown resumes");
					check(furnace.getStack(1).isEmpty() && hopper.getStack(0).getCount() < 5, "Block entities resume");
					check(world.getBlockState(piston).get(Properties.EXTENDED), "Piston block events resume");
					check(JiahaoTimeTestBlocks.randomTicks > randomTicks, "Real random ticks resume: " + JiahaoTimeTestBlocks.randomTicks + " <= " + randomTicks);
					check(world.getTime() == frozenTime + 1 && world.getTimeOfDay() == frozenDay + 1, "Clock resumes without catching up paused ticks");
					request(owner); check(observer.player().age > otherAge && actor.hasActionBar("message.jiahao-mode.time.cooldown"), "Other players resume and cooldown rejects start");
					stage = 1;
					return false;
				}
				if (stage == 1) {
					if (elapsed == 165) check(!world.getBlockState(fluid.east()).isAir(), "Fluid ticks resume");
					if (elapsed == 178) check(world.getBlockState(lamp).get(Properties.LIT), "Scheduled delay must not catch up immediately");
					if (elapsed == 180) {
						check(!world.getBlockState(lamp).get(Properties.LIT), "Scheduled block tick resumes after its remaining delay");
						check(tnt.isRemoved(), "TNT explodes only after its remaining 20 ticks");
					}
					if (elapsed == 219) check(!JiahaoTimeStopManager.startTimeStop(owner), "59 cooldown ticks still block start");
					if (elapsed < 220) return false;
					check(JiahaoTimeStopManager.startTimeStop(owner), "60 cooldown ticks allow start");
					request(owner); check(!JiahaoTimeStopManager.isTimeStopped(world), "Second R ends early");
					stage = 2; start = JiahaoTimeStopManager.getServerTick(server);
					return false;
				}
				if (stage == 2 && elapsed >= 60) {
					check(JiahaoTimeStopManager.startTimeStop(owner), "Start for form cancellation");
					JiahaoStateManager.setJiahao(owner, false);
					check(!JiahaoTimeStopManager.isTimeStopped(world), "Leaving form immediately restores world");
					JiahaoStateManager.setJiahao(owner, true);
					stage = 3; start = JiahaoTimeStopManager.getServerTick(server); return false;
				}
				if (stage == 3 && elapsed >= 60) {
					check(JiahaoTimeStopManager.startTimeStop(owner), "Start for dimension transition");
					owner.teleportTo(new TeleportTarget(server.getWorld(World.NETHER), new Vec3d(2, 90, 2), Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP));
					check(!JiahaoTimeStopManager.isTimeStopped(world), "Changing dimension releases origin immediately");
					owner.teleportTo(new TeleportTarget(world, pos(4, 3, 4).toCenterPos(), Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP)); ack(actor);
					stage = 4; start = JiahaoTimeStopManager.getServerTick(server); return false;
				}
				if (stage == 4 && elapsed >= 60) {
					check(JiahaoTimeStopManager.startTimeStop(owner), "Start for death");
					owner.setHealth(0); owner.onDeath(world.getDamageSources().generic());
					check(!JiahaoTimeStopManager.isTimeStopped(world), "Actual owner death releases world");
					check(JiahaoTimeStopManager.startTimeStop(other), "Other owner has independent cooldown");
					other.networkHandler.disconnect(net.minecraft.text.Text.literal("test disconnect"));
					check(!JiahaoTimeStopManager.isTimeStopped(world), "Actual disconnect releases world");
					check(JiahaoTimeStopManager.startTimeStop(remote.player()), "Start Nether for unload");
					net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.UNLOAD.invoker().onWorldUnload(server, server.getWorld(World.NETHER));
					check(!JiahaoTimeStopManager.isTimeStopped(server.getWorld(World.NETHER)), "Unload releases dimension");
					cleanup();
					JiahaoMode.LOGGER.info("Jiahao time-stop lifecycle, projectile, TNT, block, network and dimension checks passed");
					// Complete inside GameTestState.tick so its batch listeners are notified.
					context.runAtTick(context.getTick() + 1, context::complete); return true;
				}
				return false;
			} catch (Throwable failure) {
				JiahaoMode.LOGGER.error("Time-stop scenario failed at stage {} tick {}", stage, waits, failure);
				cleanup();
				context.runAtTick(context.getTick() + 1, () -> context.throwGameTestException(failure.toString()));
				return true;
			}
		}

		void packetRestrictions() {
			Vec3d position = other.getPos();
			other.networkHandler.onPlayerMove(new PlayerMoveC2SPacket.Full(position.x + 1, position.y + 1, position.z, 0, 0, false)); ack(observer);
			check(other.getPos().equals(position), "Frozen movement/jump cannot change position");
			other.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.JIAHAO_TRANSFORMER));
			other.networkHandler.onPlayerInteractItem(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 11, 0, 0));
			check(JiahaoStateManager.isJiahao(other), "Frozen player cannot use transformer");
			float health = zombie.getHealth();
			other.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.attack(zombie, false));
			check(zombie.getHealth() == health, "Frozen player cannot attack");
			other.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND, 5));
			other.networkHandler.onPlayerAction(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN, 12));
			check(other.getMainHandStack().getCount() == 5, "Frozen player cannot drop items");
			other.networkHandler.onCreativeInventoryAction(new CreativeInventoryActionC2SPacket(36, new ItemStack(Items.DIRT)));
			check(other.getMainHandStack().isOf(Items.DIAMOND), "Frozen creative actions cannot alter inventory");
			other.networkHandler.onClickSlot(new ClickSlotC2SPacket(other.currentScreenHandler.syncId, other.currentScreenHandler.getRevision(), 36, 0,
					net.minecraft.screen.slot.SlotActionType.THROW, ItemStack.EMPTY, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
			check(other.getMainHandStack().getCount() == 5, "Frozen container click cannot drop an inventory stack");
			other.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STONE));
			BlockPos target = pos(12, 0, 12);
			other.networkHandler.onPlayerInteractBlock(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(target.toCenterPos(), Direction.UP, target, false), 13));
			check(world.getBlockState(target.up()).isAir(), "Frozen player cannot place blocks");
			Vec3d velocity = zombie.getVelocity();
			for (int i = 0; i < 100; i++) zombie.pushAwayFrom(owner);
			check(zombie.getVelocity().equals(velocity), "Frozen entities must not accumulate collision velocity");
		}
		void checkSavedScheduledDelay() {
			WorldChunk chunk = (WorldChunk) world.getChunk(lamp);
			NbtCompound nbt = ChunkSerializer.serialize(world, chunk);
			// GameTest origins vary; these adjacent fixtures may straddle a chunk boundary.
			WorldChunk fluidChunk = (WorldChunk) world.getChunk(fluid);
			NbtCompound fluidNbt = ChunkSerializer.serialize(world, fluidChunk);
			int delay = delay(nbt, lamp);
			int fluidDelay = delay(fluidNbt, fluid, "fluid_ticks");
			check(delay == 20, "Saved scheduled tick must retain remaining delay during pause: " + delay);
			check(fluidDelay > 0, "Saved fluid tick must retain a positive remaining delay");
			NbtCompound restored = roundTripChunk(chunk, nbt);
			NbtCompound restoredFluid = roundTripChunk(fluidChunk, fluidNbt);
			check(delay(restored, lamp) == delay, "Chunk deserialize/serialize must retain remaining scheduled delay: " + delay(restored, lamp));
			check(delay(restoredFluid, fluid, "fluid_ticks") == fluidDelay, "Chunk reload retains remaining fluid delay");
		}
		NbtCompound roundTripChunk(WorldChunk chunk, NbtCompound nbt) {
			var proto = ChunkSerializer.deserialize(world, world.getChunkManager().getPointOfInterestStorage(),
					new StorageKey("time-stop-test", world.getRegistryKey(), "chunk"), chunk.getPos(), nbt);
			WorldChunk restoredChunk = proto instanceof WrapperProtoChunk wrapper ? wrapper.getWrappedChunk() : new WorldChunk(world, proto, null);
			return ChunkSerializer.serialize(world, restoredChunk);
		}
		int delay(NbtCompound nbt, BlockPos position) {
			return delay(nbt, position, "block_ticks");
		}
		int delay(NbtCompound nbt, BlockPos position, String key) {
			for (NbtElement element : nbt.getList(key, NbtElement.COMPOUND_TYPE)) {
				NbtCompound tick = (NbtCompound) element;
				if (tick.getInt("x") == position.getX() && tick.getInt("y") == position.getY() && tick.getInt("z") == position.getZ()) return tick.getInt("t");
			}
			return -1;
		}
		void codecRoundTrip() {
			var original = new JiahaoTimeStatePayload(world.getRegistryKey().getValue(), true, owner.getUuid(), 160, frozenTime, frozenDay,
					UUID.randomUUID(), 0, true, owner.getPos(), owner.getYaw());
			RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), server.getRegistryManager());
			try { JiahaoTimeStatePayload.CODEC.encode(buf, original); check(JiahaoTimeStatePayload.CODEC.decode(buf).equals(original), "S2C codec round trip"); }
			finally { buf.release(); }
		}
		boolean hasSnapshot(JiahaoTransformationTests.TestPlayerConnection connection, boolean active) {
			return connection.channel().outboundMessages().stream().anyMatch(packet -> packet instanceof CustomPayloadS2CPacket custom
					&& custom.payload() instanceof JiahaoTimeStatePayload payload && payload.active() == active && payload.dimension().equals(world.getRegistryKey().getValue()));
		}
		JiahaoTransformationTests.TestPlayerConnection connect(String name) {
			var connection = JiahaoTransformationTests.connectTestPlayer(server, world, name);
			connection.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER, List.of(JiahaoTimeStatePayload.ID.id()))));
			return connection;
		}
		void place(JiahaoTransformationTests.TestPlayerConnection connection, BlockPos position) {
			connection.player().teleport(world, position.getX() + 0.5, position.getY(), position.getZ() + 0.5, 0, 0);
			connection.player().getAbilities().flying = true; ack(connection);
		}
		void ack(JiahaoTransformationTests.TestPlayerConnection connection) {
			PlayerPositionLookS2CPacket latest = null;
			for (Object packet : connection.channel().outboundMessages()) if (packet instanceof PlayerPositionLookS2CPacket position) latest = position;
			if (latest != null && !Integer.valueOf(latest.getTeleportId()).equals(confirmedTeleports.get(connection.player().getUuid()))) {
				connection.player().networkHandler.onTeleportConfirm(new TeleportConfirmC2SPacket(latest.getTeleportId()));
				confirmedTeleports.put(connection.player().getUuid(), latest.getTeleportId());
			}
		}
		<E extends Entity> E spawn(EntityType<E> type, BlockPos position) {
			E entity = type.create(world); entity.refreshPositionAndAngles(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, 0, 0);
			world.spawnEntity(entity); entities.add(entity); return entity;
		}
		BlockPos pos(int x, int y, int z) { return context.getAbsolutePos(new BlockPos(x, y, z)); }
		void request(ServerPlayerEntity player) { player.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(JiahaoTimeTogglePayload.INSTANCE)); }
		void check(boolean condition, String message) { context.assertTrue(condition, message); }
		void cleanup() {
			JiahaoTimeStopManager.stopTimeStop(world); JiahaoTimeStopManager.stopTimeStop(server.getWorld(World.NETHER));
			for (Entity entity : entities) entity.discard();
			for (var connection : List.of(actor, observer, remote)) {
				if (server.getPlayerManager().getPlayer(connection.player().getUuid()) != null) server.getPlayerManager().remove(connection.player());
				connection.channel().finishAndReleaseAll();
			}
			if (lateJoin != null) { server.getPlayerManager().remove(lateJoin.player()); lateJoin.channel().finishAndReleaseAll(); }
			for (ChunkPos chunk : forcedChunks) world.setChunkForced(chunk.x, chunk.z, false);
		}
	}
}
