// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.armor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
/** Test coordination only; gameplay continues to use the production packets and item interactions. */
public record ArmorStagePayload(int stage) implements CustomPayload {
 public static final Id<ArmorStagePayload> ID=new Id<>(Identifier.of("jiahao-armor-smoke","stage"));
 public static final PacketCodec<RegistryByteBuf,ArmorStagePayload> CODEC=new PacketCodec<>() {
  public ArmorStagePayload decode(RegistryByteBuf b){return new ArmorStagePayload(b.readVarInt());}
  public void encode(RegistryByteBuf b,ArmorStagePayload p){b.writeVarInt(p.stage);}
 };
 public Id<? extends CustomPayload> getId(){return ID;}
}
