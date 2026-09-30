// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import java.util.UUID;
public record HaoGadgetPayload(UUID session, UUID operation, Action action, Phase phase) implements CustomPayload {
    public enum Action { BUY, SELL, CODE }
    public enum Phase { OPEN, PING, CLOSE, BEGIN, GRANT, COMMIT, COMPLETE }
    public static final UUID NONE=new UUID(0,0);
    public static final Id<HaoGadgetPayload> ID=new Id<>(JiahaoMode.id("hao_gadget"));
    public static final PacketCodec<RegistryByteBuf,HaoGadgetPayload> CODEC=new PacketCodec<>() {
        public HaoGadgetPayload decode(RegistryByteBuf b){return new HaoGadgetPayload(b.readUuid(),b.readUuid(),b.readEnumConstant(Action.class),b.readEnumConstant(Phase.class));}
        public void encode(RegistryByteBuf b,HaoGadgetPayload p){b.writeUuid(p.session);b.writeUuid(p.operation);b.writeEnumConstant(p.action);b.writeEnumConstant(p.phase);}
    };
    public Id<? extends CustomPayload> getId(){return ID;}
}
