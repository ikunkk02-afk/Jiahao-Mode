// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
public final class JiahaoSubtitleRenderer {
 public static void initialize(){HudRenderCallback.EVENT.register((draw,counter)->{
  var c=MinecraftClient.getInstance();var state=JiahaoQuoteClientState.local();
  if(state==null||state.started==0||c.options.hudHidden)return;
  int alpha=(int)(state.alpha()*255);if(alpha<4)return;
  int w=draw.getScaledWindowWidth(),h=draw.getScaledWindowHeight();
  var lines=state.lines(Math.max(60,w-40));
  int bottom=h-64;
  if(JiahaoCinematicController.isCameraActive())bottom=Math.min(bottom,h-Math.round(h*.06f)-20);
  int y=bottom-lines.size()*11;
  for(var line:lines){draw.drawText(c.textRenderer,line,(w-c.textRenderer.getWidth(line))/2,y,(alpha<<24)|0xffffff,true);y+=11;}
 });}
}
