// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;

public record JiahaoTimeStatePayload(Identifier dimension, boolean active, UUID owner,
		int remainingTicks, long gameTime, long dayTime) implements CustomPayload {
	public static final Id<JiahaoTimeStatePayload> ID = new Id<>(JiahaoMode.id("time_state"));
	public static final PacketCodec<RegistryByteBuf, JiahaoTimeStatePayload> CODEC = new PacketCodec<>() {
		@Override public JiahaoTimeStatePayload decode(RegistryByteBuf buf) {
			Identifier dimension = buf.readIdentifier();
			boolean active = buf.readBoolean();
			return new JiahaoTimeStatePayload(dimension, active, active ? buf.readUuid() : null,
					buf.readVarInt(), buf.readLong(), buf.readLong());
		}
		@Override public void encode(RegistryByteBuf buf, JiahaoTimeStatePayload payload) {
			buf.writeIdentifier(payload.dimension());
			buf.writeBoolean(payload.active());
			if (payload.active()) buf.writeUuid(payload.owner());
			buf.writeVarInt(payload.remainingTicks());
			buf.writeLong(payload.gameTime());
			buf.writeLong(payload.dayTime());
		}
	};
	@Override public Id<? extends CustomPayload> getId() { return ID; }
}
