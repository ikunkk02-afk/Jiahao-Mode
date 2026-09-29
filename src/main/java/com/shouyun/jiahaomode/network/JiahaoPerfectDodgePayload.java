// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public record JiahaoPerfectDodgePayload(UUID player, Identifier dimension, long action,
        Vec3d origin, Vec3d attackPosition) implements CustomPayload {
    public static final Id<JiahaoPerfectDodgePayload> ID = new Id<>(JiahaoMode.id("perfect_dodge"));
    public static final PacketCodec<RegistryByteBuf, JiahaoPerfectDodgePayload> CODEC = new PacketCodec<>() {
        public JiahaoPerfectDodgePayload decode(RegistryByteBuf b) {
            UUID player = b.readUuid(); Identifier dimension = b.readIdentifier(); long action = b.readVarLong();
            Vec3d origin = JiahaoDodgeStatePayload.vector(b);
            return new JiahaoPerfectDodgePayload(player, dimension, action, origin,
                    b.readBoolean() ? JiahaoDodgeStatePayload.vector(b) : null);
        }
        public void encode(RegistryByteBuf b, JiahaoPerfectDodgePayload p) {
            b.writeUuid(p.player()); b.writeIdentifier(p.dimension()); b.writeVarLong(p.action());
            JiahaoDodgeStatePayload.vector(b, p.origin()); b.writeBoolean(p.attackPosition() != null);
            if (p.attackPosition() != null) JiahaoDodgeStatePayload.vector(b, p.attackPosition());
        }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
