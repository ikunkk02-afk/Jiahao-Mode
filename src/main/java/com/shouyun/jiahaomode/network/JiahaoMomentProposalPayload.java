// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import java.util.UUID;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.util.Identifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public record JiahaoMomentProposalPayload(UUID session,Identifier dimension) implements CustomPayload {
    public static final Id<JiahaoMomentProposalPayload> ID=new Id<>(JiahaoMode.id("moment_proposal"));
    public static final PacketCodec<RegistryByteBuf,JiahaoMomentProposalPayload> CODEC=new PacketCodec<>() {
        public JiahaoMomentProposalPayload decode(RegistryByteBuf b){return new JiahaoMomentProposalPayload(b.readUuid(),b.readIdentifier());}
        public void encode(RegistryByteBuf b,JiahaoMomentProposalPayload p){b.writeUuid(p.session());b.writeIdentifier(p.dimension());}
    };
    public Id<? extends CustomPayload> getId(){return ID;}
}
