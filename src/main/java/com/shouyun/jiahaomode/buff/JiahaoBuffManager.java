// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.buff;

import com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.quote.JiahaoQuoteCategory;
import com.shouyun.jiahaomode.quote.JiahaoQuoteManager;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Called only after a one-use gadget operation has been consumed by the server. */
public final class JiahaoBuffManager {
    public static final int COOLDOWN_TICKS = 600;
    public enum Status { GRANTED, PRESERVED, COOLDOWN, NOT_IN_FORM, INVALID }
    public record Result(Status status, Identifier effect, int amplifier, int durationTicks, boolean override) {
        static Result denied(Status status) { return new Result(status,null,0,0,false); }
    }
    private static final Map<MinecraftServer,Map<UUID,long[]>> SERVERS = new WeakHashMap<>();
    private JiahaoBuffManager() { }
    private static Map<UUID,long[]> runtime(MinecraftServer server) {
        if (!server.isOnThread()) throw new IllegalStateException("Buff authority requires server thread");
        return SERVERS.computeIfAbsent(server,k -> new HashMap<>());
    }
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = JiahaoTimeStopManager.getServerTick(server);
            if (tick % 200 == 0) runtime(server).values().removeIf(t -> t[0] <= tick && t[1] <= tick);
        });
    }
    public static Result tryGrantRandomBuff(ServerPlayerEntity player, JiahaoBuffSource source) {
        var cooldowns = runtime(player.getServer());
        var item = source == JiahaoBuffSource.MARKET_VIEWER ? ModItems.MARKET_VIEWER : ModItems.JIAHAO_CODE_EDITOR;
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                || !(player.getMainHandStack().isOf(item) || player.getOffHandStack().isOf(item))
                || JiahaoCinematicLocks.isLocked(player) || JiahaoTimeStopManager.shouldFreeze(player))
            return Result.denied(Status.INVALID);
        if (!JiahaoStateManager.isJiahao(player)) return Result.denied(Status.NOT_IN_FORM);
        long tick = JiahaoTimeStopManager.getServerTick(player.getServer());
        var until = cooldowns.computeIfAbsent(player.getUuid(),k -> new long[2]);
        if (tick < until[source.ordinal()]) return Result.denied(Status.COOLDOWN);
        until[source.ordinal()] = tick + COOLDOWN_TICKS;
        var reward = JiahaoBuffPool.select(source,bound -> player.getRandom().nextInt(bound));
        boolean applied = applyReward(player,reward);
        if (applied && player.getRandom().nextInt(100) < 25)
            JiahaoQuoteManager.emit(player,source == JiahaoBuffSource.MARKET_VIEWER
                    ? JiahaoQuoteCategory.MARKET : JiahaoQuoteCategory.CODE,null);
        return new Result(applied ? Status.GRANTED : Status.PRESERVED,
                Registries.STATUS_EFFECT.getId(reward.effect().value()),reward.amplifier(),reward.durationTicks(),reward.override());
    }
    public static boolean applyReward(ServerPlayerEntity player, JiahaoBuffPool.Reward reward) {
        if (!player.getServer().isOnThread()) throw new IllegalStateException("Buff authority requires server thread");
        var current = player.getStatusEffect(reward.effect());
        if (current != null && (current.getAmplifier() > reward.amplifier()
                || current.getAmplifier() == reward.amplifier()
                && (current.isInfinite() || current.getDuration() >= reward.durationTicks()))) return false;
        // Vanilla preserves a weaker, longer effect as a hidden effect during an upgrade.
        return player.addStatusEffect(new StatusEffectInstance(reward.effect(),reward.durationTicks(),reward.amplifier(),false,true,true));
    }
}
