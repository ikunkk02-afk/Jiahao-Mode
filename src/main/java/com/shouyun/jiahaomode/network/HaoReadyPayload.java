// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
/** S2C proposal and C2S reply share a codec; only the server creates tokens. */
public record HaoReadyPayload(UUID token, Identifier dimension, boolean ready) implements CustomPayload {
    public static final Id<HaoReadyPayload> ID = new Id<>(JiahaoMode.id("hao_ready"));
    public static final PacketCodec<RegistryByteBuf,HaoReadyPayload> CODEC = new PacketCodec<>() {
        public HaoReadyPayload decode(RegistryByteBuf b) { return new HaoReadyPayload(b.readUuid(),b.readIdentifier(),b.readBoolean()); }
        public void encode(RegistryByteBuf b,HaoReadyPayload p) { b.writeUuid(p.token);b.writeIdentifier(p.dimension);b.writeBoolean(p.ready); }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
