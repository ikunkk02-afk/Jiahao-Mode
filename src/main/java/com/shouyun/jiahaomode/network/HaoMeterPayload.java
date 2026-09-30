// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public record HaoMeterPayload(int units, boolean pending, boolean bursting) implements CustomPayload {
    public static final Id<HaoMeterPayload> ID = new Id<>(JiahaoMode.id("hao_meter"));
    public static final PacketCodec<RegistryByteBuf,HaoMeterPayload> CODEC = new PacketCodec<>() {
        public HaoMeterPayload decode(RegistryByteBuf b) { return new HaoMeterPayload(b.readVarInt(),b.readBoolean(),b.readBoolean()); }
        public void encode(RegistryByteBuf b,HaoMeterPayload p) { b.writeVarInt(p.units);b.writeBoolean(p.pending);b.writeBoolean(p.bursting); }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
