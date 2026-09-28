// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.timestop;

import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import java.util.UUID;

/** Client snapshot only. Neither this attachment nor active stops are persistent. */
public record JiahaoTimeView(boolean active, UUID owner, int remainingTicks) {
	public static final JiahaoTimeView INACTIVE = new JiahaoTimeView(false, null, 0);
	public static final AttachmentType<JiahaoTimeView> CLIENT_VIEW = AttachmentRegistry.create(
			JiahaoMode.id("time_stop_view"), builder -> { });
	public static void initialize() { }
}
