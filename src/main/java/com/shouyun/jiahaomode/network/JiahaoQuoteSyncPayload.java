// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
/** Null quote means cancel; optional session defers a legacy cinematic cue, scripted lines display immediately. */
public record JiahaoQuoteSyncPayload(UUID player, Identifier dimension, Identifier quote,long event,UUID session) implements CustomPayload {
 public static final Id<JiahaoQuoteSyncPayload> ID=new Id<>(JiahaoMode.id("quote_sync"));
 public static final PacketCodec<RegistryByteBuf,JiahaoQuoteSyncPayload> CODEC=new PacketCodec<>() {
  public JiahaoQuoteSyncPayload decode(RegistryByteBuf b){return new JiahaoQuoteSyncPayload(b.readUuid(),b.readIdentifier(),b.readBoolean()?b.readIdentifier():null,b.readLong(),b.readBoolean()?b.readUuid():null);}
  public void encode(RegistryByteBuf b,JiahaoQuoteSyncPayload p){b.writeUuid(p.player);b.writeIdentifier(p.dimension);b.writeBoolean(p.quote!=null);if(p.quote!=null)b.writeIdentifier(p.quote);b.writeLong(p.event);b.writeBoolean(p.session!=null);if(p.session!=null)b.writeUuid(p.session);}
 };
 public Id<? extends CustomPayload> getId(){return ID;}
}
