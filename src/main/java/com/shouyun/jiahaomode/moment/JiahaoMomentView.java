// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.moment;
import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.fabric.api.attachment.v1.*;
/** Client-only transient lock, initialized in common code without any renderer dependencies. */
public final class JiahaoMomentView {
    public static final AttachmentType<Boolean> LOCKED=AttachmentRegistry.create(JiahaoMode.id("moment_lock"),b->{});
    public static void initialize() {}
    private JiahaoMomentView() {}
}
