// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public record JiahaoDodgeStatePayload(UUID player, Identifier dimension, long action, Phase phase,
        int steps, Vec3d position, Vec3d velocity, boolean onGround, float forward, float sideways) implements CustomPayload {
    public enum Phase { START, STEP, END, CANCEL }
    public static final Id<JiahaoDodgeStatePayload> ID = new Id<>(JiahaoMode.id("dodge_state"));
    public static final PacketCodec<RegistryByteBuf, JiahaoDodgeStatePayload> CODEC = new PacketCodec<>() {
        public JiahaoDodgeStatePayload decode(RegistryByteBuf b) {
            return new JiahaoDodgeStatePayload(b.readUuid(), b.readIdentifier(), b.readVarLong(),
                    b.readEnumConstant(Phase.class), b.readVarInt(), vector(b), vector(b), b.readBoolean(), b.readFloat(), b.readFloat());
        }
        public void encode(RegistryByteBuf b, JiahaoDodgeStatePayload p) {
            b.writeUuid(p.player()); b.writeIdentifier(p.dimension()); b.writeVarLong(p.action());
            b.writeEnumConstant(p.phase()); b.writeVarInt(p.steps()); vector(b, p.position()); vector(b, p.velocity());
            b.writeBoolean(p.onGround()); b.writeFloat(p.forward()); b.writeFloat(p.sideways());
        }
    };
    static Vec3d vector(RegistryByteBuf b) { return new Vec3d(b.readDouble(), b.readDouble(), b.readDouble()); }
    static void vector(RegistryByteBuf b, Vec3d v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    public Id<? extends CustomPayload> getId() { return ID; }
}
