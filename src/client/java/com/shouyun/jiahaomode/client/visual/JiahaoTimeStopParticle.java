// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.visual;

/** Implemented on vanilla Particle by a client-only Mixin. Future skill particles opt out. */
public interface JiahaoTimeStopParticle {
	boolean jiahao$isTimeStopImmune();
	void jiahao$setTimeStopImmune(boolean immune);
}
