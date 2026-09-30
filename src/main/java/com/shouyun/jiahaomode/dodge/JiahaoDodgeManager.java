// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.dodge;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.JiahaoQuoteManager;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Server-thread-only authority. Client input can never dictate an endpoint or a perfect dodge. */
public final class JiahaoDodgeManager {
    public static final int DURATION_TICKS = 6, COOLDOWN_TICKS = 18, PERFECT_TICKS = 4;
    public static final double DISTANCE = 2.8, MAX_SPEED = 1.2;
    public static final TagKey<DamageType> UNDODGEABLE = TagKey.of(RegistryKeys.DAMAGE_TYPE, JiahaoMode.id("undodgeable"));
    private static final Map<MinecraftServer, Runtime> SERVERS = new WeakHashMap<>();
    private static final class Runtime {
        long sequence;
        int administrativeDepth;
        final Map<UUID, JiahaoDodgeState> active = new HashMap<>();
        final Map<UUID, History> history = new HashMap<>();
    }
    private static final class History {
        long cooldownUntil, messageAt = -10, lastPerfect = Long.MIN_VALUE / 2;
        boolean airUsed;
        int combo;
    }
    private JiahaoDodgeManager() { }
    private static Runtime runtime(MinecraftServer server) {
        if (!server.isOnThread()) throw new IllegalStateException("Dodge authority requires server thread");
        return SERVERS.computeIfAbsent(server, ignored -> new Runtime());
    }
    private static long now(ServerPlayerEntity p) { return JiahaoTimeStopManager.getServerTick(p.getServer()); }
    private static History history(ServerPlayerEntity p) { return runtime(p.getServer()).history.computeIfAbsent(p.getUuid(), ignored -> new History()); }
    public static JiahaoDodgeState state(ServerPlayerEntity p) { return runtime(p.getServer()).active.get(p.getUuid()); }
    public static boolean isDodging(PlayerEntity p) { return p instanceof ServerPlayerEntity server && state(server) != null; }
    public static int getCooldownTicks(ServerPlayerEntity p) { return (int)Math.max(0, history(p).cooldownUntil - now(p)); }
    public static int getPerfectDodgeCombo(ServerPlayerEntity p) { return now(p) - history(p).lastPerfect <= 60 ? history(p).combo : 0; }
    public static boolean isPerfectWindow(PlayerEntity p) {
        if (!(p instanceof ServerPlayerEntity server)) return false;
        var s = state(server); long age = s == null ? -1 : now(server) - s.startTick;
        return s != null && !s.perfectTriggered && age >= 0 && age < PERFECT_TICKS;
    }
    private static boolean stable(ServerPlayerEntity p) {
        return p.isAlive() && !p.isRemoved() && !p.isSleeping() && !p.hasVehicle() && !p.isSpectator()
                && !p.getAbilities().flying && !p.isFallFlying() && !p.isSwimming() && !p.isTouchingWater()
                && !p.isInLava() && !p.isClimbing() && !p.noClip && !p.notInAnyWorld
                && !JiahaoTimeStopManager.shouldFreeze(p) && !com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks.isLocked(p)
                && p.getServerWorld().isSpaceEmpty(p, p.getBoundingBox().contract(1.0E-6))
                && !((JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport();
    }
    public static boolean canDodge(ServerPlayerEntity p) {
        return JiahaoStateManager.isJiahao(p) && stable(p) && state(p) == null && getCooldownTicks(p) == 0
                && (p.isOnGround() || !history(p).airUsed);
    }
    /** Local +sideways is left, matching vanilla KeyboardInput. */
    public static Vec3d direction(int mask, float yaw) {
        if ((mask & ~15) != 0 || !Float.isFinite(yaw)) throw new IllegalArgumentException("Invalid dodge direction");
        int f = ((mask & 1) != 0 ? 1 : 0) - ((mask & 2) != 0 ? 1 : 0);
        int s = ((mask & 4) != 0 ? 1 : 0) - ((mask & 8) != 0 ? 1 : 0);
        if (f == 0 && s == 0) f = -1;
        double a = Math.toRadians(yaw);
        return new Vec3d(s * Math.cos(a) - f * Math.sin(a), 0, f * Math.cos(a) + s * Math.sin(a)).normalize();
    }
    public static double stepDistance(int step) {
        double a = 1 - step / (double)DURATION_TICKS, b = 1 - (step + 1) / (double)DURATION_TICKS;
        return Math.min(MAX_SPEED, DISTANCE * (a*a*a - b*b*b));
    }
    public static boolean startDodge(ServerPlayerEntity p, int mask) {
        if ((mask & ~15) != 0) return false;
        if (!JiahaoStateManager.isJiahao(p)) { message(p, "not_in_form"); return false; }
        if (getCooldownTicks(p) > 0) { message(p, "cooldown"); return false; }
        if (!canDodge(p)) return false;
        History h = history(p); if (p.isOnGround()) h.airUsed = false; else h.airUsed = true;
        int f = ((mask & 1) != 0 ? 1 : 0) - ((mask & 2) != 0 ? 1 : 0);
        int side = ((mask & 4) != 0 ? 1 : 0) - ((mask & 8) != 0 ? 1 : 0);
        if (f == 0 && side == 0) f = -1;
        double length = Math.hypot(f, side);
        Runtime r = runtime(p.getServer());
        var s = new JiahaoDodgeState(p.getUuid(), p.getWorld().getRegistryKey().getValue(), ++r.sequence,
                now(p), p.getPos(), direction(mask, p.getYaw()), (float)(f/length), (float)(side/length), p.getVelocity());
        r.active.put(p.getUuid(), s);
        p.stopUsingItem(); p.setSprinting(false);
        ((JiahaoDodgeInteractionAccess)p.interactionManager).jiahao$cancelMining();
        JiahaoQuoteManager.activity(p);
        send(p, s, JiahaoDodgeStatePayload.Phase.START);
        return true;
    }
    public static boolean beforeTravel(ServerPlayerEntity p) {
        var s = state(p); if (s == null) return false;
        if (!stable(p) || !JiahaoStateManager.isJiahao(p)) { clear(p, false); return false; }
        if (s.movementTick == now(p) || s.steps >= DURATION_TICKS) return false;
        s.movementTick = now(p);
        Vec3d v = p.getVelocity();
        // Preserve external impulses (notably explosion knockback), separate from scheduled dodge velocity.
        s.impulse = s.impulse.add(v.subtract(s.expectedVelocity).multiply(1, 0, 1));
        Vec3d horizontal = s.direction.multiply(stepDistance(s.steps)).add(s.impulse);
        p.setVelocity(horizontal.x, v.y, horizontal.z);
        p.setJumping(false);
        return true;
    }
    public static void afterTravel(ServerPlayerEntity p) {
        var s = state(p); if (s == null) return;
        s.steps++; s.expectedVelocity = p.getVelocity(); s.impulse = s.impulse.multiply(.91);
        if (!p.isOnGround()) history(p).airUsed = true;
    }
    private static void message(ServerPlayerEntity p, String suffix) {
        History h = history(p); if (now(p) - h.messageAt < 10) return;
        h.messageAt = now(p); p.sendMessage(Text.translatable("message.jiahao-mode.dodge." + suffix), true);
    }
    private static void send(ServerPlayerEntity p, JiahaoDodgeState s, JiahaoDodgeStatePayload.Phase phase) {
        var packet = new JiahaoDodgeStatePayload(p.getUuid(), s.dimension, s.action, phase, s.steps,
                p.getPos(), p.getVelocity(), p.isOnGround(), s.forward, s.sideways);
        Set<UUID> sent = new HashSet<>();
        for (var observer : p.getServerWorld().getPlayers()) {
            if ((observer == p || PlayerLookup.tracking(p).contains(observer)) && ServerPlayNetworking.canSend(observer, packet.getId())) {
                ServerPlayNetworking.send(observer, packet); s.recipients.add(observer.getUuid()); sent.add(observer.getUuid());
            }
        }
        if (phase == JiahaoDodgeStatePayload.Phase.END || phase == JiahaoDodgeStatePayload.Phase.CANCEL) {
            for (UUID id : s.recipients) {
                var observer = p.getServer().getPlayerManager().getPlayer(id);
                if (observer != null && !sent.contains(id) && ServerPlayNetworking.canSend(observer, packet.getId())) ServerPlayNetworking.send(observer, packet);
            }
        }
    }
    public static void clear(ServerPlayerEntity p, boolean forget) {
        Runtime r = runtime(p.getServer()); var s = r.active.remove(p.getUuid());
        if (s != null) {
            // Vanilla player teleports retain velocity; other teleport implementations may replace it.
            // Remove a retained dodge, but never subtract its old velocity from an already replaced target.
            if (s.steps > 0) {
                Vec3d v = p.getVelocity();
                boolean relocated = !s.dimension.equals(p.getWorld().getRegistryKey().getValue())
                        || ((JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport();
                if (!relocated || v.subtract(s.expectedVelocity).multiply(1,0,1).lengthSquared() < 1.0E-10) {
                    p.setVelocity(s.impulse.x + v.x - s.expectedVelocity.x, v.y,
                            s.impulse.z + v.z - s.expectedVelocity.z);
                }
            }
            history(p).cooldownUntil = Math.max(history(p).cooldownUntil, now(p) + COOLDOWN_TICKS);
            send(p, s, JiahaoDodgeStatePayload.Phase.CANCEL);
        }
        if (forget) r.history.remove(p.getUuid());
    }
    public static boolean isDodgeableDamage(DamageSource source) {
        if (source.isIn(UNDODGEABLE) || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.isIn(DamageTypeTags.IS_FALL) || source.isIn(DamageTypeTags.IS_DROWNING)
                || source.isIn(DamageTypeTags.IS_FREEZING)) return false;
        if (source.isIn(DamageTypeTags.IS_EXPLOSION) || source.isIn(DamageTypeTags.IS_PROJECTILE)) return true;
        Entity direct = source.getSource(), attacker = source.getAttacker();
        return direct instanceof ProjectileEntity || direct instanceof LivingEntity && direct == attacker
                || direct == null && attacker instanceof LivingEntity;
    }
    public static boolean allowDamage(ServerPlayerEntity p, DamageSource source, float amount) {
        if (!Float.isFinite(amount) || amount <= 0 || runtime(p.getServer()).administrativeDepth > 0
                || !JiahaoStateManager.isJiahao(p) || !isPerfectWindow(p) || !isDodgeableDamage(source)) return true;
        var s = state(p); s.perfectTriggered = true;
        History h = history(p); h.combo = now(p) - h.lastPerfect <= 60 ? h.combo + 1 : 1; h.lastPerfect = now(p);
        Vec3d attack = source.getAttacker() != null ? source.getAttacker().getPos() : source.getPosition();
        var packet = new JiahaoPerfectDodgePayload(p.getUuid(), s.dimension, s.action, s.origin, attack);
        for (var observer : p.getServerWorld().getPlayers()) {
            if ((observer == p || PlayerLookup.tracking(p).contains(observer)) && ServerPlayNetworking.canSend(observer, packet.getId())) ServerPlayNetworking.send(observer, packet);
        }
        com.shouyun.jiahaomode.hao.HaoMeterManager.gain(p,12);
        JiahaoQuoteManager.perfectDodge(p);
        return false;
    }
    public static void enterAdministrativeDamage(MinecraftServer server) { runtime(server).administrativeDepth++; }
    public static void leaveAdministrativeDamage(MinecraftServer server) { runtime(server).administrativeDepth--; }
    private static void tick(MinecraftServer server) {
        Runtime r = runtime(server);
        for (var entry : r.history.entrySet()) {
            var p = server.getPlayerManager().getPlayer(entry.getKey());
            if (p != null && p.isOnGround() && !r.active.containsKey(entry.getKey())) entry.getValue().airUsed = false;
        }
        for (var s : new ArrayList<>(r.active.values())) {
            var p = server.getPlayerManager().getPlayer(s.player);
            if (p == null) { r.active.remove(s.player); r.history.remove(s.player); continue; }
            if (!JiahaoStateManager.isJiahao(p) || !p.isAlive() || !s.dimension.equals(p.getWorld().getRegistryKey().getValue())
                    || !stable(p)) { clear(p, false); continue; }
            send(p, s, JiahaoDodgeStatePayload.Phase.STEP);
            if (s.steps >= DURATION_TICKS) {
                r.active.remove(s.player); history(p).cooldownUntil = now(p) + COOLDOWN_TICKS;
                Vec3d v = p.getVelocity(); p.setVelocity(s.impulse.x, v.y, s.impulse.z);
                send(p, s, JiahaoDodgeStatePayload.Phase.END);
                p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
            }
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(JiahaoDodgeManager::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity p) || allowDamage(p, source, amount));
        ServerLivingEntityEvents.AFTER_DEATH.register((e, source) -> { if (e instanceof ServerPlayerEntity p) clear(p, true); });
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> clear(h.player, true));
        ServerPlayerEvents.AFTER_RESPAWN.register((old, p, alive) -> clear(old, true));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p, old, next) -> clear(p, false));
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            for (var p : world.getPlayers()) clear(p, true);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { for (var p : server.getPlayerManager().getPlayerList()) clear(p, true); });
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
    }
}
