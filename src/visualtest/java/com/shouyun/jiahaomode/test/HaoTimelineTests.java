// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.hao.HaoBurstTimeline;
import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
import java.util.*;
public final class HaoTimelineTests {
    public static void main(String[] args) {
        for(int seed=0;seed<1000;seed++) {
            var random=new Random(seed);var poses=HaoBurstTimeline.poses(random::nextInt);
            check(poses.size()>=3&&poses.size()<=5&&new HashSet<>(poses).size()==poses.size(),"Unique sequence");
            check(!poses.contains(JiahaoPoseType.DEFAULT),"No placeholder");
            for(int i=0;i<poses.size();i++) {
                double start=HaoBurstTimeline.poseStart(i,poses.size());
                check(HaoBurstTimeline.index(start+.001,poses.size())==i,"Shared pose boundary");
                check(HaoBurstTimeline.transition(start+4.001,poses.size())==1,"Static hold after transition");
                check(HaoBurstTimeline.poseWeight(start+4.001)==1,"Full static weight");
            }
        }
        for(int count=2;count<=4;count++) {
            var ticks=HaoBurstTimeline.quoteTicks(count);check(ticks.length==count,"Quote count");
            for(int i=1;i<count;i++)check(ticks[i]-ticks[i-1]>=50,"Display leases never overlap");
            check(ticks[count-1]+50<=240,"Last quote fits the session");
        }
        check(HaoBurstTimeline.cameraWeight(10)==0&&HaoBurstTimeline.cameraWeight(18)==1,"Camera entry");
        check(HaoBurstTimeline.cameraWeight(200)==1&&HaoBurstTimeline.cameraWeight(220)==0,"Camera return");
        check(HaoBurstTimeline.musicVolume(6)==0&&HaoBurstTimeline.musicVolume(14)==.75,"Music fade in");
        check(HaoBurstTimeline.musicVolume(220)==.75&&HaoBurstTimeline.musicVolume(240)==0,"Music fade out");
        System.out.println("Hao sequence, static pose holds, quote timing, camera and sound fades passed");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
