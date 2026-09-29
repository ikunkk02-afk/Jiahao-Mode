// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.dodge;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Ephemeral action. Cooldown/air history deliberately lives outside this object. */
public final class JiahaoDodgeState {
    public final UUID player;
    public final Identifier dimension;
    public final long action, startTick;
    public final Vec3d origin, direction;
    public final float forward, sideways;
    public final Set<UUID> recipients = new HashSet<>();
    public int steps;
    public long movementTick = -1;
    public boolean perfectTriggered;
    public Vec3d expectedVelocity, impulse = Vec3d.ZERO;

    JiahaoDodgeState(UUID player, Identifier dimension, long action, long tick,
                     Vec3d origin, Vec3d direction, float forward, float sideways, Vec3d velocity) {
        this.player = player; this.dimension = dimension; this.action = action;
        this.startTick = tick; this.origin = origin; this.direction = direction;
        this.forward = forward; this.sideways = sideways; this.expectedVelocity = velocity;
    }
}
