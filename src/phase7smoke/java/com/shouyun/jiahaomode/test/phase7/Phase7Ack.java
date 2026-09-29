package com.shouyun.jiahaomode.test.phase7;
import net.minecraft.network.*;import net.minecraft.network.codec.PacketCodec;import net.minecraft.network.packet.CustomPayload;import net.minecraft.util.Identifier;
public record Phase7Ack(int stage,String result) implements CustomPayload {
 public static final Id<Phase7Ack> ID=new Id<>(Identifier.of("jiahao-phase7-smoke","ack"));
 public static final PacketCodec<RegistryByteBuf,Phase7Ack> CODEC=new PacketCodec<>(){
  public Phase7Ack decode(RegistryByteBuf b){return new Phase7Ack(b.readVarInt(),b.readString(512));}
  public void encode(RegistryByteBuf b,Phase7Ack p){b.writeVarInt(p.stage);b.writeString(p.result,512);}
 };
 public Id<? extends CustomPayload> getId(){return ID;}
}
