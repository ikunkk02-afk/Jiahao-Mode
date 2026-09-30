// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.hao;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Persistent authority only; HUD snapshots use an owner-only payload. */
public record HaoState(int units, int gainLock, boolean bursting) {
    public static final int MAX = 10000;
    public static final HaoState EMPTY = new HaoState(0, 0, false);
    public HaoState { units = Math.max(0, Math.min(MAX, units)); gainLock = Math.max(0, Math.min(100, gainLock)); }
    public static final Codec<HaoState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("units", 0).forGetter(HaoState::units),
        Codec.INT.optionalFieldOf("gain_lock", 0).forGetter(HaoState::gainLock),
        Codec.BOOL.optionalFieldOf("bursting", false).forGetter(HaoState::bursting)
    ).apply(i, HaoState::new));
    public static final AttachmentType<HaoState> STORAGE = AttachmentRegistry.create(JiahaoMode.id("hao_state"),
        b -> b.persistent(CODEC).copyOnDeath());
    public static void initialize() {}
}
