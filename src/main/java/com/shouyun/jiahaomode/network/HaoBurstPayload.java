// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;
public record HaoBurstPayload(UUID player, Identifier dimension, UUID session, boolean active, int elapsed,
        List<JiahaoPoseType> poses, Vec3d origin, float yaw) implements CustomPayload {
    public HaoBurstPayload { poses = List.copyOf(poses); if(poses.size()<3 || poses.size()>5)throw new IllegalArgumentException("Burst poses"); }
    public static final Id<HaoBurstPayload> ID = new Id<>(JiahaoMode.id("hao_burst"));
    public static final PacketCodec<RegistryByteBuf,HaoBurstPayload> CODEC = new PacketCodec<>() {
        public HaoBurstPayload decode(RegistryByteBuf b) {
            var player=b.readUuid();var dimension=b.readIdentifier();var session=b.readUuid();boolean active=b.readBoolean();int elapsed=b.readVarInt(),count=b.readVarInt();
            if(count<3||count>5)throw new IllegalArgumentException("Burst poses");
            var poses=new ArrayList<JiahaoPoseType>();for(int i=0;i<count;i++)poses.add(b.readEnumConstant(JiahaoPoseType.class));
            return new HaoBurstPayload(player,dimension,session,active,elapsed,poses,new Vec3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat());
        }
        public void encode(RegistryByteBuf b,HaoBurstPayload p) {
            b.writeUuid(p.player);b.writeIdentifier(p.dimension);b.writeUuid(p.session);b.writeBoolean(p.active);b.writeVarInt(p.elapsed);b.writeVarInt(p.poses.size());
            for(var pose:p.poses)b.writeEnumConstant(pose);b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);b.writeFloat(p.yaw);
        }
    };
    public Id<? extends CustomPayload> getId() { return ID; }
}
