// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public record JiahaoMomentPreferencePayload(boolean enabled) implements CustomPayload {
    public static final Id<JiahaoMomentPreferencePayload> ID=new Id<>(JiahaoMode.id("moment_preference"));
    public static final PacketCodec<RegistryByteBuf,JiahaoMomentPreferencePayload> CODEC=new PacketCodec<>() {
        public JiahaoMomentPreferencePayload decode(RegistryByteBuf b){return new JiahaoMomentPreferencePayload(b.readBoolean());}
        public void encode(RegistryByteBuf b,JiahaoMomentPreferencePayload p){b.writeBoolean(p.enabled());}
    };
    public Id<? extends CustomPayload> getId(){return ID;}
}
