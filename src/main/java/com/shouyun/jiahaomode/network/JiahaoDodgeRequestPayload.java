// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** W=1, S=2, A=4, D=8. No position, yaw or perfect claim is accepted. */
public record JiahaoDodgeRequestPayload(int inputMask) implements CustomPayload {
    public static final Id<JiahaoDodgeRequestPayload> ID = new Id<>(JiahaoMode.id("dodge_request"));
    public static final PacketCodec<RegistryByteBuf, JiahaoDodgeRequestPayload> CODEC = new PacketCodec<>() {
        public JiahaoDodgeRequestPayload decode(RegistryByteBuf b) { return new JiahaoDodgeRequestPayload(b.readUnsignedByte()); }
        public void encode(RegistryByteBuf b, JiahaoDodgeRequestPayload p) { b.writeByte(p.inputMask()); }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
