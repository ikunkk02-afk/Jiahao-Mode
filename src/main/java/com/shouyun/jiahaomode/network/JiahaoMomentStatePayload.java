// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import java.util.UUID;
import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public record JiahaoMomentStatePayload(UUID player,Identifier dimension,UUID session,boolean active,int elapsed,
        JiahaoPoseType pose,Vec3d origin,float yaw,int arc) implements CustomPayload {
    public static final Id<JiahaoMomentStatePayload> ID=new Id<>(JiahaoMode.id("moment_state"));
    public static final PacketCodec<RegistryByteBuf,JiahaoMomentStatePayload> CODEC=new PacketCodec<>() {
        public JiahaoMomentStatePayload decode(RegistryByteBuf b){return new JiahaoMomentStatePayload(b.readUuid(),b.readIdentifier(),b.readUuid(),
            b.readBoolean(),b.readVarInt(),b.readEnumConstant(JiahaoPoseType.class),new Vec3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readVarInt());}
        public void encode(RegistryByteBuf b,JiahaoMomentStatePayload p){b.writeUuid(p.player());b.writeIdentifier(p.dimension());b.writeUuid(p.session());
            b.writeBoolean(p.active());b.writeVarInt(p.elapsed());b.writeEnumConstant(p.pose());b.writeDouble(p.origin().x);b.writeDouble(p.origin().y);b.writeDouble(p.origin().z);b.writeFloat(p.yaw());b.writeVarInt(p.arc());}
    };
    public Id<? extends CustomPayload> getId(){return ID;}
}
