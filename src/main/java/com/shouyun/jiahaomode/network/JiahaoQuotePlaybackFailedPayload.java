// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import java.util.UUID;
public record JiahaoQuotePlaybackFailedPayload(UUID session) implements CustomPayload {
 public static final Id<JiahaoQuotePlaybackFailedPayload> ID=new Id<>(JiahaoMode.id("quote_playback_failed"));
 public static final PacketCodec<RegistryByteBuf,JiahaoQuotePlaybackFailedPayload> CODEC=new PacketCodec<>() {
  public JiahaoQuotePlaybackFailedPayload decode(RegistryByteBuf b){return new JiahaoQuotePlaybackFailedPayload(b.readUuid());}
  public void encode(RegistryByteBuf b,JiahaoQuotePlaybackFailedPayload p){b.writeUuid(p.session);}
 };
 public Id<? extends CustomPayload> getId(){return ID;}
}
