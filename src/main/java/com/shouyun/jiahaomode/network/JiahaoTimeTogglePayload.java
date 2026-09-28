// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Only a request: player identity and all decisions come from the server. */
public record JiahaoTimeTogglePayload() implements CustomPayload {
	public static final JiahaoTimeTogglePayload INSTANCE = new JiahaoTimeTogglePayload();
	public static final Id<JiahaoTimeTogglePayload> ID = new Id<>(JiahaoMode.id("time_toggle"));
	public static final PacketCodec<RegistryByteBuf, JiahaoTimeTogglePayload> CODEC = PacketCodec.unit(INSTANCE);
	@Override public Id<? extends CustomPayload> getId() { return ID; }
}
