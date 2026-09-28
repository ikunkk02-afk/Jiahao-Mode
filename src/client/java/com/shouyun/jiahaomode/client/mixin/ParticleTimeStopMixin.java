// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleTimeStopMixin implements JiahaoTimeStopParticle {
	@Unique private boolean jiahao$timeStopImmune;
	public boolean jiahao$isTimeStopImmune() { return jiahao$timeStopImmune; }
	public void jiahao$setTimeStopImmune(boolean immune) { jiahao$timeStopImmune = immune; }
}
