// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.cinematic;
import net.minecraft.entity.Entity;
public final class JiahaoCinematicLocks {
    private JiahaoCinematicLocks() {}
    public static boolean isLocked(Entity entity) {
        return com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.isCinematicLocked(entity)
                || com.shouyun.jiahaomode.moment.JiahaoMomentManager.isLocked(entity);
    }
}
