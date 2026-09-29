// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.armor;
import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.armor.JiahaoArmorUtil;
import com.shouyun.jiahaomode.client.*;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import java.nio.file.*;
import java.util.*;

/** Actual client framebuffers and remote player tracking; no native-window automation. */
public final class ArmorSmokeClient implements ClientModInitializer {
 private int stage,ticks,total;
 private boolean sent,acked,done;
 private long movingTime=-1;
 private static String pendingShot;
 private static final Set<String> CAPTURED=new HashSet<>();
 private static final List<Identifier> QUOTES=new ArrayList<>();
 public void onInitializeClient() {
  ClientPlayNetworking.registerGlobalReceiver(ArmorStagePayload.ID,(p,c)->{stage=p.stage();ticks=0;sent=acked=false;movingTime=-1;});
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
 }
 private void tick(MinecraftClient c) {
  if(done)return;
  try {
   check(++total<3600,"Client watchdog stage "+stage);
   if(c.player==null||c.world==null||c.getOverlay()!=null||stage==0)return;
   c.setScreen(null);c.options.pauseOnLostFocus=false;c.options.language="zh_cn";c.options.getMaxFps().setValue(120);ticks++;
   boolean actor=c.player.getName().getString().equals("ArmorActor");
   var target=actor?c.player:c.world.getPlayers().stream().filter(p->p.getName().getString().equals("ArmorActor")).findFirst().orElse(null);
   if(target==null)return;
   switch(stage) {
    case 1,2 -> {
     check(JiahaoArmorUtil.isWearingFullJiahaoArmor(target)&&!JiahaoStateManager.isJiahao(target),"Remote/local normal full armor");
     if(actor)c.options.setPerspective(stage==1?Perspective.THIRD_PERSON_FRONT:Perspective.THIRD_PERSON_BACK);
     if(ticks==5&&actor)validateTextures(c);
     if(ticks>(stage==1?60:15)){String name=(actor?"actor-":"observer-")+(stage==1?"armor-front.png":"armor-back.png");if(shot(name))ack();}
    }
    case 3,8 -> {
     if(actor&&!sent){sent=true;c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);}
     var q=JiahaoQuoteClientState.active().get(target.getUuid());
     if(q!=null&&q.age()>.25) {
      check(JiahaoStateManager.isJiahao(target)&&q.quote.id().equals(JiahaoQuoteRegistry.TRANSFORM_REVENGE)&&q.started!=0,"Successful transform gives immediate fixed line");
      check(q.text.getString().equals("10年前的仇难道不报了吗"),"Exact Chinese revenge text");
      if(stage==3&&shot(actor?"actor-transform.png":"observer-transform-bubble.png"))ack();else if(stage==8)ack();
     }
    }
    case 4 -> {
     if(actor&&!sent){sent=true;ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE);}
     var q=JiahaoQuoteClientState.active().get(target.getUuid());
     if(q!=null&&q.quote.id().equals(JiahaoQuoteRegistry.TIME_STOP_NOTICE)&&q.age()>.25) {
      check(JiahaoTimeStopClientState.isTimeStopped(c.world)&&q.started!=0&&q.session==null,"Frozen world gets immediate fixed time subtitle");
      check(q.text.getString().equals("注意时间并没有静止"),"Exact Chinese time text");
      if(actor)check(JiahaoCinematicController.locksInput(),"Owner cinematic input is locked while equipped");
      else check(!JiahaoCinematicController.locksInput()&&!JiahaoCinematicController.isCameraActive(),"Observer camera remains their own");
      if(shot(actor?"actor-time-notice.png":"observer-time-bubble.png"))ack();
     }
    }
    case 5 -> {
     check(JiahaoTimeStopClientState.isTimeStopped(c.world),"Time stop remains active through cinematic cue boundary");
     check(QUOTES.stream().noneMatch(id->id.getPath().startsWith("quote/cinematic.")||id.getPath().startsWith("quote/time_stop_start.")||id.getPath().startsWith("quote/transform.")),"No random or delayed cinematic opening lines");
     if(ticks>55)ack();
    }
    case 6,102 -> {
     if(ticks<8)return;
     check(!JiahaoStateManager.isJiahao(target)&&!JiahaoArmorUtil.isWearingFullJiahaoArmor(target),"Armor loss synchronizes effective form false");
     check(!JiahaoTimeStopClientState.isTimeStopped(c.world)&&!JiahaoCinematicController.locksInput()&&!JiahaoCinematicController.isCameraActive(),"World, camera and input unlock");
     check(!JiahaoQuoteClientState.active().containsKey(target.getUuid()),"Subtitle and remote bubble canceled");
     if(movingTime<0)movingTime=c.world.getTimeOfDay();
     else if(c.world.getTimeOfDay()>movingTime&&shot(actor?"actor-restored-control.png":"observer-restored-world.png"))ack();
    }
    case 7,103 -> {
     if(ticks<8)return;
     check(JiahaoArmorUtil.isWearingFullJiahaoArmor(target)&&!JiahaoStateManager.isJiahao(target),"Re-equipping cannot automatically transform");
     if(stage==7&&actor){c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);if(ticks>25&&shot("actor-armor-front-stable.png"))ack();}else ack();
    }
    case 101 -> {
     if(ticks<15)return;
     check(JiahaoArmorUtil.isWearingFullJiahaoArmor(target)&&JiahaoStateManager.isJiahao(target),"Real disk reload preserves equipped form");
     check(!JiahaoTimeStopClientState.isTimeStopped(c.world)&&JiahaoQuoteClientState.active().isEmpty()&&QUOTES.isEmpty(),"Reload does not replay transform or active abilities");ack();
    }
    case 9,104 -> finish(c,"PASSED");
    default -> throw new AssertionError("Unexpected stage "+stage);
   }
  }catch(Throwable e){e.printStackTrace();if(ClientPlayNetworking.canSend(ArmorAckPayload.ID))ClientPlayNetworking.send(new ArmorAckPayload(stage,e.toString()));finish(c,"FAILED: stage "+stage+" "+e);}
 }
 private static void validateTextures(MinecraftClient c) throws Exception {
  for(int layer=1;layer<=2;layer++) {
   var id=JiahaoMode.id("textures/models/armor/jiahao_layer_"+layer+".png");
   try(var stream=c.getResourceManager().getResource(id).orElseThrow().getInputStream();var image=NativeImage.read(stream)) {
    check(image.getWidth()==128&&image.getHeight()==64,"Real armor texture resolution");int clear=0,white=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){int color=image.getColor(x,y);if((color>>>24)==0)clear++;else if((color&255)>170&&((color>>>8)&255)>170)white++;}
    check(clear>1000&&white>1,"Transparent UV margins and white logo pixels");
   }
  }
 }
 private void ack(){if(!acked){acked=true;ClientPlayNetworking.send(new ArmorAckPayload(stage,"PASSED"));System.out.println("ARMOR CLIENT STAGE "+stage+" PASSED");}}
 private static boolean shot(String name){if(CAPTURED.contains(name))return true;if(pendingShot==null)pendingShot=name;return false;}
 public static void observe(JiahaoQuoteSyncPayload packet){if(packet.quote()!=null)QUOTES.add(packet.quote());}
 public static void capture(){if(pendingShot!=null){String name=pendingShot;var c=MinecraftClient.getInstance();net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,name,c.getFramebuffer(),text->{});CAPTURED.add(name);pendingShot=null;}}
 private void finish(MinecraftClient c,String result){done=true;try{Files.writeString(Path.of("armor-client-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}c.disconnect();c.scheduleStop();}
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
