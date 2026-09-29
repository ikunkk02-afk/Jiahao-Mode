// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public record JiahaoQuoteRequestPayload() implements CustomPayload {
 public static final JiahaoQuoteRequestPayload INSTANCE=new JiahaoQuoteRequestPayload();
 public static final Id<JiahaoQuoteRequestPayload> ID=new Id<>(JiahaoMode.id("quote_request"));
 public static final PacketCodec<RegistryByteBuf,JiahaoQuoteRequestPayload> CODEC=PacketCodec.unit(INSTANCE);
 public Id<? extends CustomPayload> getId(){return ID;}
}
