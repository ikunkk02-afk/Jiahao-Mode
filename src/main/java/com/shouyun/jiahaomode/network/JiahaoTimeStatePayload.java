// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;
import net.minecraft.util.math.Vec3d;

public record JiahaoTimeStatePayload(Identifier dimension, boolean active, UUID owner,
		int remainingTicks, long gameTime, long dayTime, UUID session, int elapsedTicks,
		boolean cinematic, Vec3d origin, float yaw, com.shouyun.jiahaomode.cinematic.JiahaoPoseType pose, com.shouyun.jiahaomode.timestop.TimeStopReason reason) implements CustomPayload {
    public JiahaoTimeStatePayload(Identifier dimension, boolean active, UUID owner, int remainingTicks,
            long gameTime, long dayTime, UUID session, int elapsedTicks, boolean cinematic, Vec3d origin, float yaw,
            com.shouyun.jiahaomode.cinematic.JiahaoPoseType pose) {
        this(dimension,active,owner,remainingTicks,gameTime,dayTime,session,elapsedTicks,cinematic,origin,yaw,pose,
            com.shouyun.jiahaomode.timestop.TimeStopReason.MANUAL);
    }

	public JiahaoTimeStatePayload(Identifier dimension, boolean active, UUID owner, int remainingTicks,
			long gameTime, long dayTime, UUID session, int elapsedTicks, boolean cinematic, Vec3d origin, float yaw) {
		this(dimension, active, owner, remainingTicks, gameTime, dayTime, session, elapsedTicks, cinematic, origin, yaw,
				com.shouyun.jiahaomode.cinematic.JiahaoPoseType.DEFAULT);
	}
	public static final Id<JiahaoTimeStatePayload> ID = new Id<>(JiahaoMode.id("time_state"));
	public static final PacketCodec<RegistryByteBuf, JiahaoTimeStatePayload> CODEC = new PacketCodec<>() {
		@Override public JiahaoTimeStatePayload decode(RegistryByteBuf buf) {
			Identifier dimension = buf.readIdentifier();
			boolean active = buf.readBoolean();
			return new JiahaoTimeStatePayload(dimension, active, active ? buf.readUuid() : null,
					buf.readVarInt(), buf.readLong(), buf.readLong(), buf.readUuid(), buf.readVarInt(),
					buf.readBoolean(), new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat(),
					buf.readEnumConstant(com.shouyun.jiahaomode.cinematic.JiahaoPoseType.class), buf.readEnumConstant(com.shouyun.jiahaomode.timestop.TimeStopReason.class));
		}
		@Override public void encode(RegistryByteBuf buf, JiahaoTimeStatePayload payload) {
			buf.writeIdentifier(payload.dimension());
			buf.writeBoolean(payload.active());
			if (payload.active()) buf.writeUuid(payload.owner());
			buf.writeVarInt(payload.remainingTicks());
			buf.writeLong(payload.gameTime());
			buf.writeLong(payload.dayTime());
			buf.writeUuid(payload.session());
			buf.writeVarInt(payload.elapsedTicks());
			buf.writeBoolean(payload.cinematic());
			buf.writeDouble(payload.origin().x); buf.writeDouble(payload.origin().y); buf.writeDouble(payload.origin().z);
			buf.writeFloat(payload.yaw());
			buf.writeEnumConstant(payload.pose());
            buf.writeEnumConstant(payload.reason());
		}
	};
	@Override public Id<? extends CustomPayload> getId() { return ID; }
}
