// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import java.util.UUID;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
/** Positive reply only confirms a pending proposal; negative reply also cancels the owned session. */
public record JiahaoMomentResponsePayload(UUID session,boolean ready) implements CustomPayload {
    public static final Id<JiahaoMomentResponsePayload> ID=new Id<>(JiahaoMode.id("moment_response"));
    public static final PacketCodec<RegistryByteBuf,JiahaoMomentResponsePayload> CODEC=new PacketCodec<>() {
        public JiahaoMomentResponsePayload decode(RegistryByteBuf b){return new JiahaoMomentResponsePayload(b.readUuid(),b.readBoolean());}
        public void encode(RegistryByteBuf b,JiahaoMomentResponsePayload p){b.writeUuid(p.session());b.writeBoolean(p.ready());}
    };
    public Id<? extends CustomPayload> getId(){return ID;}
}
