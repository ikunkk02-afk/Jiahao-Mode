// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.client.visual.FrozenRenderClock;

/** Standalone animation continuity checks; no Minecraft bootstrap or extra test dependency. */
public final class FrozenRenderClockTests {
	public static void main(String[] args) {
		FrozenRenderClock clock = new FrozenRenderClock();
		check(clock.sample(100.72) == 100.72, "Untouched clock must use vanilla time exactly");
		clock.freeze(100.72);
		for (int tick = 101; tick <= 260; tick++) {
			for (float delta : new float[] {0, 0.25F, 0.9F}) {
				var phase = clock.phase(tick, delta);
				near(phase.ticks() + phase.tickDelta(), 100.72, "Weather/cloud phase stays fixed across client ticks and frames");
				near(cloud(phase), 100.72 * 0.03, "Cloud world-space displacement stays fixed regardless of camera position");
				near(rain(phase, 173, 3.5F), rain(clock.phase(100, 0.72F), 173, 3.5F), "Rain UV phase stays fixed");
				near(snow(phase), snow(clock.phase(100, 0.72F)), "Snow falling phase stays fixed");
			}
		}
		clock.freeze(200.0); near(clock.sample(260), 100.72, "Periodic active sync must not recapture");
		clock.release(260.19);
		near(clock.sample(260.19), 100.72, "Release must not catch up the hidden 8 seconds");
		near(clock.sample(261.19), 101.72, "Animation continues at vanilla speed");
		clock.freeze(280.54); double second = clock.sample(280.54);
		clock.release(340.12); near(clock.sample(340.12), second, "Repeated pauses accumulate fractional offsets");
		clock.reset(); check(!clock.isFrozen() && !clock.hasAdjustment() && clock.sample(0.5) == 0.5, "World replacement clears all visual state");
		clock.freeze(131071.8); clock.release(131231.2);
		near(rain(clock.phase(131231, 0.2F), 7, 3), rain(new FrozenRenderClock().phase(131071, 0.8F), 7, 3), "Rain integer mask boundary remains continuous");
		System.out.println("Frozen render clock rain, snow, clouds, fractional resume and reset checks passed");
	}
	private static double cloud(FrozenRenderClock.Phase p) { return (p.ticks() + (double) p.tickDelta()) * 0.03; }
	private static double rain(FrozenRenderClock.Phase p, int columnSeed, float speed) { return (-((p.ticks() & 131071) + columnSeed + (double) p.tickDelta()) / 32.0 * speed) % 32; }
	private static double snow(FrozenRenderClock.Phase p) { return -((p.ticks() & 511) + (double) p.tickDelta()) / 512.0; }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
	private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < 0.00001, message + ": " + actual + " != " + expected); }
}
