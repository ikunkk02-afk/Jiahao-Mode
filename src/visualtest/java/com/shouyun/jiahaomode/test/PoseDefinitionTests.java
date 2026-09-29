// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
public final class PoseDefinitionTests {
    public static void main(String[] args) {
        int[] counts = new int[8];
        for (int i = 0; i < 17; i++) { final int ticket = i; counts[JiahaoPoseType.moment(bound -> ticket).ordinal()]++; }
        for (var pose : JiahaoPoseType.values()) {
            check(counts[pose.ordinal()] == pose.momentWeight, "Weighted distribution: " + pose);
            for (int ticket = 0; ticket < (pose == JiahaoPoseType.DEFAULT ? 7 : 6); ticket++) {
                final int index = ticket;
                var next = JiahaoPoseType.timeStop(pose, bound -> index);
                check(next != pose && next != JiahaoPoseType.DEFAULT, "No repeat or placeholder");
            }
            for (int p = 0; p < 6; p++) for (int a = 0; a < 3; a++) check(Double.isFinite(pose.degrees(p, a)), "Finite pose");
        }
        var run = JiahaoPoseType.RUNNING_FREEZE;
        check(run.degrees(1,0) == 20 && run.degrees(4,0) > 0 && run.degrees(5,0) < 0, "Run leans with opposite legs");
        check(run.degrees(2,0) < 0 && run.degrees(3,0) > 0, "Opposite arms");
        check(run.degrees(0,0)+run.degrees(1,0)<0,"Head lifts above the tilted torso");
        check(JiahaoPoseType.RUNNING_LOOK_BACK.degrees(0,1) == 135, "Fixed head looks behind");
        System.out.println("Pose presets, no-repeat selection and exact rare-pose weights passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
