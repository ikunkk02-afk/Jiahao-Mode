// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.client.cinematic.CinematicTimeline;

public final class CinematicTimelineTests {
    public static void main(String[] args) {
        near(CinematicTimeline.angle(7), 135); near(CinematicTimeline.angle(84), 360);
        near(CinematicTimeline.radius(7), 4.5); near(CinematicTimeline.radius(32), 4);
        near(CinematicTimeline.radius(60), 3.2); near(CinematicTimeline.radius(84), 2.2);
        near(CinematicTimeline.height(7), 1.2); near(CinematicTimeline.height(84), 1.8);
        near(CinematicTimeline.weight(0), 0); near(CinematicTimeline.weight(50), 1); near(CinematicTimeline.weight(100), 0);
        near(CinematicTimeline.bars(0), 0); near(CinematicTimeline.bars(100), 0);
        for (int fps : new int[]{30,60,180}) {
            CinematicTimeline timeline = new CinematicTimeline(); timeline.start(0);
            double previous = -1, previousAngle = 135;
            int logic = 0, movingSamples = 0;
            for (int frame = 0; frame < 5 * fps; frame++) {
                double ticks = frame * 20.0 / fps;
                while (logic < (int) ticks) {
                    timeline.tick(); logic++;
                    if (logic % 20 == 0) timeline.sync(logic);
                }
                double value = timeline.sample((float) (ticks - logic));
                check(value >= previous, "Progress never reverses at " + fps + " FPS");
                near(value, ticks);
                double angle = CinematicTimeline.angle(value);
                check(angle >= previousAngle, "Orbit never reverses");
                if (angle > previousAngle) movingSamples++;
                previous = value; previousAngle = angle;
            }
            check(movingSamples > fps * 3, "Orbit must advance at render cadence, not only 20 TPS");
            timeline.sync(0); check(timeline.sample(0) >= previous, "Duplicate/old sync must not restart");
        }
        CinematicTimeline lag = new CinematicTimeline(); lag.start(0);
        for (int tick = 0; tick < 100; tick++) lag.tick();
        near(lag.sample(.9F), 20); // no complete cinematic based on unconfirmed server progress
        lag.sync(40); double before = lag.sample(0); lag.tick();
        check(lag.sample(0) > before && lag.sample(0) <= before + 2, "Forward correction is bounded");
        lag.start(63); near(lag.sample(0), 63);
        for (double boundary : new double[]{7,32,60,84,95,96}) {
            check(Math.abs(CinematicTimeline.radius(boundary+.0001)-CinematicTimeline.radius(boundary-.0001)) < .001, "Radius continuous");
            check(Math.abs(CinematicTimeline.angle(boundary+.0001)-CinematicTimeline.angle(boundary-.0001)) < .01, "Angle continuous");
        }
        System.out.println("Cinematic timing, high-FPS orbit, duplicate sync, late join, lag and easing tests passed");
    }
    private static void near(double a, double b) { check(Math.abs(a-b) < .00001, a + " != " + b); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
