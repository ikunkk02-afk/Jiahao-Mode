// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.cinematic;

import java.util.function.IntUnaryOperator;

/** Immutable degrees: head, body, right arm, left arm, right leg, left leg. */
public enum JiahaoPoseType {
    DEFAULT(0, new double[][]{{-12,-5,-4},{-8,0,-3},{-112,-30,22},{8,0,-8},{0,0,0},{0,0,0}}),
    RUNNING_FREEZE(1, new double[][]{{-32,0,0},{20,0,0},{-75,0,-12},{60,0,12},{50,0,0},{-55,0,0}}),
    LEAN_BACK(3, new double[][]{{-25,10,-4},{-15,0,0},{-65,-20,-18},{8,0,8},{-12,0,0},{6,0,0}}),
    POINT_SKY(3, new double[][]{{-22,12,0},{-5,0,0},{-165,0,-12},{5,0,7},{-5,0,0},{5,0,0}}),
    LOOK_DISTANCE(3, new double[][]{{-8,-60,0},{0,35,0},{-30,0,-8},{25,0,8},{-8,0,0},{8,0,0}}),
    THINKING_HAO(3, new double[][]{{20,-10,-5},{3,15,0},{-125,-40,-8},{-65,35,15},{-4,0,0},{4,0,0}}),
    RUNNING_LOOK_BACK(1, new double[][]{{-32,135,0},{20,0,0},{-75,0,-12},{60,0,12},{50,0,0},{-55,0,0}}),
    RAIN_EMBRACE(3, new double[][]{{-28,0,0},{-10,0,0},{-12,0,48},{-12,0,-48},{0,0,7},{0,0,-7}});

    private final double[][] rotations;
    public final int momentWeight;
    JiahaoPoseType(int weight, double[][] rotations) { this.momentWeight = weight; this.rotations = rotations; }
    public double degrees(int part, int axis) { return rotations[part][axis]; }
    public static JiahaoPoseType timeStop(JiahaoPoseType last, IntUnaryOperator random) {
        int count = last == null || last == DEFAULT ? 7 : 6;
        int choice = random.applyAsInt(count);
        for (var pose : values()) if (pose != DEFAULT && pose != last && choice-- == 0) return pose;
        return POINT_SKY;
    }
    public static JiahaoPoseType moment(IntUnaryOperator random) {
        int choice = random.applyAsInt(17);
        for (var pose : values()) { choice -= pose.momentWeight; if (choice < 0) return pose; }
        return POINT_SKY;
    }
}
