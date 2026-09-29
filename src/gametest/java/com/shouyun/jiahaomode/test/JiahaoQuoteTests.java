// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.*;
import static com.shouyun.jiahaomode.quote.JiahaoQuoteCategory.*;
public final class JiahaoQuoteTests implements FabricGameTest {
 private static final List<Scenario> RUNNING=new ArrayList<>();
 static {ServerTickEvents.END_SERVER_TICK.register(server->RUNNING.removeIf(s->s.server==server&&s.tick()));}
 @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_quotes",tickLimit=2200)
 public void quoteLifecycle(TestContext context){RUNNING.add(new Scenario(context));}
 private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 private static final class Scenario {
  final TestContext context; final MinecraftServer server;
  final JiahaoTransformationTests.TestPlayerConnection a,b,far,otherDimension;
  final long start; Identifier manual; UUID session;long lowBefore;
  Scenario(TestContext c){
   context=c;server=c.getWorld().getServer();
   a=connect("QuoteActor");b=connect("QuoteObserver");far=connect("QuoteFar");otherDimension=connect("QuoteNether");
   var origin=Vec3d.ofBottomCenter(c.getAbsolutePos(new net.minecraft.util.math.BlockPos(1,3,1)));
   a.player().setPosition(origin);b.player().setPosition(origin.add(64,0,0));far.player().setPosition(origin.add(65,0,0));
   otherDimension.player().teleportTo(new net.minecraft.world.TeleportTarget(server.getWorld(World.NETHER),origin,Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));
   for(var connection:List.of(a,b,far,otherDimension)){connection.player().getAbilities().flying=true;connection.player().setNoGravity(true);}
   check(!JiahaoQuoteManager.manual(a.player()),"Normal player cannot request quote");
   JiahaoStateManager.setJiahao(a.player(),true);JiahaoStateManager.setJiahao(a.player(),false);
   start=JiahaoTimeStopManager.getServerTick(server);
   for(var cat:JiahaoQuoteCategory.values()){
    Identifier previous=null;
    for(int i=0;i<30;i++){var q=JiahaoQuoteRegistry.select(cat,previous,previous,bound->0);check(cat.count==1||!q.id().equals(previous),"No repeated selection "+cat);previous=q.id();}
   }
  }
  JiahaoTransformationTests.TestPlayerConnection connect(String name){
   var connection=JiahaoTransformationTests.connectTestPlayer(server,context.getWorld(),name);
   connection.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER,List.of(JiahaoQuoteSyncPayload.ID.id(),JiahaoTimeStatePayload.ID.id()))));
   return connection;
  }
  List<JiahaoQuoteSyncPayload> quotes(JiahaoTransformationTests.TestPlayerConnection c){return c.channel().outboundMessages().stream().filter(p->p instanceof CustomPayloadS2CPacket x&&x.payload() instanceof JiahaoQuoteSyncPayload).map(p->(JiahaoQuoteSyncPayload)((CustomPayloadS2CPacket)p).payload()).filter(p->p.quote()!=null).toList();}
  long count(JiahaoQuoteCategory category){return quotes(a).stream().filter(p->JiahaoQuoteRegistry.get(p.quote()).category()==category).count();}
  boolean tick(){
   long t=JiahaoTimeStopManager.getServerTick(server)-start;
   try {
    var p=a.player();
    if(t==25){check(quotes(a).isEmpty(),"Canceled transform never fires");JiahaoStateManager.setJiahao(p,true);}
    if(t==44)check(quotes(a).isEmpty(),"Transform waits 20 ticks");
    if(t==46){check(count(TRANSFORM)==1,"One delayed transform");check(quotes(b).size()==1,"64 block recipient included");check(quotes(far).isEmpty()&&quotes(otherDimension).isEmpty(),"Far and other dimension excluded");}
    if(t==105){check(JiahaoQuoteManager.manual(p),"Manual allowed");manual=quotes(a).getLast().quote();for(int i=0;i<100;i++)check(!JiahaoQuoteManager.manual(p),"Spam rejected");JiahaoStateManager.setJiahao(p,false);JiahaoStateManager.setJiahao(p,true);check(!JiahaoQuoteManager.manual(p),"Form toggle does not reset cooldown");}
    if(t==154)check(!JiahaoQuoteManager.manual(p),"Manual 49 ticks blocked");
    if(t==155){check(JiahaoQuoteManager.manual(p),"Manual boundary 50 allowed");check(!manual.equals(quotes(a).getLast().quote()),"Consecutive manual differs");}
    if(t==220)p.setHealth(5);
    if(t==280){check(count(LOW_HEALTH)==1,"Low stage fires once");p.setHealth(10);}
    if(t==282)p.setHealth(5);
    if(t==290){check(count(LOW_HEALTH)==1,"Exactly 50 percent does not rearm");p.setHealth(11);}
    if(t==292)p.setHealth(5);
    if(t==355){check(count(LOW_HEALTH)==2,"Above 50 rearms");p.setHealth(20);session=UUID.randomUUID();JiahaoQuoteManager.timeStarted(p,session,true);}
    if(t==356){check(!JiahaoQuoteManager.manual(p),"Cinematic reservation blocks manual");JiahaoQuoteManager.playbackFailed(p,UUID.randomUUID());}
    if(t==428)check(count(CINEMATIC)==0,"Cue never early");
    if(t==430){check(count(CINEMATIC)==1,"Cue at 74");JiahaoQuoteManager.cinematicStopped(p,session);check(count(TIME_STOP_START)==0,"No duplicate start fallback");JiahaoQuoteManager.timeEnded(p,session,true);}
    if(t==479)check(count(TIME_STOP_END)==0,"End waits for cue plus gap");
    if(t==485)check(count(TIME_STOP_END)==1,"End eventually emitted once");
    if(t==550){session=UUID.randomUUID();JiahaoQuoteManager.timeStarted(p,session,false);check(count(TIME_STOP_START)==1,"No cinematic start fallback");JiahaoQuoteManager.cinematicStopped(p,session);check(count(TIME_STOP_START)==1,"Fallback idempotent");JiahaoQuoteManager.timeEnded(p,session,true);}
    if(t==670){p.setHealth(20);seed(p,true);JiahaoQuoteManager.damage(p,1);check(count(TAKE_DAMAGE)==0,"Small damage excluded");seed(p,false);JiahaoQuoteManager.damage(p,4);check(count(TAKE_DAMAGE)==0,"Damage probability failure");seed(p,true);JiahaoQuoteManager.damage(p,4);check(count(TAKE_DAMAGE)==1,"Damage probability success");}
    if(t==735){seed(p,false);JiahaoQuoteManager.killed(p);check(count(KILL_ENTITY)==0,"Kill probability failure");seed(p,true);JiahaoQuoteManager.killed(p);check(count(KILL_ENTITY)==1,"Kill probability success");}
    if(t==800){JiahaoStateManager.setJiahao(p,false);seed(p,true);JiahaoQuoteManager.killed(p);check(count(KILL_ENTITY)==1,"Normal kill ignored");JiahaoStateManager.setJiahao(p,true);JiahaoQuoteManager.clear(p,false);JiahaoQuoteManager.track(p);}
    if(t==830){
     check(count(TRANSFORM)==1,"Cleared delay cannot leak");
     var source=p.getServerWorld().getDamageSources().playerAttack(p);
     var armor=net.minecraft.entity.EntityType.ARMOR_STAND.create(p.getServerWorld());
     var cow=net.minecraft.entity.EntityType.COW.create(p.getServerWorld());
     var zombie=net.minecraft.entity.EntityType.ZOMBIE.create(p.getServerWorld());
     seed(p,true);net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(armor,source);
     seed(p,true);net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(cow,source);
     check(count(KILL_ENTITY)==1,"Nonhostile living entities excluded by actual event adapter");
     zombie.setPosition(p.getPos());p.getServerWorld().spawnEntity(zombie);seed(p,true);check(zombie.damage(source,1000),"Actual zombie kill succeeds");
     check(count(KILL_ENTITY)==2,"Hostile death event routes to quote manager");
    }
    if(t==895){
     check(JiahaoQuoteManager.manual(p),"Manual before dimension switch");
     p.teleportTo(new net.minecraft.world.TeleportTarget(server.getWorld(World.NETHER),new Vec3d(0,100,0),Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));
     check(!JiahaoQuoteManager.manual(p),"Dimension switch retains manual cooldown");
     check(a.channel().outboundMessages().stream().anyMatch(packet->packet instanceof CustomPayloadS2CPacket cp&&cp.payload() instanceof JiahaoQuoteSyncPayload quote&&quote.quote()==null),"Lifecycle sends cancel packet");
    }
    if(t==925){p.teleportTo(new net.minecraft.world.TeleportTarget(context.getWorld(),new Vec3d(0,100,0),Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));p.setNoGravity(true);p.getAbilities().flying=true;}
    if(t==940){
     p.onTeleportationDone();p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);p.setHealth(20);p.timeUntilRegen=0;
     check(p.damage(p.getServerWorld().getDamageSources().generic(),1),"Actual damage method succeeds");
     check(p.getHealth()==19&&count(TAKE_DAMAGE)==1,"Actual damage mixin excludes small hits");p.changeGameMode(net.minecraft.world.GameMode.CREATIVE);p.setHealth(20);
    }
    if(t==960)JiahaoQuoteManager.activity(p);
    if(t==1559){check(count(IDLE)==0,"Idle cannot fire before 30 seconds");seed(p,true);}
    if(t==1561){check(count(IDLE)==1,"Idle probability success at 30 second boundary");finish();context.runAtTick(context.getTick()+1,context::complete);return true;}
   }catch(Throwable error){finish();context.runAtTick(context.getTick()+1,()->context.throwGameTestException(error.toString()));return true;}
   return false;
  }
  void seed(net.minecraft.server.network.ServerPlayerEntity p,boolean success){for(long seed=0;seed<100000;seed++){p.getRandom().setSeed(seed);float value=p.getRandom().nextFloat();if(success?value<.1:value>.9){p.getRandom().setSeed(seed);return;}}throw new AssertionError("seed");}
  void finish(){for(var c:List.of(a,b,far,otherDimension)){JiahaoQuoteManager.clear(c.player(),true);server.getPlayerManager().remove(c.player());c.channel().finishAndReleaseAll();}}
 }
}
