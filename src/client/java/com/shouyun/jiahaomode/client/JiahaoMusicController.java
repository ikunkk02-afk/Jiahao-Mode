// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import com.shouyun.jiahaomode.hao.HaoBurstTimeline;
import com.shouyun.jiahaomode.network.HaoBurstPayload;
import com.shouyun.jiahaomode.network.JiahaoTimeStatePayload;
import com.shouyun.jiahaomode.sound.ModSounds;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.TimeStopReason;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** One local music voice driven by server-confirmed sessions, never the camera lease. */
public final class JiahaoMusicController {
    public static final int FADE_TICKS = 15;
    private static final Set<UUID> ended = new LinkedHashSet<>();
    private static ClientWorld world;
    private static UUID session;
    private static JiahaoMusicReason reason;
    private static HaoMarchSound playing;
    private static int age, duration, fadeRemaining;
    private static float fadeVolume;
    private static boolean attempted;
    private JiahaoMusicController() { }

    private static void useWorld(ClientWorld next) {
        if (world != next) { stopJiahaoMarch(); ended.clear(); world = next; }
    }
    private static void remember(UUID id) {
        if (id == null) return;
        ended.add(id);
        if (ended.size() > 256) ended.remove(ended.iterator().next());
    }
    public static void onTimeState(ClientWorld next, JiahaoTimeStatePayload state) {
        useWorld(next);
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!state.active() || !client.player.getUuid().equals(state.owner())) {
            if (reason == JiahaoMusicReason.MANUAL_TIME_STOP) fadeOutJiahaoMarch();
            return;
        }
        // Burst and time-stop UUIDs differ; the burst payload owns burst music.
        if (state.reason() == TimeStopReason.HAO_BURST) return;
        playJiahaoMarch(state.session(), JiahaoMusicReason.MANUAL_TIME_STOP,
                state.elapsedTicks(), state.elapsedTicks() + state.remainingTicks());
    }
    public static void onBurstState(HaoBurstPayload state) {
        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null
                || !client.world.getRegistryKey().getValue().equals(state.dimension())
                || !client.player.getUuid().equals(state.player())) return;
        useWorld(client.world);
        if (!state.active()) {
            remember(state.session());
            if (state.session().equals(session)) stopJiahaoMarch();
        } else if (state.elapsed() >= 0 && state.elapsed() < HaoBurstTimeline.DURATION) {
            playJiahaoMarch(state.session(), JiahaoMusicReason.HAO_BURST,
                    state.elapsed(), HaoBurstTimeline.DURATION);
        }
    }
    public static void playJiahaoMarch(UUID id, JiahaoMusicReason source, int elapsed, int total) {
        if (id == null || ended.contains(id) || elapsed < 0 || elapsed >= total) return;
        if (id.equals(session)) { age = Math.max(age, elapsed); return; }
        if (session != null && fadeRemaining == 0 && reason.ordinal() > source.ordinal()) return;
        boolean upgrade = session != null && reason == JiahaoMusicReason.MANUAL_TIME_STOP
                && source == JiahaoMusicReason.HAO_BURST && playing != null;
        if (upgrade) remember(session);
        else stopJiahaoMarch();
        session = id; reason = source; age = elapsed; duration = total; fadeRemaining = 0;
        attempted = upgrade;
        startVoice(MinecraftClient.getInstance());
    }
    private static void startVoice(MinecraftClient client) {
        if (session == null || attempted || fadeRemaining > 0
                || reason == JiahaoMusicReason.HAO_BURST && (age < 6 || age >= 220)) return;
        attempted = true;
        var resource = client.getSoundManager().get(ModSounds.JIAHAO_MARCH.getId());
        if (resource == null || resource.getWeight() == 0) return;
        playing = new HaoMarchSound();
        // Minecraft skips a voice started at exactly zero volume; the fade still begins at silence.
        playing.volume(Math.max(.001f,volume()));
        client.getSoundManager().play(playing);
    }
    private static float volume() {
        return fadeRemaining > 0 ? fadeVolume * fadeRemaining / FADE_TICKS
                : reason == JiahaoMusicReason.HAO_BURST ? (float) HaoBurstTimeline.musicVolume(age) : .75f;
    }
    public static void update(MinecraftClient client) {
        useWorld(client.world);
        if (session == null) return;
        if (client.player == null || !client.player.isAlive()) { stopJiahaoMarch(); return; }
        if (client.isPaused()) return;
        if (!JiahaoStateManager.isJiahao(client.player)) {
            if (reason == JiahaoMusicReason.MANUAL_TIME_STOP) fadeOutJiahaoMarch();
            else { stopJiahaoMarch(); return; }
        }
        if (fadeRemaining > 0) {
            if (--fadeRemaining == 0) { stopJiahaoMarch(); return; }
        } else if (++age >= duration) {
            if (reason == JiahaoMusicReason.MANUAL_TIME_STOP) fadeOutJiahaoMarch();
            else { stopJiahaoMarch(); return; }
        }
        startVoice(client);
        if (playing != null) playing.volume(volume());
    }
    public static void fadeOutJiahaoMarch() {
        if (session == null || fadeRemaining > 0) return;
        remember(session);
        if (playing == null) { stopJiahaoMarch(); return; }
        fadeVolume = volume(); fadeRemaining = FADE_TICKS;
    }
    public static void stopJiahaoMarch() {
        remember(session);
        if (playing != null) {
            playing.finish(); MinecraftClient.getInstance().getSoundManager().stop(playing); playing = null;
        }
        session = null; reason = null; attempted = false; age = duration = fadeRemaining = 0;
    }
    public static void onResourceReload() { MinecraftClient.getInstance().execute(JiahaoMusicController::stopJiahaoMarch); }
    public static boolean isPlaying() { return playing != null && MinecraftClient.getInstance().getSoundManager().isPlaying(playing); }
    public static SoundInstance soundInstance() { return playing; }
    public static UUID sessionId() { return session; }
    public static boolean isFading() { return fadeRemaining > 0; }
}
