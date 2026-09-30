// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
public final class HaoCapture {
    public static String pending;
    public static void capture(){if(pending==null)return;var c=MinecraftClient.getInstance();String name=pending;pending=null;ScreenshotRecorder.saveScreenshot(c.runDirectory,name,c.getFramebuffer(),t->{});}
}
