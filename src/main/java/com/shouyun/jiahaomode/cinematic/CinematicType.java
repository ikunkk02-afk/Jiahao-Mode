// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.cinematic;
public enum CinematicType {
    HAO_BURST(4,240), TIME_STOP(3,100), RANDOM_HAO_MOMENT(2,60), PERFECT_DODGE(1,6);
    public final int priority, duration;
    CinematicType(int priority,int duration) { this.priority=priority;this.duration=duration; }
}
