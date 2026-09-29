// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.mojang.authlib.GameProfile;
import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.client.cinematic.*;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.network.JiahaoTimeTogglePayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Real render path, real existing C2S/S2C; never bundled with the mod. */
public final class CinematicSmoke implements ClientModInitializer {
    private int stage, ticks, cycle, totalTicks, frames, movingFrames, postFrames;
    private double frozenWeather, lastElapsed;
    private Vec3d previousCamera, anchor;
    private Perspective perspective;
    private int fov;
    private boolean earlyRequested, menuTested, menuPaused;
    private long menuOpened;
    private double pausedElapsed;
    private String failure;
    private final java.util.Set<String> screenshots = new java.util.HashSet<>();
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, counter) -> {
            if (stage != 3 || (cycle != 0 && cycle != 2) || !JiahaoCinematicController.isCameraActive()) return;
            int phase = JiahaoCinematicController.elapsedTicks() < 24 ? 0 : JiahaoCinematicController.elapsedTicks() < 50 ? 1
                    : JiahaoCinematicController.elapsedTicks() < 72 ? 2 : 3;
            if (JiahaoCinematicController.elapsedTicks() < new int[]{16,42,66,91}[phase]) return;
            String name = "cinematic-" + cycle + "-pose-" + phase + ".png";
            if (screenshots.add(name)) {
                var client = MinecraftClient.getInstance();
                net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(client.runDirectory, name, client.getFramebuffer(), text -> { });
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (failure != null) return;
            try {
                var client = MinecraftClient.getInstance();
                if (stage != 3 || client.player == null) return;
                if (JiahaoCinematicController.isCameraActive()) {
                    frames++;
                    double elapsed = JiahaoCinematicController.elapsedTicks();
                    check(elapsed >= lastElapsed, "Render progress is monotonic"); lastElapsed = elapsed;
                    check(context.camera().isThirdPerson(), "First-person owner model must be visible");
                    check(client.options.getPerspective() == perspective, "Perspective option never changes");
                    check(client.options.getFov().getValue() == fov, "FOV option never changes");
                    if (!JiahaoCinematicController.isReturning()) {
                        check(JiahaoTimeStopClientState.isTimeStopped(client.world), "World remains stopped through normal cinematic");
                        near(JiahaoTimeStopClientState.getWorldAnimationTime(), frozenWeather, .00001, "Weather remains frozen");
                    }
                    if (elapsed > 10 && elapsed < 90 && !JiahaoCinematicController.isReturning()) {
                        Vec3d toTarget = JiahaoCinematicController.origin().add(0,1.45,0).subtract(context.camera().getPos()).normalize();
                        Vec3d forward = Vec3d.fromPolar(context.camera().getPitch(), context.camera().getYaw());
                        check(forward.dotProduct(toTarget) > .9999, "Orbit looks at upper body");
                        var hit = client.world.raycast(new RaycastContext(JiahaoCinematicController.origin().add(0,1.45,0),
                                context.camera().getPos(), RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, client.player));
                        check(hit.getType() == HitResult.Type.MISS, "Camera remains in front of the blocking wall");
                        if (previousCamera != null && previousCamera.squaredDistanceTo(context.camera().getPos()) > 1e-12) movingFrames++;
                    }
                    previousCamera = context.camera().getPos();
                    if (cycle == 0 && elapsed > 30 && !menuTested) {
                        menuTested = true; menuOpened = System.nanoTime(); client.setScreen(new GameMenuScreen(true));
                    }
                    if (menuTested && client.currentScreen instanceof GameMenuScreen) {
                        if (client.isPaused()) {
                            if (!menuPaused) { pausedElapsed = elapsed; menuPaused = true; }
                            near(elapsed, pausedElapsed, .00001, "Singleplayer pause freezes cinematic clock");
                        }
                        if (System.nanoTime() - menuOpened > 700_000_000L) client.setScreen(null);
                    }
                } else postFrames++;
            } catch (Throwable error) { fail(error); }
        });
    }
    private void tick(MinecraftClient client) {
        try {
            if (failure != null) { finish(client, false); return; }
            if (++totalTicks > 3200) throw new AssertionError("Cinematic smoke watchdog");
            ticks++;
            if (stage == 0) {
                if (client.player == null || client.world == null || client.getServer() == null || ticks < 100) return;
                client.options.pauseOnLostFocus = false; client.options.getMaxFps().setValue(180); client.setScreen(null);
                configure(client); stage = 1; ticks = 0; return;
            }
            if (stage == 1 && ticks > 80) {
                perspective = Perspective.values()[Math.min(cycle, 2)]; client.options.setPerspective(perspective);
                fov = client.options.getFov().getValue();
                frames = movingFrames = postFrames = 0; lastElapsed = 0; previousCamera = null; earlyRequested = false;
                check(client.player.isOnGround(), "Client fixture is grounded");
                ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE);
                stage = 2; ticks = 0; return;
            }
            if (stage == 2) {
                if (!JiahaoCinematicController.isCameraActive()) { check(ticks < 80, "S2C must start cinematic"); return; }
                anchor = client.player.getPos(); frozenWeather = JiahaoTimeStopClientState.getWorldAnimationTime();
                stage = 3; ticks = 0; return;
            }
            if (stage == 3) {
                double elapsed = JiahaoCinematicController.elapsedTicks();
                if (JiahaoCinematicController.locksInput()) {
                    check(client.player.getPos().squaredDistanceTo(anchor) < .003, "Owner position stable");
                    client.options.forwardKey.setPressed(true); client.options.jumpKey.setPressed(true);
                    if (elapsed > 12) {
                        near(client.player.input.movementForward, 0, 0, "W input suppressed");
                        check(!client.player.input.jumping, "Jump input suppressed");
                    }
                    if (cycle == 1 && elapsed > 25 && elapsed < 35) client.setScreen(new ChatScreen(""));
                    if (cycle == 1 && elapsed >= 35 && client.currentScreen instanceof ChatScreen) client.setScreen(null);
                    if (cycle >= 3 && elapsed > 35 && !earlyRequested) {
                        earlyRequested = true;
                        if (cycle == 3) ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE);
                        if (cycle == 4) client.getServer().execute(() -> {
                            var p = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                            p.setHealth(0); p.onDeath(p.getServerWorld().getDamageSources().generic());
                        });
                        if (cycle == 5) {
                            var server = client.getServer(); var uuid = client.player.getUuid();
                            server.execute(() -> {
                                var p = server.getPlayerManager().getPlayer(uuid);
                                p.teleportTo(new TeleportTarget(server.getWorld(World.NETHER), new Vec3d(0,80,0), Vec3d.ZERO,0,0,TeleportTarget.NO_OP));
                            });
                        }
                        if (cycle == 6) {
                            finish(client, true); return;
                        }
                    }
                    return;
                }
                client.options.forwardKey.setPressed(false); client.options.jumpKey.setPressed(false);
                if (JiahaoCinematicController.isCameraActive()) return;
                check(frames > 10, "Actual cinematic camera rendered multiple frames");
                check(client.options.getPerspective() == perspective && client.options.getFov().getValue() == fov, "Original settings preserved");
                if (cycle < 3) {
                    check(JiahaoTimeStopClientState.isTimeStopped(client.world), "Cinematic finishes before world resumes");
                    check(movingFrames > 30, "Camera advances on rendered frames");
                    if (cycle == 0) check(menuPaused, "ESC test actually paused integrated server");
                    testModelAndObserver(client);
                }
                JiahaoMode.LOGGER.info("CINEMATIC CYCLE {} PASSED: {} rendered frames, {} moving frames, perspective {}", cycle, frames, movingFrames, perspective);
                stage = 4; ticks = 0; return;
            }
            if (stage == 4 && ticks > 100) {
                if (cycle == 4 && client.player != null && !client.player.isAlive()) {
                    client.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
                    client.setScreen(null); ticks = 60; return;
                }
                if (client.player == null || !client.player.isAlive()) return;
                cycle++; configure(client); stage = 1; ticks = 0;
            }
        } catch (Throwable error) { fail(error); }
    }
    private void configure(MinecraftClient client) {
        var server = client.getServer(); var uuid = client.player.getUuid();
        server.execute(() -> {
            var p = server.getPlayerManager().getPlayer(uuid); var world = server.getWorld(World.OVERWORLD);
            // Dedicated, disposable fixture above terrain; no daily saves involved.
            for (int x=-8; x<=8; x++) for (int z=-8; z<=8; z++) {
                world.setBlockState(new BlockPos(x,179,z), Blocks.STONE.getDefaultState());
                for (int y=180; y<186; y++) world.setBlockState(new BlockPos(x,y,z), Blocks.AIR.getDefaultState());
            }
            if (cycle == 2) for (int y=180;y<184;y++) for (int z=-5;z<=5;z++) world.setBlockState(new BlockPos(2,y,z), Blocks.STONE.getDefaultState());
            p.teleportTo(new TeleportTarget(world, new Vec3d(.5,180,.5), Vec3d.ZERO, 0,0,TeleportTarget.NO_OP));
            server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4), "fillbiome -8 179 -8 8 185 8 minecraft:plains");
            p.getAbilities().flying = false; p.sendAbilitiesUpdate();
            equipArmor(p); JiahaoStateManager.setJiahao(p,true); world.setWeather(0,12000,true,false); world.setTimeOfDay(12000);
        });
    }
    private void testModelAndObserver(MinecraftClient client) {
        var actor = new OtherClientPlayerEntity(client.world, new GameProfile(UUID.randomUUID(), "PoseObserverFixture"));
        var renderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(actor);
        var model = renderer.getModel();
        UUID session = UUID.randomUUID();
        var payload = new JiahaoTimeStatePayload(client.world.getRegistryKey().getValue(),true,actor.getUuid(),110,
                client.world.getTime(),client.world.getTimeOfDay(),session,50,true,actor.getPos(),0);
        JiahaoCinematicController.onStateSync(client.world, payload);
        check(!JiahaoCinematicController.isCameraActive() && !JiahaoCinematicController.locksInput(), "Observer never gets camera/input override");
        model.setAngles(actor,0,0,0,0,0);
        check(Math.abs(model.rightArm.pitch) > .5, "Remote owner model receives pose");
        near(model.rightSleeve.pitch,model.rightArm.pitch,.00001,"Sleeve follows pose");
        var armor = new net.minecraft.client.render.entity.model.BipedEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity>(
                client.getEntityModelLoader().getModelPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER_OUTER_ARMOR));
        model.copyBipedStateTo(armor); near(armor.rightArm.pitch,model.rightArm.pitch,.00001,"Armor copies pose");
        JiahaoPoseController.restore(model); JiahaoCinematicController.stop(true);
        model.setAngles(actor,0,0,0,0,0); check(Math.abs(model.rightArm.pitch) < .2,"Vanilla pose restored");
        // Completed local session must not be replaced again by any synthetic input.
        JiahaoCinematicController.cleanup();
    }
    private void fail(Throwable error) { failure = error.toString(); JiahaoMode.LOGGER.error("CINEMATIC SMOKE FAILED stage {} cycle {}",stage,cycle,error); }
    private void finish(MinecraftClient client, boolean success) {
        stage = 9;
        client.options.forwardKey.setPressed(false); client.options.jumpKey.setPressed(false);
        if (client.getServer() != null) client.getServer().stop(false);
        client.disconnect();
        check(!JiahaoCinematicController.isCameraActive() && !JiahaoCinematicController.locksInput(), "Disconnect cleanup");
        try { Files.writeString(Path.of("cinematic-smoke-result.txt"), success ? "PASSED" : "FAILED: " + failure); }
        catch (Exception error) { throw new RuntimeException(error); }
        JiahaoMode.LOGGER.info("CINEMATIC SMOKE {}", success ? "PASSED" : "FAILED");
        client.scheduleStop();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void near(double actual,double expected,double epsilon,String message) { check(Math.abs(actual-expected)<=epsilon,message+": "+actual+" != "+expected); }

 private static void equipArmor(net.minecraft.server.network.ServerPlayerEntity p) {
  p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_HELMET));
  p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_CHESTPLATE));
  p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_LEGGINGS));
  p.equipStack(net.minecraft.entity.EquipmentSlot.FEET,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_BOOTS));
 }
}
