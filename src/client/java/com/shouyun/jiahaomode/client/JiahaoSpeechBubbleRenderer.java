// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
public final class JiahaoSpeechBubbleRenderer {
 public static boolean inRange(double squaredDistance){return squaredDistance<=32*32;}
 public static void initialize(){WorldRenderEvents.AFTER_ENTITIES.register(context->{
  var c=MinecraftClient.getInstance();if(c.world==null||c.player==null||c.options.hudHidden)return;
  var matrices=context.matrixStack();var vertices=context.consumers();if(matrices==null||vertices==null)return;
  var camera=context.camera();
  for(var entry:JiahaoQuoteClientState.active().entrySet()){
   if(entry.getKey().equals(c.player.getUuid()))continue;
   var player=c.world.getPlayerByUuid(entry.getKey());var state=entry.getValue();
   if(player==null||player.isInvisibleTo(c.player)||!inRange(player.getPos().squaredDistanceTo(camera.getPos()))||state.started==0)continue;
   int alpha=(int)(state.alpha()*255);if(alpha<4)continue;
   var pos=player.getLerpedPos(context.tickCounter().getTickDelta(false)).add(0,player.getHeight()+.95,0).subtract(camera.getPos());
   matrices.push();matrices.translate(pos.x,pos.y,pos.z);matrices.multiply(camera.getRotation());matrices.scale(.025f,-.025f,.025f);
   var lines=state.lines(220);int y=-(lines.size()-1)*10;
   for(var line:lines){c.textRenderer.draw(line,-c.textRenderer.getWidth(line)/2f,y,(alpha<<24)|0xffffff,false,matrices.peek().getPositionMatrix(),vertices,TextRenderer.TextLayerType.NORMAL,((int)(alpha*.45))<<24,LightmapTextureManager.MAX_LIGHT_COORDINATE);y+=10;}
   matrices.pop();
  }
 });}
}
