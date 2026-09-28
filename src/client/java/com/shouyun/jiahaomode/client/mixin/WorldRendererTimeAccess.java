// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererTimeAccess {
	@Accessor("ticks") int jiahao$getRendererTicks();
}
