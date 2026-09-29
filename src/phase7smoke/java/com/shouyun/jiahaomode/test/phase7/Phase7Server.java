package com.shouyun.jiahaomode.test.phase7;
import com.shouyun.jiahaomode.item.ModItems;import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.moment.JiahaoMomentManager;import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.api.ModInitializer;import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.Blocks;import net.minecraft.entity.EquipmentSlot;import net.minecraft.item.*;import net.minecraft.server.MinecraftServer;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.util.Hand;import net.minecraft.util.math.BlockPos;import net.minecraft.world.*;
import java.nio.file.*;import java.util.*;

/** Opt-in real dedicated server fixture. Reflection changes only later test deadlines. */
public final class Phase7Server implements ModInitializer {
 private int ticks,stage,stageAt,joinedAt=-1;private boolean done,armed,seen,interrupted,freeVerified;private long clock=-1,stopStarted=-1;private final Set<UUID> acks=new HashSet<>();
 public void onInitialize(){
  PayloadTypeRegistry.playS2C().register(Phase7Stage.ID,Phase7Stage.CODEC);PayloadTypeRegistry.playC2S().register(Phase7Ack.ID,Phase7Ack.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Phase7Ack.ID,(p,c)->{if(!c.server().isDedicated())return;if(!p.result().equals("PASSED")){finish("FAILED: "+c.player().getName().getString()+" "+p.result());return;}if(p.stage()==stage)acks.add(c.player().getUuid());});
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->{
   if(!server.isDedicated())return;var p=h.player;var w=p.getServerWorld();
   w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);w.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true,server);w.setTimeOfDay(6000);w.setWeather(0,12000,true,false);
   for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){w.setBlockState(new BlockPos(x,179,z),Blocks.STONE.getDefaultState());for(int y=180;y<187;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
   p.changeGameMode(GameMode.SURVIVAL);equip(p);p.getAbilities().flying=false;p.sendAbilitiesUpdate();p.setHealth(20);
   p.teleport(w,.5,180,p.getName().getString().equals("Phase7Actor")?.5:5.5,180,0);
   server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4),"fillbiome -16 176 -16 16 188 16 minecraft:plains");
   for(String id:new String[]{"jiahao_transformer","market_viewer","jiahao_code_editor"}){
    server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4),"give @s jiahao-mode:"+id);
    check(p.getInventory().main.stream().anyMatch(stack->stack.isOf(net.minecraft.registry.Registries.ITEM.get(com.shouyun.jiahaomode.JiahaoMode.id(id)))),"Real /give acquires "+id);
   }
   p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));JiahaoStateManager.setJiahao(p,p.getName().getString().equals("Phase7Actor"));
  });
  ServerTickEvents.END_SERVER_TICK.register(this::tick);
 }
 private void tick(MinecraftServer server){
  if(!server.isDedicated()||done)return;
  try{
   check(++ticks<7500,"Watchdog stage "+stage);var a=server.getPlayerManager().getPlayer("Phase7Actor");var b=server.getPlayerManager().getPlayer("Phase7Observer");if(a==null||b==null)return;
   int elapsed=ticks-stageAt;
   if(stage==0){if(joinedAt<0)joinedAt=ticks;if(ticks-joinedAt>230&&ServerPlayNetworking.canSend(a,Phase7Stage.ID)&&ServerPlayNetworking.canSend(b,Phase7Stage.ID)){long window=JiahaoMomentManager.nextWindow(a)-JiahaoTimeStopManager.getServerTick(server);check(window>0&&window<=2400,"Natural first random window scheduled");System.out.println("PHASE7 NATURAL WINDOW remaining="+window);advance(server,1);}return;}
   if(stage==3&&JiahaoMomentManager.isLocked(a)){if(!seen){seen=true;clock=a.getWorld().getTimeOfDay();}if(elapsed%10==0)System.out.println("PHASE7 NATURAL MOMENT active; world clock="+a.getWorld().getTimeOfDay());}
   if(stage==4){if(elapsed==20)nextTick(a);if(elapsed==40){check(!JiahaoMomentManager.isLocked(a),"Client GUI proposal rejected");check(JiahaoMomentManager.nextWindow(a)-JiahaoTimeStopManager.getServerTick(server)>=880,"Rejected window waits full interval");}if(elapsed==70)check(JiahaoMomentManager.nextWindow(a)==-1,"Personal disabled preference clears schedule");}
   if(stage==5&&!armed&&elapsed>=80){nextTick(a);nextTick(b);armed=true;}
   if(stage==5&&JiahaoMomentManager.isLocked(a)&&JiahaoMomentManager.isLocked(b))seen=true;
   if(stage==6&&!armed&&elapsed>=80){nextTick(a);armed=true;}
   if(stage==6&&JiahaoMomentManager.isLocked(a)&&!interrupted){seen=true;if(clock<0)clock=JiahaoTimeStopManager.getServerTick(server);if(JiahaoTimeStopManager.getServerTick(server)-clock>=24){check(JiahaoTimeStopManager.startTimeStop(a),"Time stop starts");stopStarted=JiahaoTimeStopManager.getServerTick(server);interrupted=true;check(!JiahaoMomentManager.isLocked(a)&&JiahaoTimeStopManager.isTimeStopped(a.getWorld()),"Time stop preempts random");}}
   if(stage==6&&stopStarted>0&&!freeVerified&&JiahaoTimeStopManager.getServerTick(server)-stopStarted>=110){check(JiahaoTimeStopManager.isTimeStopped(a.getWorld())&&com.shouyun.jiahaomode.dodge.JiahaoDodgeManager.canDodge(a),"Owner can dodge after 100 cinematic ticks while world is still frozen");freeVerified=true;}
   if(stage==7&&!armed&&elapsed>=80){nextTick(a);armed=true;}
   if(stage==7&&JiahaoMomentManager.isLocked(a)&&!interrupted){seen=true;if(clock<0)clock=JiahaoTimeStopManager.getServerTick(server);if(JiahaoTimeStopManager.getServerTick(server)-clock>=30){check(a.damage(a.getServerWorld().getDamageSources().generic(),2),"Real hurt accepted");interrupted=true;check(!JiahaoMomentManager.isLocked(a),"Hurt cancels immediately");}}
   if(stage==8&&!armed&&elapsed>=220){nextTick(a);armed=true;}
   if(stage==8&&JiahaoMomentManager.isLocked(a))seen=true;
   if(acks.size()<2)return;
   if(stage==1&&elapsed<90||stage==2&&elapsed<90)return;
   if(stage==3){check(seen&&a.getWorld().getTimeOfDay()-clock>=50,"Random moment never freezes world");}
   if(stage==5)check(seen,"Both players performed simultaneously");
   if(stage==6||stage==7)check(seen&&interrupted,"Interruption executed");
   if(stage==6)check(freeVerified,"Free time stop verified");
   if(stage==8){check(seen&&!JiahaoMomentManager.isLocked(a),"GUI cancels established session");finish("PASSED");advance(server,9);return;}
   if(stage==1){a.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_CODE_EDITOR));b.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_CODE_EDITOR));}
   if(stage==4){JiahaoStateManager.setJiahao(b,true);a.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);b.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);}
   advance(server,stage+1);
  }catch(Throwable e){e.printStackTrace();finish("FAILED: "+e);}
 }
 private void advance(MinecraftServer server,int next){stage=next;stageAt=ticks;armed=seen=interrupted=freeVerified=false;clock=stopStarted=-1;acks.clear();var a=server.getPlayerManager().getPlayer("Phase7Actor");var b=server.getPlayerManager().getPlayer("Phase7Observer");for(var p:server.getPlayerManager().getPlayerList())ServerPlayNetworking.send(p,new Phase7Stage(stage,a.getUuid(),b.getUuid()));System.out.println("PHASE7 SERVER STAGE "+stage);}
 private void finish(String result){done=true;try{Files.writeString(Path.of("phase7-server-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}System.out.println("PHASE7 SERVER "+result);}
 private static void equip(ServerPlayerEntity p){p.equipStack(EquipmentSlot.HEAD,new ItemStack(ModItems.JIAHAO_HELMET));p.equipStack(EquipmentSlot.CHEST,new ItemStack(ModItems.JIAHAO_CHESTPLATE));p.equipStack(EquipmentSlot.LEGS,new ItemStack(ModItems.JIAHAO_LEGGINGS));p.equipStack(EquipmentSlot.FEET,new ItemStack(ModItems.JIAHAO_BOOTS));}
 private static void nextTick(ServerPlayerEntity p)throws Exception{var method=JiahaoMomentManager.class.getDeclaredMethod("state",ServerPlayerEntity.class);method.setAccessible(true);var state=method.invoke(null,p);var next=state.getClass().getDeclaredField("nextAt");next.setAccessible(true);next.setLong(state,JiahaoTimeStopManager.getServerTick(p.getServer())+1);}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
