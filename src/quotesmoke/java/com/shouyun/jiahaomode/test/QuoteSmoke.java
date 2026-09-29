// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.mojang.authlib.GameProfile;
import com.shouyun.jiahaomode.client.*;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.*;
import java.nio.file.*;
import java.util.*;
public final class QuoteSmoke implements ClientModInitializer {
 private int stage,ticks,total;private boolean cueSeen,fadeIn,fadeOut;private long cueStart,manualStart;private net.minecraft.util.Identifier manualId;
 private static String pendingShot;
 private final Set<String> shots=new HashSet<>();private String failure;
 public void onInitializeClient(){
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
  HudRenderCallback.EVENT.register((draw,counter)->{
   try {
    var c=MinecraftClient.getInstance();var q=JiahaoQuoteClientState.local();
    if(stage==1&&q!=null&&q.started!=0){
     if(q.quote.id().equals(JiahaoQuoteRegistry.TIME_STOP_NOTICE)){
      check(q.session==null,"Fixed notice displays without cinematic timeline wait");
      check(JiahaoTimeStopClientState.isTimeStopped(c.world),"Cue displayed in frozen world");
      if(!cueSeen){cueSeen=true;cueStart=q.started;}else check(cueStart==q.started,"Same cinematic starts once");
      if(q.age()>.01&&q.age()<.14&&q.alpha()>0&&q.alpha()<1)fadeIn=true;
      if(q.age()>2.76&&q.age()<2.99&&q.alpha()>0&&q.alpha()<1)fadeOut=true;
      if(q.age()>.4)shot(c,"quote-cinematic.png");
     } else if(q.age()>.3)shot(c,"quote-"+q.quote.category().name().toLowerCase()+".png");
    }
    if(stage==2&&ticks>5&&ticks<35)shot(c,"quote-speech-bubble.png");
   }catch(Throwable e){failure=e.toString();}
  });
 }
 private void tick(MinecraftClient c){
  try {
   if(failure!=null)throw new AssertionError(failure);
   check(++total<1800,"Watchdog");if(c.isPaused()||c.getOverlay()!=null)return;ticks++;
   if(stage==0){
    if(c.player==null||c.world==null||c.getServer()==null||ticks<60)return;
    c.setScreen(null);c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(120);c.options.language="zh_cn";
    check(JiahaoQuoteClientState.opacity(0,2.5)==0&&JiahaoQuoteClientState.opacity(.15,2.5)==1&&JiahaoQuoteClientState.opacity(2.5,2.5)==0,"Fade endpoints");
    check(JiahaoSpeechBubbleRenderer.inRange(1024)&&!JiahaoSpeechBubbleRenderer.inRange(1024.1),"Bubble distance boundary");
    c.getServer().execute(()->{
     var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());var w=c.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
     JiahaoStateManager.setJiahao(p,false);
     for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){w.setBlockState(new BlockPos(x,179,z),Blocks.STONE.getDefaultState());for(int y=180;y<187;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
     p.teleport(w,.5,180,.5,0,0);p.getAbilities().flying=false;p.sendAbilitiesUpdate();p.setHealth(20);
    });stage=1;ticks=0;return;
   }
   if(stage==1){
    if(ticks==10)ClientPlayNetworking.send(JiahaoQuoteRequestPayload.INSTANCE);
    if(ticks==20){check(JiahaoQuoteClientState.local()==null,"Normal V ignored");form(c,true);}
    if(ticks==55){var q=JiahaoQuoteClientState.local();check(q!=null&&q.quote.category()==JiahaoQuoteCategory.TRANSFORM,"Fixed transform received");}
    if(ticks==110)ClientPlayNetworking.send(JiahaoQuoteRequestPayload.INSTANCE);
    if(ticks==118){var q=JiahaoQuoteClientState.local();check(q!=null&&q.quote.category()==JiahaoQuoteCategory.MANUAL,"Manual C2S/S2C");manualId=q.quote.id();manualStart=q.started;for(int i=0;i<30;i++)ClientPlayNetworking.send(JiahaoQuoteRequestPayload.INSTANCE);}
    if(ticks==125)check(JiahaoQuoteClientState.local().started==manualStart,"Spam cannot restart subtitle");
    if(ticks==170)ClientPlayNetworking.send(JiahaoQuoteRequestPayload.INSTANCE);
    if(ticks==178)check(!JiahaoQuoteClientState.local().quote.id().equals(manualId),"Manual does not repeat");
    if(ticks==235)ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE);
    if(ticks==245){var q=JiahaoQuoteClientState.local();check(q!=null&&q.quote.id().equals(JiahaoQuoteRegistry.TIME_STOP_NOTICE)&&q.started!=0,"Immediate fixed time notice in cinematic");}
    if(ticks==405){check(cueSeen&&fadeIn&&fadeOut,"Actual cinematic fade in/out sampled");check(!JiahaoTimeStopClientState.isTimeStopped(c.world),"Time resumes");}
    if(ticks==465){check(JiahaoQuoteClientState.local()==null,"End subtitle expires");remote(c);stage=2;ticks=0;}
   }else if(stage==2){
    check(JiahaoQuoteClientState.local()==null,"Other speaker never creates local subtitle");
    if(ticks==60){check(JiahaoQuoteClientState.active().isEmpty(),"Bubble expires on real clock");JiahaoQuoteClientState.clear();form(c,false);stage=3;ticks=0;}
   }else if(stage==3){
    if(ticks==5){form(c,true);}
    if(ticks==10){form(c,false);}
    if(ticks==40){check(JiahaoQuoteClientState.active().isEmpty(),"Rapid untransform cancels pending quote");remote(c);
     var server=c.getServer();var id=c.player.getUuid();server.execute(()->server.getPlayerManager().getPlayer(id).teleportTo(new net.minecraft.world.TeleportTarget(server.getWorld(net.minecraft.world.World.NETHER),new Vec3d(0,100,0),Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP)));
     stage=4;ticks=0;}
   }else if(stage==4&&ticks>30){check(c.world.getRegistryKey()==net.minecraft.world.World.NETHER,"Actual dimension changed");check(JiahaoQuoteClientState.active().isEmpty(),"Dimension clears old quote state");finish(c,true);}
  }catch(Throwable e){e.printStackTrace();failure=e.toString();finish(c,false);}
 }
 private void remote(MinecraftClient c){
  var actor=new OtherClientPlayerEntity(c.world,new GameProfile(UUID.randomUUID(),"QuoteObserverFixture"));actor.setId(-12345);actor.refreshPositionAndAngles(.5,180,4.5,180,0);c.world.addEntity(actor);
  c.player.setYaw(0);c.player.setPitch(0);
  var q=JiahaoQuoteRegistry.select(JiahaoQuoteCategory.CINEMATIC,null,null,b->0);
  JiahaoQuoteClientState.receive(new JiahaoQuoteSyncPayload(actor.getUuid(),c.world.getRegistryKey().getValue(),q.id(),Long.MAX_VALUE-1,null));
  check(JiahaoQuoteClientState.active().containsKey(actor.getUuid()),"Remote UUID stored");
  var original=JiahaoQuoteClientState.active().get(actor.getUuid());
  JiahaoQuoteClientState.receive(new JiahaoQuoteSyncPayload(actor.getUuid(),c.world.getRegistryKey().getValue(),q.id(),Long.MAX_VALUE-2,null));
  check(JiahaoQuoteClientState.active().get(actor.getUuid())==original,"Old event cannot restart remote text");
  JiahaoQuoteClientState.receive(new JiahaoQuoteSyncPayload(actor.getUuid(),c.world.getRegistryKey().getValue(),net.minecraft.util.Identifier.of("jiahao-mode","unknown"),Long.MAX_VALUE,null));
  check(JiahaoQuoteClientState.active().get(actor.getUuid())==original,"Unknown quote ignored");
  var wrong=c.world.getRegistryKey()==net.minecraft.world.World.NETHER?net.minecraft.world.World.OVERWORLD:net.minecraft.world.World.NETHER;
  JiahaoQuoteClientState.receive(new JiahaoQuoteSyncPayload(actor.getUuid(),wrong.getValue(),q.id(),Long.MAX_VALUE,null));
  check(JiahaoQuoteClientState.active().get(actor.getUuid())==original,"Wrong dimension ignored");
 }
 private void form(MinecraftClient c,boolean value){var server=c.getServer();var id=c.player.getUuid();server.execute(()->{var p=server.getPlayerManager().getPlayer(id);if(value)equipArmor(p);JiahaoStateManager.setJiahao(p,value);});}
 private void shot(MinecraftClient c,String name){if(shots.add(name))pendingShot=name;}
 public static void capture(){if(pendingShot!=null){var c=MinecraftClient.getInstance();net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,pendingShot,c.getFramebuffer(),t->{});pendingShot=null;}}
 private void finish(MinecraftClient c,boolean success){
  if(stage==9)return;stage=9;if(c.getServer()!=null)c.getServer().stop(false);c.disconnect();
  check(JiahaoQuoteClientState.active().isEmpty(),"Disconnect clears state");
  try{Files.writeString(Path.of("quote-smoke-result.txt"),success?"PASSED":"FAILED: "+failure);}catch(Exception e){throw new RuntimeException(e);}c.scheduleStop();
 }
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}

 private static void equipArmor(net.minecraft.server.network.ServerPlayerEntity p) {
  p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_HELMET));
  p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_CHESTPLATE));
  p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_LEGGINGS));
  p.equipStack(net.minecraft.entity.EquipmentSlot.FEET,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_BOOTS));
 }
}
