// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.armor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public record ArmorAckPayload(int stage,String result) implements CustomPayload {
 public static final Id<ArmorAckPayload> ID=new Id<>(Identifier.of("jiahao-armor-smoke","ack"));
 public static final PacketCodec<RegistryByteBuf,ArmorAckPayload> CODEC=new PacketCodec<>() {
  public ArmorAckPayload decode(RegistryByteBuf b){return new ArmorAckPayload(b.readVarInt(),b.readString(1024));}
  public void encode(RegistryByteBuf b,ArmorAckPayload p){b.writeVarInt(p.stage);b.writeString(p.result,1024);}
 };
 public Id<? extends CustomPayload> getId(){return ID;}
}
