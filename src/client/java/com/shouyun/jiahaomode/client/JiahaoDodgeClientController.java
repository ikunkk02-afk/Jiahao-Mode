// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopParticle;
import com.shouyun.jiahaomode.network.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Server samples drive movement; render interpolation, pose and camera never decide gameplay. */
public final class JiahaoDodgeClientController {
    public static final class Visual {
        public final long action;
        public final float forward, sideways;
        public final double started;
        public double lastReceived, perfectAt = -1, endedAt = -1;
        public int steps = -1;
        public boolean active = true, perfect;
        public Vec3d attackPosition;
        Visual(JiahaoDodgeStatePayload p) {
            action = p.action(); forward = p.forward(); sideways = p.sideways();
            started = clock - p.steps(); lastReceived = clock;
        }
        public double progress() { return Math.max(0, Math.min(1, (frame - started) / 6)); }
        public double weight() {
            double age = frame - started;
            double weight = Math.sin(Math.PI * Math.max(0, Math.min(1, age / 8)));
            if (endedAt >= 0) weight *= Math.max(0, 1 - (frame - endedAt) / 2);
            return Math.max(0, weight);
        }
    }
    private static final Map<UUID, Visual> STATES = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();
    private static ClientWorld world;
    private static double clock, frame;
    private static JiahaoDodgeStatePayload pending;
    private static long localReconcileAction;
    private JiahaoDodgeClientController() { }
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(JiahaoDodgeStatePayload.ID, (p,c) -> receive(p));
        ClientPlayNetworking.registerGlobalReceiver(JiahaoPerfectDodgePayload.ID, (p,c) -> perfect(p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(c -> tick(c));
    }
    private static void checkWorld() {
        var c = MinecraftClient.getInstance();
        if (world != c.world) { clear(); world = c.world; }
    }
    public static void receive(JiahaoDodgeStatePayload p) {
        checkWorld(); if (world == null || !world.getRegistryKey().getValue().equals(p.dimension())) return;
        long last = LAST_ACTION.getOrDefault(p.player(), -1L);
        if (p.action() < last || p.steps() < 0 || p.steps() > 6) return;
        Visual v = STATES.get(p.player());
        if (p.action() == last && (v == null || !v.active)) return;
        if (v == null || v.action != p.action()) { v = new Visual(p); STATES.put(p.player(), v); LAST_ACTION.put(p.player(), p.action()); }
        if (p.steps() < v.steps || p.steps() == v.steps && p.phase() == JiahaoDodgeStatePayload.Phase.START) return;
        if (p.phase() == JiahaoDodgeStatePayload.Phase.STEP && p.steps() > v.steps && p.steps() > 0 && p.steps() % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                var particle = MinecraftClient.getInstance().particleManager.addParticle(ParticleTypes.CLOUD,
                        p.position().x, p.position().y + .35 + i * .35, p.position().z, 0, .005, 0);
                if (particle instanceof JiahaoTimeStopParticle immune) immune.jiahao$setTimeStopImmune(true);
            }
        }
        v.steps = p.steps(); v.lastReceived = clock;
        boolean local = MinecraftClient.getInstance().player != null && p.player().equals(MinecraftClient.getInstance().player.getUuid());
        if (local) pending = p;
        if (p.phase() == JiahaoDodgeStatePayload.Phase.END || p.phase() == JiahaoDodgeStatePayload.Phase.CANCEL) {
            v.active = false; v.endedAt = clock;
            if (local) localReconcileAction = p.action();
            if (p.phase() == JiahaoDodgeStatePayload.Phase.CANCEL) {
                v.perfect = false; v.perfectAt = -1; STATES.remove(p.player());
            }
        }
    }
    public static void perfect(JiahaoPerfectDodgePayload p) {
        checkWorld(); if (world == null || !world.getRegistryKey().getValue().equals(p.dimension())) return;
        Visual v = STATES.get(p.player());
        if (v == null || v.action != p.action() || v.perfect) return;
        v.perfect = true; v.perfectAt = frame; v.attackPosition = p.attackPosition();
        world.playSound(p.origin().x, p.origin().y, p.origin().z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, .65f, 1.6f, false);
        for (int i = 0; i < 8; i++) {
            var particle = MinecraftClient.getInstance().particleManager.addParticle(i < 4 ? ParticleTypes.CLOUD : ParticleTypes.CRIT,
                    p.origin().x + (world.random.nextDouble()-.5)*.4, p.origin().y + .5 + world.random.nextDouble(),
                    p.origin().z + (world.random.nextDouble()-.5)*.4, 0, .01, 0);
            if (particle instanceof JiahaoTimeStopParticle immune) immune.jiahao$setTimeStopImmune(true);
        }
    }
    private static void tick(MinecraftClient c) {
        checkWorld(); if (world == null || c.isPaused()) return;
        clock++; frame = Math.max(frame, clock);
        if (pending != null && c.player != null && pending.player().equals(c.player.getUuid())) {
            c.player.setPosition(pending.position()); c.player.setVelocity(pending.velocity()); c.player.setOnGround(pending.onGround());
            pending = null;
        }
        STATES.entrySet().removeIf(e -> {
            var player = world.getPlayerByUuid(e.getKey()); var v = e.getValue();
            return player != null && (!player.isAlive() || player.isRemoved()) || clock - v.lastReceived > 12
                    || !v.active && clock - v.endedAt > 8;
        });
    }
    public static void beginFrame(float delta) {
        checkWorld(); if (!MinecraftClient.getInstance().isPaused()) frame = clock + Math.max(0, Math.min(1, delta));
    }
    public static Visual visual(PlayerEntity p) { checkWorld(); return STATES.get(p.getUuid()); }
    public static Visual local() { var p = MinecraftClient.getInstance().player; return p == null ? null : visual(p); }
    public static boolean locksMovement() { var v = local(); return v != null && v.active; }
    public static double cameraWeight() {
        if (JiahaoCinematicController.isCameraActive()) return 0;
        var v = local(); if (v == null || v.perfectAt < 0) return 0;
        return Math.sin(Math.PI * Math.max(0, Math.min(1, (frame - v.perfectAt) / 6)));
    }
    public static double fovOffset() { return 4 * cameraWeight(); }
    public static double cameraRoll() { var v = local(); return v == null ? 0 : -4 * v.sideways * cameraWeight(); }
    /** Called after vanilla's final reconciliation so its coordinate is not overwritten by a queued sample. */
    public static void reconciled() { if (localReconcileAction != 0) { pending = null; localReconcileAction = 0; } }
    public static void clear() { STATES.clear(); LAST_ACTION.clear(); world = null; pending = null; clock = frame = 0; localReconcileAction = 0; }
}
