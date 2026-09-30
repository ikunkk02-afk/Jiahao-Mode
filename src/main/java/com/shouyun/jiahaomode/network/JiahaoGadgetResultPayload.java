// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.buff.JiahaoBuffManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import java.util.UUID;

/** S2C only. A client never supplies the chosen effect, level or duration. */
public record JiahaoGadgetResultPayload(UUID session, UUID operation, JiahaoBuffManager.Result result) implements CustomPayload {
    public static final Id<JiahaoGadgetResultPayload> ID = new Id<>(JiahaoMode.id("gadget_result"));
    public static final PacketCodec<RegistryByteBuf,JiahaoGadgetResultPayload> CODEC = new PacketCodec<>() {
        public JiahaoGadgetResultPayload decode(RegistryByteBuf buf) {
            var session = buf.readUuid(); var operation = buf.readUuid();
            var status = buf.readEnumConstant(JiahaoBuffManager.Status.class);
            var effect = buf.readBoolean() ? buf.readIdentifier() : null;
            return new JiahaoGadgetResultPayload(session,operation,new JiahaoBuffManager.Result(
                    status,effect,buf.readVarInt(),buf.readVarInt(),buf.readBoolean()));
        }
        public void encode(RegistryByteBuf buf, JiahaoGadgetResultPayload p) {
            buf.writeUuid(p.session()); buf.writeUuid(p.operation()); buf.writeEnumConstant(p.result().status());
            buf.writeBoolean(p.result().effect() != null);
            if (p.result().effect() != null) buf.writeIdentifier(p.result().effect());
            buf.writeVarInt(p.result().amplifier()); buf.writeVarInt(p.result().durationTicks()); buf.writeBoolean(p.result().override());
        }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
