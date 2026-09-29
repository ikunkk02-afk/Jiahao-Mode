// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** A closed set of entertainment cues. Never carries terminal input. */
public record JiahaoGadgetQuoteRequestPayload(Kind kind) implements CustomPayload {
    public enum Kind { MARKET, CODE }
    public static final Id<JiahaoGadgetQuoteRequestPayload> ID = new Id<>(JiahaoMode.id("gadget_quote_request"));
    public static final PacketCodec<RegistryByteBuf, JiahaoGadgetQuoteRequestPayload> CODEC = new PacketCodec<>() {
        public JiahaoGadgetQuoteRequestPayload decode(RegistryByteBuf b) { return new JiahaoGadgetQuoteRequestPayload(b.readEnumConstant(Kind.class)); }
        public void encode(RegistryByteBuf b, JiahaoGadgetQuoteRequestPayload p) { b.writeEnumConstant(p.kind()); }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
