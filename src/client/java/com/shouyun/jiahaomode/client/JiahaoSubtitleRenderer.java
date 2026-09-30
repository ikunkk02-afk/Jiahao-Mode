// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
public final class JiahaoSubtitleRenderer {
 public static void initialize(){HudRenderCallback.EVENT.register((draw,counter)->{
  if(!(MinecraftClient.getInstance().currentScreen instanceof QuoteOverlayScreen))render(draw);
 });}
 /** Entertainment screens draw this same renderer after their background and widgets. */
 public interface QuoteOverlayScreen {}
 public static void render(net.minecraft.client.gui.DrawContext draw){
  var c=MinecraftClient.getInstance();var state=JiahaoQuoteClientState.local();
  if(state==null||state.started==0||c.options.hudHidden)return;
  int alpha=(int)(state.alpha()*255);if(alpha<4)return;
  int w=draw.getScaledWindowWidth(),h=draw.getScaledWindowHeight();
  var lines=state.lines(Math.max(60,w-40));
  // Immediate fixed lines can coincide with vanilla Action Bar feedback; reserve its text row.
  int bottom=h-84;
  if(c.player!=null&&com.shouyun.jiahaomode.state.JiahaoStateManager.isJiahao(c.player))bottom=h-126;
  if(c.currentScreen instanceof QuoteOverlayScreen)bottom=h-10;
  if(JiahaoCinematicController.isCameraActive())bottom=Math.min(bottom,h-Math.round(h*.06f)-20);
  int y=bottom-lines.size()*11;
  for(var line:lines){int x=(w-c.textRenderer.getWidth(line))/2;if(c.currentScreen instanceof QuoteOverlayScreen)draw.fill(x-4,y-2,x+c.textRenderer.getWidth(line)+4,y+10,0xA010171A);draw.drawText(c.textRenderer,line,x,y,(alpha<<24)|0xffffff,true);y+=11;}
 }
}
