// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.visual;

/** A local animation clock; never changes Minecraft's global render tick counter. */
public final class FrozenRenderClock {
	private boolean frozen;
	private double offset;
	private double frozenTime;

	public void freeze(double rawTime) {
		if (frozen) return;
		frozenTime = sample(rawTime);
		frozen = true;
	}
	public void release(double rawTime) {
		if (!frozen) return;
		offset = rawTime - frozenTime;
		frozen = false;
	}
	public double sample(double rawTime) { return frozen ? frozenTime : rawTime - offset; }
	public boolean isFrozen() { return frozen; }
	public boolean hasAdjustment() { return frozen || offset != 0.0; }
	public void reset() { frozen = false; offset = 0; frozenTime = 0; }
	public Phase phase(int ticks, float tickDelta) {
		double time = sample((double) ticks + tickDelta);
		int whole = (int) Math.floor(time);
		return new Phase(whole, (float) (time - whole));
	}
	public record Phase(int ticks, float tickDelta) { }
}
