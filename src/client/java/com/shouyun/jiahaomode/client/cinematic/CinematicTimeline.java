// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.cinematic;

/** Pure timing/math: independent of world time and weather's FrozenRenderClock. */
public final class CinematicTimeline {
    public static final double DURATION = 100;
    private double tick, correctionTarget, lastSample;
    private int authoritative;
    public void start(int elapsed) { tick = lastSample = correctionTarget = authoritative = Math.max(0, elapsed); }
    public void sync(int elapsed) {
        authoritative = Math.max(authoritative, elapsed);
        correctionTarget = Math.max(correctionTarget, elapsed);
    }
    public void tick() {
        tick = Math.min(authoritative + 20, tick + 1 + Math.min(1, Math.max(0, correctionTarget - tick) * 0.2));
    }
    public double sample(float delta) {
        lastSample = Math.max(lastSample, Math.min(DURATION, Math.min(authoritative + 20, tick + clamp(delta))));
        return lastSample;
    }
    public static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    public static double smooth(double value) { double x = clamp(value); return x * x * (3 - 2 * x); }
    public static double cubic(double value) { double x = clamp(value); return x < .5 ? 4*x*x*x : 1 - Math.pow(-2*x+2, 3)/2; }
    public static double lerp(double a, double b, double value) { return a + (b - a) * value; }
    public static double angle(double ticks) { return lerp(135, 360, cubic((ticks - 7) / 77)); }
    public static double radius(double ticks) {
        if (ticks < 32) return lerp(4.5, 4, smooth((ticks - 7) / 25));
        if (ticks < 60) return lerp(4, 3.2, smooth((ticks - 32) / 28));
        return lerp(3.2, 2.2, smooth((ticks - 60) / 24));
    }
    public static double height(double ticks) {
        return ticks < 60 ? lerp(1.2, 1.5, smooth((ticks - 7) / 53)) : lerp(1.5, 1.8, smooth((ticks - 60) / 24));
    }
    public static double weight(double ticks) { return smooth(ticks / 7) * (1 - smooth((ticks - 96) / 4)); }
    public static double bars(double ticks) { return smooth(ticks / 5) * (1 - smooth((ticks - 95) / 5)); }
}
