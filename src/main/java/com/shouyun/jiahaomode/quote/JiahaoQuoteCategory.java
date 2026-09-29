// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.quote;
public enum JiahaoQuoteCategory {
 MANUAL(10,8), TRANSFORM(20,4), TIME_STOP_START(30,5), TIME_STOP_END(30,4),
 CINEMATIC(40,1), LOW_HEALTH(20,4), TAKE_DAMAGE(10,4), KILL_ENTITY(20,4), IDLE(10,3);
 public final int priority, count;
 JiahaoQuoteCategory(int priority,int count) { this.priority=priority; this.count=count; }
}
