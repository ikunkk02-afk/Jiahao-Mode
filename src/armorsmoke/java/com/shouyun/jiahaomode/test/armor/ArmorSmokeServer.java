// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.armor;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.armor.JiahaoArmorUtil;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import java.nio.file.*;
import java.util.*;

public final class ArmorSmokeServer implements ModInitializer {
 private int ticks,stage,stageAt;
 private boolean done;
 private final Set<UUID> acknowledgments=new HashSet<>();
 private final boolean reload=Boolean.getBoolean("jiahao.armor.reload");
 public void onInitialize() {
  PayloadTypeRegistry.playS2C().register(ArmorStagePayload.ID,ArmorStagePayload.CODEC);
  PayloadTypeRegistry.playC2S().register(ArmorAckPayload.ID,ArmorAckPayload.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(ArmorAckPayload.ID,(payload,context)->{
   if(!context.server().isDedicated())return;
   if(!payload.result().equals("PASSED")){finish("FAILED: "+context.player().getName().getString()+" "+payload.result());return;}
   if(payload.stage()==stage)acknowledgments.add(context.player().getUuid());
  });
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
   if(!server.isDedicated())return;
   var p=handler.player;
   if(!reload) {
    JiahaoStateManager.setJiahao(p,false);equip(p);
    p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_TRANSFORMER));
    p.changeGameMode(GameMode.SURVIVAL);
    boolean actor=p.getGameProfile().getName().equals("ArmorActor");
    var w=p.getServerWorld();w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);
    w.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true,server);w.setTimeOfDay(6000);
    for(int x=-8;x<=8;x++)for(int z=-8;z<=10;z++) {w.setBlockState(new BlockPos(x,180,z),Blocks.OBSIDIAN.getDefaultState());for(int y=181;y<188;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
    p.teleport(w,.5,181,actor?.5:6.5,actor?0:180,0);
   }
  });
  ServerTickEvents.END_SERVER_TICK.register(this::tick);
 }
 private void tick(MinecraftServer server) {
  if(!server.isDedicated()||done)return;
  try {
   ticks++;if(ticks>3600)throw new AssertionError("Server stage watchdog "+stage);
   var a=server.getPlayerManager().getPlayer("ArmorActor");var b=server.getPlayerManager().getPlayer("ArmorObserver");
   if(a==null||b==null)return;
   if(stage==0) {
    if(ticks<100||!ServerPlayNetworking.canSend(a,ArmorStagePayload.ID)||!ServerPlayNetworking.canSend(b,ArmorStagePayload.ID))return;
    advance(server,reload?101:1);return;
   }
   if(acknowledgments.size()<2)return;
   int elapsed=ticks-stageAt;
   if(stage==5&&elapsed<65)return;
   if(stage==8) {
    if(!JiahaoStateManager.isJiahao(a)||!JiahaoArmorUtil.isWearingFullJiahaoArmor(a)||JiahaoTimeStopManager.isTimeStopped(a.getServerWorld()))throw new AssertionError("Saved form must be equipped and time active false");
    finish("PASSED");advance(server,9);return;
   }
   if(stage==103){finish("PASSED");advance(server,104);return;}
   int next=stage+1;
   if(next==6||next==102)a.equipStack(EquipmentSlot.HEAD,ItemStack.EMPTY);
   if(next==7||next==103)equip(a);
   advance(server,next);
  }catch(Throwable e){e.printStackTrace();finish("FAILED: "+e);}
 }
 private void advance(MinecraftServer server,int next) {
  stage=next;stageAt=ticks;acknowledgments.clear();
  for(var p:server.getPlayerManager().getPlayerList())if(ServerPlayNetworking.canSend(p,ArmorStagePayload.ID))ServerPlayNetworking.send(p,new ArmorStagePayload(stage));
  System.out.println("ARMOR MULTIPLAYER STAGE "+stage);
 }
 private void finish(String result) {
  done=true;try{Files.writeString(Path.of("armor-server-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}
  System.out.println("ARMOR MULTIPLAYER SERVER "+result);
 }
 public static void equip(ServerPlayerEntity p) {
  p.equipStack(EquipmentSlot.HEAD,new ItemStack(ModItems.JIAHAO_HELMET));p.equipStack(EquipmentSlot.CHEST,new ItemStack(ModItems.JIAHAO_CHESTPLATE));
  p.equipStack(EquipmentSlot.LEGS,new ItemStack(ModItems.JIAHAO_LEGGINGS));p.equipStack(EquipmentSlot.FEET,new ItemStack(ModItems.JIAHAO_BOOTS));
 }
}
