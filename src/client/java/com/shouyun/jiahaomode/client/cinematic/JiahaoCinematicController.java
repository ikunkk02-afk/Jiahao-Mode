// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.cinematic;

import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.timestop.JiahaoTimeView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/** One dimension-local session. Only its owner gets camera/input overrides. */
public final class JiahaoCinematicController {
    private static final CinematicTimeline TIMELINE = new CinematicTimeline();
    private static ClientWorld world;
    private static UUID session, owner, failedSession;
    private static Vec3d origin = Vec3d.ZERO;
    private static float yaw;
    private static boolean playing, returning;
    private static double elapsed, returnStart, returnWeight, returnBars, rawFrame;
    private static long clientTicks;
    private JiahaoCinematicController() { }
    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            validate(client);
            JiahaoCinematicInput.update(client);
            if (!client.isPaused()) {
                clientTicks++;
                if (playing) TIMELINE.tick();
            }
            publishMovementLock();
        });
    }
    public static void onStateSync(ClientWorld next, JiahaoTimeStatePayload state) {
        if (world != next) { cleanup(); world = next; }
        if (!state.active() || !state.cinematic()) { stop(false); return; }
        if (!state.session().equals(session)) {
            stop(true);
            session = state.session(); owner = state.owner(); origin = state.origin(); yaw = state.yaw();
            elapsed = state.elapsedTicks();
            TIMELINE.start(state.elapsedTicks());
            playing = state.elapsedTicks() < 100;
            JiahaoCinematicCamera.reset();
            if (locksInput()) {
                var player = MinecraftClient.getInstance().player;
                player.setVelocity(Vec3d.ZERO); player.stopUsingItem(); player.setSprinting(false);
            }
        } else TIMELINE.sync(state.elapsedTicks());
        if (isLocalOwner() && playing && MinecraftClient.getInstance().getCameraEntity() != MinecraftClient.getInstance().player
                && !session.equals(failedSession)) {
            failedSession = session;
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(com.shouyun.jiahaomode.network.JiahaoQuotePlaybackFailedPayload.ID))
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.shouyun.jiahaomode.network.JiahaoQuotePlaybackFailedPayload(session));
            stop(true);
        }
        publishMovementLock();
    }
    public static void beginFrame(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        validate(client);
        if (!client.isPaused()) rawFrame = Math.max(rawFrame, clientTicks + CinematicTimeline.clamp(delta));
        if (playing && !client.isPaused()) elapsed = TIMELINE.sample(delta);
        if (playing && elapsed >= 100) { playing = false; JiahaoCinematicCamera.reset(); }
        if (returning && rawFrame - returnStart >= 3) { returning = false; JiahaoCinematicCamera.reset(); }
        publishMovementLock();
    }
    private static void validate(MinecraftClient client) {
        if (world != null && (client.world != world || client.player == null || !client.player.isAlive())) { cleanup(); return; }
        if (world != null && owner != null) {
            PlayerEntity actor = world.getPlayerByUuid(owner);
            if (actor != null && (!actor.isAlive() || actor.isRemoved())) stop(true);
        }
    }
    public static void stop(boolean immediate) {
        if (!immediate && playing && isLocalOwner() && elapsed < 100) {
            returnWeight = cameraWeight(); returnBars = barOpacity(); returnStart = rawFrame; returning = true;
        } else if (immediate) { returning = false; JiahaoCinematicCamera.reset(); }
        playing = false;
        publishMovementLock();
    }
    public static void cleanup() {
        stop(true); world = null; session = owner = failedSession = null; origin = Vec3d.ZERO;
        elapsed = rawFrame = 0; clientTicks = 0;
        JiahaoCinematicInput.reset();
    }
    private static void publishMovementLock() {
        if (world == null) return;
        var view = world.getAttachedOrElse(JiahaoTimeView.CLIENT_VIEW, JiahaoTimeView.INACTIVE);
        boolean locked = playing && elapsed < 100;
        if (view.cinematicLocked() != locked)
            world.setAttached(JiahaoTimeView.CLIENT_VIEW, new JiahaoTimeView(view.active(), view.owner(), view.remainingTicks(), locked));
    }
    public static boolean isLocalOwner() {
        var player = MinecraftClient.getInstance().player;
        return player != null && world == player.getWorld() && player.getUuid().equals(owner);
    }
    public static boolean locksInput() { return playing && elapsed < 100 && isLocalOwner(); }
    public static boolean isCameraActive() { return (playing || returning) && isLocalOwner(); }
    public static boolean isPoseActive(PlayerEntity player) {
        return playing && elapsed < 100 && player.getWorld() == world && player.getUuid().equals(owner);
    }
    public static UUID sessionId() { return session; }
    public static double elapsedTicks() { return elapsed; }
    public static double getProgress() { return elapsed / 100; }
    public static Vec3d origin() { return origin; }
    public static float yaw() { return yaw; }
    public static double rawFrame() { return rawFrame; }
    public static boolean isReturning() { return returning; }
    public static double cameraWeight() {
        return returning ? returnWeight * (1 - CinematicTimeline.smooth((rawFrame - returnStart) / 3)) : CinematicTimeline.weight(elapsed);
    }
    public static double barOpacity() {
        if (!isCameraActive()) return 0;
        return returning ? returnBars * (1 - CinematicTimeline.smooth((rawFrame - returnStart) / 3)) : CinematicTimeline.bars(elapsed);
    }
}
