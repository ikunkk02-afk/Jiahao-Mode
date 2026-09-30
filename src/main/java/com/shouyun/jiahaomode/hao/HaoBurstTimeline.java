// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.hao;

import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
import java.util.*;
import java.util.function.IntUnaryOperator;

/** Shared tick schedule, independent of frozen world clocks and client classes. */
public final class HaoBurstTimeline {
    public static final int DURATION = 240, POSE_START = 14, FINAL_SHOT = 170, TRANSITION = 4;
    private HaoBurstTimeline() {}
    public static List<JiahaoPoseType> poses(IntUnaryOperator random) {
        var pool = new ArrayList<>(Arrays.stream(JiahaoPoseType.values()).filter(p -> p != JiahaoPoseType.DEFAULT).toList());
        int count = 3 + random.applyAsInt(3);
        var result = new ArrayList<JiahaoPoseType>();
        for (int i = 0; i < count; i++) result.add(pool.remove(random.applyAsInt(pool.size())));
        return List.copyOf(result);
    }
    public static int index(double tick, int count) {
        return Math.max(0, Math.min(count - 1, (int)((tick - POSE_START) * count / (FINAL_SHOT - POSE_START))));
    }
    public static double poseStart(int index, int count) { return POSE_START + index * (FINAL_SHOT - POSE_START) / (double) count; }
    public static double smooth(double x) { x = Math.max(0, Math.min(1, x)); return x*x*(3-2*x); }
    public static double transition(double tick, int count) { return smooth((tick - poseStart(index(tick, count), count)) / TRANSITION); }
    public static double poseWeight(double tick) { return smooth((tick - POSE_START)/4) * (1-smooth((tick-234)/6)); }
    public static double cameraWeight(double tick) { return smooth((tick-10)/8) * (1-smooth((tick-200)/20)); }
    public static double musicVolume(double tick) { return .75 * smooth((tick-6)/8) * (1-smooth((tick-220)/20)); }
    public static int[] quoteTicks(int count) {
        return switch(count) { case 2 -> new int[]{70,170}; case 3 -> new int[]{32,104,176}; case 4 -> new int[]{22,76,130,184}; default -> throw new IllegalArgumentException("Quote count"); };
    }
}
