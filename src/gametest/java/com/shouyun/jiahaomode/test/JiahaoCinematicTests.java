// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

/** A separate batch, since time stop is dimension-wide. Completion occurs after unfreezing. */
public final class JiahaoCinematicTests implements FabricGameTest {
    private static final List<Scenario> RUNNING = new ArrayList<>();
    static { ServerTickEvents.END_SERVER_TICK.register(server -> RUNNING.removeIf(test -> test.server == server && test.tick())); }
    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "jiahao_cinematic", tickLimit = 2000)
    public void cinematicLifecycle(TestContext context) { RUNNING.add(new Scenario(context)); }
    private static final class Scenario {
        final TestContext context;
        final MinecraftServer server;
        final JiahaoTransformationTests.TestPlayerConnection actor;
        JiahaoTransformationTests.TestPlayerConnection observer;
        final Vec3d origin;
        long start;
        int stage;
        com.shouyun.jiahaomode.cinematic.JiahaoPoseType lastPose;
        Scenario(TestContext context) {
            this.context = context; server = context.getWorld().getServer();
            actor = JiahaoTransformationTests.connectTestPlayer(server, context.getWorld(), "CinematicTest");
            var player = actor.player();
            BlockPos floor = context.getAbsolutePos(new BlockPos(1, 2, 1));
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                context.getWorld().setBlockState(floor.add(x, 0, z), Blocks.STONE.getDefaultState());
                for (int y = 1; y < 4; y++) context.getWorld().setBlockState(floor.add(x, y, z), Blocks.AIR.getDefaultState());
            }
            origin = Vec3d.ofBottomCenter(floor.up()); player.setPosition(origin);
            player.setOnGround(true); player.setVelocity(.2, 0, .1); player.getAbilities().flying = false;
            player.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER, List.of(JiahaoTimeStatePayload.ID.id()))));
            JiahaoStateManager.setJiahao(player, true);
            check(JiahaoTimeStopManager.isStableForCinematic(player), "Grounded fixture is stable");
            check(startWithPose(), "Start succeeds");
            check(JiahaoTimeStopManager.isCinematicLocked(player), "Stable owner enters cinematic");
            check(player.getVelocity().equals(Vec3d.ZERO), "Residual velocity cleared once");
            start = JiahaoTimeStopManager.getServerTick(server);
        }
        boolean tick() {
            try {
                var player = actor.player();
                long elapsed = JiahaoTimeStopManager.getServerTick(server) - start;
                if (stage == 0) {
                    if (elapsed < 100) {
                        check(JiahaoTimeStopManager.isCinematicLocked(player), "Cinematic remains active: " + elapsed);
                        player.move(MovementType.SELF, new Vec3d(.2, .1, 0)); player.jump();
                        player.networkHandler.onPlayerMove(new PlayerMoveC2SPacket.Full(origin.x+1, origin.y+1, origin.z, 90, 20, false));
                        check(player.getPos().squaredDistanceTo(origin) < .000001, "Travel and movement packets cannot move owner");
                        check(player.getVelocity().y <= 0, "Jump blocked");
                        if (elapsed == 40) {
                            var snapshots = actor.channel().outboundMessages().stream()
                                    .filter(packet -> packet instanceof CustomPayloadS2CPacket p && p.payload() instanceof JiahaoTimeStatePayload)
                                    .map(packet -> (JiahaoTimeStatePayload) ((CustomPayloadS2CPacket) packet).payload()).filter(JiahaoTimeStatePayload::active).toList();
                            check(snapshots.size() >= 3 && snapshots.stream().map(JiahaoTimeStatePayload::session).distinct().count() == 1,
                                    "Periodic refresh uses the same cinematic session");
                            observer = JiahaoTransformationTests.connectTestPlayer(server, context.getWorld(), "CinematicObserver");
                            observer.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(
                                    RegistrationPayload.REGISTER, List.of(JiahaoTimeStatePayload.ID.id()))));
                            JiahaoTimeStatePayload late = observer.channel().outboundMessages().stream()
                                    .filter(packet -> packet instanceof CustomPayloadS2CPacket p && p.payload() instanceof JiahaoTimeStatePayload)
                                    .map(packet -> (JiahaoTimeStatePayload) ((CustomPayloadS2CPacket) packet).payload()).filter(JiahaoTimeStatePayload::active)
                                    .findFirst().orElseThrow();
                            check(late.cinematic() && late.elapsedTicks() == 40 && late.owner().equals(player.getUuid())
                                    && late.session().equals(snapshots.getFirst().session()) && late.pose()==snapshots.getFirst().pose() && late.origin().equals(origin),
                                    "Late observer receives current pose time, same session, owner and anchor through existing S2C");
                        }
                    } else if (elapsed == 100) {
                        check(!JiahaoTimeStopManager.isCinematicLocked(player) && JiahaoTimeStopManager.isTimeStopped(context.getWorld()), "Only cinematic ends at 100");
                        player.jump(); check(player.getVelocity().y > 0, "Jump restores while world still frozen");
                        player.move(MovementType.SELF, new Vec3d(.1, 0, 0));
                        check(player.getX() > origin.x, "Travel restores");
                    } else if (elapsed == 300) {
                        check(!JiahaoTimeStopManager.isTimeStopped(context.getWorld()), "World resumes at 300"); stage = 1;
                    }
                } else if (stage == 1 && elapsed >= 460) {
                    player.setPosition(origin.add(0, 3, 0)); player.setOnGround(false); player.setVelocity(0,-.2,0);
                    check(startWithPose(), "Airborne time stop still succeeds");
                    check(!JiahaoTimeStopManager.isCinematicLocked(player), "Airborne start skips cinematic");
                    player.setPosition(origin); player.setOnGround(true); player.setVelocity(Vec3d.ZERO);
                    stage = 2; start = JiahaoTimeStopManager.getServerTick(server);
                } else if (stage == 2 && elapsed >= 10) {
                    check(!JiahaoTimeStopManager.isCinematicLocked(player), "Landing never starts a skipped sequence");
                    JiahaoTimeStopManager.stopTimeStop(player); stage = 3; start = JiahaoTimeStopManager.getServerTick(server);
                } else if (stage == 3 && elapsed >= 160) {
                    player.setPosition(origin); player.setOnGround(true); player.setVelocity(Vec3d.ZERO);
                    check(startWithPose() && JiahaoTimeStopManager.isCinematicLocked(player), "Next stable stop starts another sequence");
                    player.setPosition(origin.add(1,0,0)); stage = 4;
                } else if (stage == 4) {
                    check(!JiahaoTimeStopManager.isCinematicLocked(player) && JiahaoTimeStopManager.isTimeStopped(context.getWorld()), "External position change cancels cinematic alone");
                    JiahaoTimeStopManager.stopTimeStop(player); stage = 5; start = JiahaoTimeStopManager.getServerTick(server);
                } else if (stage == 5 && elapsed >= 160) {
                    player.setPosition(origin); player.setOnGround(true); player.setVelocity(Vec3d.ZERO);
                    check(startWithPose() && JiahaoTimeStopManager.isCinematicLocked(player), "Start for second-R test");
                    com.shouyun.jiahaomode.network.JiahaoTimeNetworking.handleToggle(player);
                    check(!JiahaoTimeStopManager.isCinematicLocked(player) && !JiahaoTimeStopManager.isTimeStopped(context.getWorld()), "Second R releases both states");
                    stage = 6; start = JiahaoTimeStopManager.getServerTick(server);
                } else if (stage == 6 && elapsed >= 160) {
                    player.setPosition(origin); player.setOnGround(true); player.setVelocity(Vec3d.ZERO);
                    check(startWithPose() && JiahaoTimeStopManager.isCinematicLocked(player), "Start for dimension test");
                    player.teleportTo(new net.minecraft.world.TeleportTarget(server.getWorld(net.minecraft.world.World.NETHER),
                            new Vec3d(0,80,0), Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));
                    check(!JiahaoTimeStopManager.isTimeStopped(context.getWorld()) && !JiahaoTimeStopManager.isCinematicLocked(player), "Dimension change releases cinematic");
                    player.teleportTo(new net.minecraft.world.TeleportTarget(context.getWorld(),origin,Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));
                    stage = 7; start = JiahaoTimeStopManager.getServerTick(server);
                } else if (stage == 7 && elapsed >= 160) {
                    player.setPosition(origin); player.setOnGround(true); player.setVelocity(Vec3d.ZERO);
                    check(startWithPose() && JiahaoTimeStopManager.isCinematicLocked(player), "Start for death test");
                    player.setHealth(0); player.onDeath(player.getServerWorld().getDamageSources().generic());
                    check(!JiahaoTimeStopManager.isCinematicLocked(player) && !JiahaoTimeStopManager.isTimeStopped(context.getWorld()), "Death releases both states");
                    stage = 8;
                } else if (stage == 8) {
                    server.getPlayerManager().remove(player); actor.channel().finishAndReleaseAll();
                    if (observer != null) { server.getPlayerManager().remove(observer.player()); observer.channel().finishAndReleaseAll(); }
                    context.runAtTick(context.getTick() + 1, context::complete); return true;
                }
                return false;
            } catch (Throwable failure) {
                JiahaoTimeStopManager.stopTimeStop(context.getWorld());
                server.getPlayerManager().remove(actor.player()); actor.channel().finishAndReleaseAll();
                if (observer != null) { server.getPlayerManager().remove(observer.player()); observer.channel().finishAndReleaseAll(); }
                context.runAtTick(context.getTick() + 1, () -> context.throwGameTestException(failure.toString())); return true;
            }
        }
        private boolean startWithPose() {
            if (!JiahaoTimeStopManager.startTimeStop(actor.player())) return false;
            var packet = actor.channel().outboundMessages().stream().filter(x -> x instanceof CustomPayloadS2CPacket cp && cp.payload() instanceof JiahaoTimeStatePayload)
                    .map(x -> (JiahaoTimeStatePayload)((CustomPayloadS2CPacket)x).payload()).filter(JiahaoTimeStatePayload::active).reduce((a,b)->b).orElseThrow();
            check(packet.pose()!=com.shouyun.jiahaomode.cinematic.JiahaoPoseType.DEFAULT && packet.pose()!=lastPose,"Server chooses a new non-repeating Pose");
            lastPose=packet.pose();return true;
        }
        private void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    }
}
