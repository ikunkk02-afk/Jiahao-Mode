// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.state;

import com.mojang.serialization.Codec;
import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.PacketCodecs;

/** Registers storage shared by server players and their synchronized client copies. */
public final class JiahaoState {
	static final AttachmentType<Boolean> FORM = AttachmentRegistry.create(
			JiahaoMode.id("jiahao_state"),
			builder -> builder.persistent(Codec.BOOL)
					.copyOnDeath()
					.syncWith(PacketCodecs.BOOL, AttachmentSyncPredicate.all()));

	private JiahaoState() {
	}

	public static void initialize() {
		// Loading this class registers the attachment before player data is loaded.
	}
}
