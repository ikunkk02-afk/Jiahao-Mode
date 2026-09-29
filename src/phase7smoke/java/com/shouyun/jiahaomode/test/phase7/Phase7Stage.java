package com.shouyun.jiahaomode.test.phase7;
import net.minecraft.network.*;import net.minecraft.network.codec.PacketCodec;import net.minecraft.network.packet.CustomPayload;import net.minecraft.util.Identifier;import java.util.UUID;
public record Phase7Stage(int stage,UUID actor,UUID observer) implements CustomPayload {
 public static final Id<Phase7Stage> ID=new Id<>(Identifier.of("jiahao-phase7-smoke","stage"));
 public static final PacketCodec<RegistryByteBuf,Phase7Stage> CODEC=new PacketCodec<>(){
  public Phase7Stage decode(RegistryByteBuf b){return new Phase7Stage(b.readVarInt(),b.readUuid(),b.readUuid());}
  public void encode(RegistryByteBuf b,Phase7Stage p){b.writeVarInt(p.stage);b.writeUuid(p.actor);b.writeUuid(p.observer);}
 };
 public Id<? extends CustomPayload> getId(){return ID;}
}
