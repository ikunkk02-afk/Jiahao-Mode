// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.buff;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** Explicit weights and inclusive integer-second durations; random authority stays on the server. */
public final class JiahaoBuffPool {
    public record Entry(RegistryEntry<StatusEffect> effect, int weight, int minSeconds, int maxSeconds) { }
    public record Reward(RegistryEntry<StatusEffect> effect, int amplifier, int durationTicks, boolean override) { }
    public static final List<Entry> MARKET = List.of(
            new Entry(StatusEffects.LUCK,30,30,60), new Entry(StatusEffects.HASTE,25,30,45),
            new Entry(StatusEffects.SPEED,20,20,40), new Entry(StatusEffects.ABSORPTION,15,20,30),
            new Entry(StatusEffects.REGENERATION,10,8,12));
    public static final List<Entry> CODE = List.of(
            new Entry(StatusEffects.HASTE,25,30,45), new Entry(StatusEffects.SPEED,20,20,35),
            new Entry(StatusEffects.NIGHT_VISION,20,45,90), new Entry(StatusEffects.RESISTANCE,15,15,25),
            new Entry(StatusEffects.JUMP_BOOST,10,20,30), new Entry(StatusEffects.STRENGTH,10,15,20));
    private JiahaoBuffPool() { }
    public static Reward select(JiahaoBuffSource source, IntUnaryOperator random) {
        if (source == JiahaoBuffSource.CODE_EDITOR && random.applyAsInt(100) < 5) {
            return switch (random.applyAsInt(3)) {
                case 0 -> new Reward(StatusEffects.SPEED,1,300,true);
                case 1 -> new Reward(StatusEffects.HASTE,1,400,true);
                default -> new Reward(StatusEffects.JUMP_BOOST,1,300,true);
            };
        }
        var pool = source == JiahaoBuffSource.MARKET_VIEWER ? MARKET : CODE;
        int roll = random.applyAsInt(100);
        for (var entry : pool) {
            if (roll < entry.weight()) return new Reward(entry.effect(),0,
                    20 * (entry.minSeconds() + random.applyAsInt(entry.maxSeconds() - entry.minSeconds() + 1)),false);
            roll -= entry.weight();
        }
        throw new IllegalArgumentException("Random weight outside pool");
    }
}
