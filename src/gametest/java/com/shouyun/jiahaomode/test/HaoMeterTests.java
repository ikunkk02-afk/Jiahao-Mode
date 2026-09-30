// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.hao.*;
import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks;
import com.shouyun.jiahaomode.moment.JiahaoMomentManager;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;
import java.util.*;

public final class HaoMeterTests implements FabricGameTest {
    private static final List<Scenario> RUNNING=new ArrayList<>();
    static {
        ServerTickEvents.START_SERVER_TICK.register(server->{for(var s:new ArrayList<>(RUNNING))if(s.actor.player().getServer()==server&&!s.actor.player().isRemoved()){s.actor.player().networkHandler.tick();s.observer.player().networkHandler.tick();}});
        var phase=JiahaoMode.id("hao_assertions");ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE,phase);
        ServerTickEvents.END_SERVER_TICK.register(phase,server->RUNNING.removeIf(s->s.actor.player().getServer()==server&&s.tick()));
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hao_burst",tickLimit=1600)
    public void automaticCombatBurstAndLifecycle(TestContext c){RUNNING.add(new Scenario(c));}
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hao_storage")
    public void ordinaryRespawnKeepsStoredMeter(TestContext c){
        var a=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),"HaoStorage");
        HaoMeterManager.set(a.player(),4300);
        var respawned=a.player().getServer().getPlayerManager().respawnPlayer(a.player(),false,net.minecraft.entity.Entity.RemovalReason.KILLED);
        check(HaoMeterManager.data(respawned).units()==4300,"Ordinary respawn copies meter even when armor/form is lost");
        respawned.getServer().getPlayerManager().remove(respawned);a.channel().finishAndReleaseAll();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hao_storage")
    public void loadedMarkerAndDimensionPersistence(TestContext c){
        var server=c.getWorld().getServer();
        var options=net.minecraft.server.network.ConnectedClientData.createDefault(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HaoLoaded"),false);
        var p=new ServerPlayerEntity(server,c.getWorld(),options.gameProfile(),options.syncedOptions());
        p.setAttached(HaoState.STORAGE,new HaoState(10000,0,true));
        var connection=new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND);
        var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerManager().onPlayerConnect(connection,p,options);
        check(HaoMeterManager.data(p).units()==0&&HaoMeterManager.data(p).gainLock()==100&&!HaoMeterManager.data(p).bursting(),"Login clears unfinished marker loaded before connection");
        p.setAttached(HaoState.STORAGE,new HaoState(4300,60,false));
        p.teleportTo(new net.minecraft.world.TeleportTarget(server.getWorld(net.minecraft.world.World.NETHER),new Vec3d(0,80,0),Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));
        check(HaoMeterManager.data(p).units()==4300&&HaoMeterManager.data(p).gainLock()==60,"Ordinary dimension change preserves stored meter and gain lock");
        server.getPlayerManager().remove(p);channel.finishAndReleaseAll();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hao_rewards")
    public void hostileKillUsesServerDeathEvent(TestContext c){
        var a=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),"HaoKill");var p=a.player();
        JiahaoStateManager.setJiahao(p,true);HaoMeterManager.set(p,0);
        var hostile=EntityType.ZOMBIE.create(c.getWorld());hostile.setAiDisabled(true);hostile.setPosition(Vec3d.ofBottomCenter(c.getAbsolutePos(BlockPos.ORIGIN)));c.getWorld().spawnEntity(hostile);
        hostile.damage(c.getWorld().getDamageSources().playerAttack(p),1000);
        check(HaoMeterManager.data(p).units()==600,"Actual hostile death rewards exactly +6");
        hostile.discard();p.getServer().getPlayerManager().remove(p);a.channel().finishAndReleaseAll();c.complete();
    }
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static List<Object> packets(JiahaoTransformationTests.TestPlayerConnection c){return c.channel().outboundMessages().stream().filter(p->p instanceof CustomPayloadS2CPacket).map(p->(Object)((CustomPayloadS2CPacket)p).payload()).toList();}
    private static final Map<java.util.UUID,Integer> ACKED=new HashMap<>();
    private static void ack(JiahaoTransformationTests.TestPlayerConnection c){c.channel().outboundMessages().stream().filter(p->p instanceof PlayerPositionLookS2CPacket).map(p->(PlayerPositionLookS2CPacket)p).reduce((a,b)->b).ifPresent(p->{if(!java.util.Objects.equals(ACKED.put(c.player().getUuid(),p.getTeleportId()),p.getTeleportId()))c.player().networkHandler.onTeleportConfirm(new TeleportConfirmC2SPacket(p.getTeleportId()));});}
    private static final class Scenario {
        final TestContext c;final JiahaoTransformationTests.TestPlayerConnection actor,observer;JiahaoTransformationTests.TestPlayerConnection late;
        final List<ChunkPos> forced=new ArrayList<>();final ZombieEntity enemy;final long first;long started,frozenTime,enemyAge,observerAge;UUID id;int stage;long mark;
        Scenario(TestContext c){
            this.c=c;JiahaoTimeStopManager.stopTimeStop(c.getWorld());actor=connect("HaoActor");observer=connect("HaoObserver");
            var base=c.getAbsolutePos(new BlockPos(1,2,1));var origin=Vec3d.ofBottomCenter(base.up());
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){c.getWorld().setBlockState(base.add(x,0,z),Blocks.STONE.getDefaultState());for(int y=1;y<=5;y++)c.getWorld().setBlockState(base.add(x,y,z),Blocks.AIR.getDefaultState());}
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var pos=new ChunkPos(base.add(x*16,0,z*16));if(!c.getWorld().getForcedChunks().contains(pos.toLong())){c.getWorld().setChunkForced(pos.x,pos.z,true);forced.add(pos);}}
            var p=actor.player();ack(actor);p.setPosition(origin);p.networkHandler.syncWithPlayerPosition();p.setOnGround(true);p.setVelocity(Vec3d.ZERO);p.setHealth(4);
            observer.player().setPosition(origin.add(2,0,0));observer.player().setNoGravity(true);
            JiahaoStateManager.setJiahao(p,true);HaoMeterManager.set(p,4200);
            var saved=p.writeNbt(new NbtCompound());var restored=HaoState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,saved.getCompound("fabric:attachments").get("jiahao-mode:hao_state")).getOrThrow();
            check(restored.units()==4200,"Persistent NBT round trip");
            JiahaoStateManager.setJiahao(p,false);check(HaoMeterManager.data(p).units()==4200,"Leaving form preserves meter");JiahaoStateManager.setJiahao(p,true);HaoMeterManager.set(p,0);
            p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(0),"jiahao hao set 80");check(HaoMeterManager.data(p).units()==0,"Non-OP command rejected even in creative");
            enemy=EntityType.ZOMBIE.create(c.getWorld());enemy.setPosition(origin.add(3,0,0));enemy.setAiDisabled(true);enemy.setTarget(p);c.getWorld().spawnEntity(enemy);
            JiahaoMomentManager.combat(p);check(HaoMeterManager.safe(p),"Combat, 20% health and targeting hostile do not block burst");
            first=now();
        }
        long now(){return JiahaoTimeStopManager.getServerTick(actor.player().getServer());}
        JiahaoTransformationTests.TestPlayerConnection connect(String name){
            var a=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),name);
            a.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER,List.of(HaoMeterPayload.ID.id(),HaoReadyPayload.ID.id(),HaoBurstPayload.ID.id(),JiahaoTimeStatePayload.ID.id(),JiahaoQuoteSyncPayload.ID.id(),JiahaoMomentProposalPayload.ID.id(),JiahaoMomentStatePayload.ID.id()))));return a;
        }
        void respond(){
            var offer=packets(actor).stream().filter(p->p instanceof HaoReadyPayload).map(p->(HaoReadyPayload)p).reduce((a,b)->b);
            if(offer.isPresent())HaoMeterManager.ready(actor.player(),new HaoReadyPayload(offer.get().token(),offer.get().dimension(),true));
        }
        void begin(){started=now();id=HaoMeterManager.sessionId(actor.player());ack(actor);frozenTime=c.getWorld().getTime();enemyAge=enemy.age;observerAge=observer.player().age;}
        boolean tick(){
            try {
                var p=actor.player();ack(actor);ack(observer);long age=now()-started;
                if(stage==0&&now()-first==20){
                    check(HaoMeterManager.data(p).units()>=100&&HaoMeterManager.data(p).units()<=105,"One second passive gain units="+HaoMeterManager.data(p).units());HaoMeterManager.set(p,9600);
                    check(JiahaoDodgeManager.startDodge(p,0),"Dodge starts");check(!JiahaoDodgeManager.allowDamage(p,c.getWorld().getDamageSources().mobAttack(enemy),1),"Actual perfect dodge verification");
                    check(HaoMeterManager.data(p).units()==10000&&HaoMeterManager.isPending(p),"Perfect dodge caps and queues meter");check(!HaoMeterManager.safe(p),"Active dodge temporarily blocks burst");stage=1;mark=now();
                }else if(stage==1){
                    respond();if(HaoMeterManager.isBursting(p)){check(now()-mark<25,"No combat grace delay");begin();stage=2;}else check(now()-mark<40,"Automatic proposal after dodge ends safe="+HaoMeterManager.safe(p)+" ground="+p.isOnGround()+" vel="+p.getVelocity()+" dodging="+JiahaoDodgeManager.isDodging(p)+" teleport="+((com.shouyun.jiahaomode.dodge.JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport()+" readyChannel="+net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,HaoReadyPayload.ID));
                }else if(stage==2&&age<240){
                    check(JiahaoTimeStopManager.isTimeStopped(c.getWorld())&&JiahaoTimeStopManager.getReason(c.getWorld())==TimeStopReason.HAO_BURST,"Existing freeze is used with burst reason");
                    check(c.getWorld().getTime()==frozenTime&&enemy.age==enemyAge&&observer.player().age==observerAge,"World, hostile and observer stay frozen");
                    check(JiahaoCinematicLocks.isLocked(p)&&!JiahaoDodgeManager.canDodge(p),"Full-session authority input lock");
                    if(age==25){JiahaoTimeNetworking.handleToggle(p);check(HaoMeterManager.isBursting(p),"R cannot cancel burst");HaoMeterManager.gain(p,12);check(HaoMeterManager.data(p).units()==10000,"No recursive gain during burst");}
                    if(age==40){late=connect("HaoLate");check(packets(late).stream().anyMatch(x->x instanceof HaoBurstPayload b&&b.session().equals(id)&&b.elapsed()==40),"Late observer receives current sequence and tick");}
                }else if(stage==2){
                    check(age==240&&!HaoMeterManager.isBursting(p)&&!JiahaoTimeStopManager.isTimeStopped(c.getWorld()),"Ends exactly at 12 seconds");
                    check(HaoMeterManager.data(p).units()==0&&!JiahaoCinematicLocks.isLocked(p),"Meter and control restored");
                    var states=packets(observer).stream().filter(x->x instanceof HaoBurstPayload).map(x->(HaoBurstPayload)x).toList();var poses=states.getFirst().poses();
                    check(poses.size()>=3&&poses.size()<=5&&new HashSet<>(poses).size()==poses.size(),"Server chooses unique poses");
                    check(!states.getLast().active()&&states.stream().allMatch(x->x.poses().equals(poses)),"Observer sees identical sequence and end");
                    var quotes=packets(actor).stream().filter(x->x instanceof JiahaoQuoteSyncPayload q&&q.quote()!=null&&JiahaoQuoteRegistry.get(q.quote()).category()==JiahaoQuoteCategory.HAO_BURST).count();
                    check(quotes>=2&&quotes<=4,"Two to four burst quotes");stage=3;
                }else if(stage==3&&age<340){check(HaoMeterManager.data(p).units()==0,"Five-second gain lock");}
                else if(stage==3){
                    check(HaoMeterManager.data(p).units()==5,"Growth resumes after five seconds");stage=31;
                }else if(stage==31&&age>=400){
                    p.setHealth(20);enemy.setTarget(null);
                    p.currentScreenHandler=new net.minecraft.screen.HopperScreenHandler(1,p.getInventory());check(!HaoMeterManager.safe(p),"Container postpones burst");p.currentScreenHandler=p.playerScreenHandler;
                    JiahaoMomentManager.preference(p,true);JiahaoMomentTests.nextTickWindow(p);stage=32;mark=now();
                }else if(stage==32){
                    var offer=packets(actor).stream().filter(x->x instanceof JiahaoMomentProposalPayload).map(x->(JiahaoMomentProposalPayload)x).reduce((a,b)->b);
                    if(offer.isPresent()){
                        int before=HaoMeterManager.data(p).units();JiahaoMomentManager.respond(p,new JiahaoMomentResponsePayload(offer.get().session(),true));
                        if(JiahaoMomentManager.isLocked(p)){check(HaoMeterManager.data(p).units()==before+400,"Confirmed moment rewards +4");ack(actor);HaoMeterManager.set(p,10000);stage=33;mark=now();}
                    }
                    check(now()-mark<40,"Random fixture starts");
                }else if(stage==33&&now()-mark<60){check(HaoMeterManager.isPending(p)&&!HaoMeterManager.isBursting(p),"MAX waits for existing random cinematic");}
                else if(stage==33){respond();if(HaoMeterManager.isBursting(p)){HaoMeterManager.abort(p);JiahaoMomentManager.preference(p,false);stage=34;mark=now();}else check(now()-mark<90,"Burst follows random moment");}
                else if(stage==34&&now()-mark>=160){
                    int before=HaoMeterManager.data(p).units();check(JiahaoTimeStopManager.startTimeStop(p),"Manual stop starts after existing cooldown");check(HaoMeterManager.data(p).units()==before+800,"Manual start rewards +8");ack(actor);HaoMeterManager.set(p,10000);stage=4;mark=now();
                }else if(stage==4&&now()-mark<300){check(HaoMeterManager.isPending(p)&&!HaoMeterManager.isBursting(p),"Full meter waits for entire manual stop");}
                else if(stage==4){respond();if(HaoMeterManager.isBursting(p)){check(now()-mark<325,"Burst bypasses manual cooldown");begin();stage=5;}else check(now()-mark<340,"Pending manual burst eventually starts");}
                else if(stage==5&&age==25){
                    var b=observer.player();b.setNoGravity(false);b.setOnGround(true);JiahaoStateManager.setJiahao(b,true);HaoMeterManager.set(b,10000);
                    check(HaoMeterManager.isPending(b)&&!HaoMeterManager.isBursting(b),"Second MAX player waits behind current dimension owner");
                }else if(stage==5&&age==30){
                    p.setHealth(0);p.onDeath(c.getWorld().getDamageSources().generic());check(!HaoMeterManager.isBursting(p)&&!JiahaoTimeStopManager.isTimeStopped(c.getWorld())&&!JiahaoCinematicLocks.isLocked(p),"Death clears authority session and freeze");
                    check(HaoMeterManager.data(p).units()==0,"Interrupted burst resets meter");stage=6;mark=now();
                }else if(stage==6){
                    var b=observer.player();var offer=packets(observer).stream().filter(x->x instanceof HaoReadyPayload).map(x->(HaoReadyPayload)x).reduce((a,d)->d);
                    if(offer.isPresent())HaoMeterManager.ready(b,new HaoReadyPayload(offer.get().token(),offer.get().dimension(),true));
                    if(HaoMeterManager.isBursting(b)){
                        var owner=HaoMeterManager.sessionId(b);HaoMeterManager.ready(b,new HaoReadyPayload(UUID.randomUUID(),b.getWorld().getRegistryKey().getValue(),false));check(HaoMeterManager.isBursting(b),"Forged playback cancellation rejected");
                        HaoMeterManager.ready(b,new HaoReadyPayload(owner,b.getWorld().getRegistryKey().getValue(),false));check(!HaoMeterManager.isBursting(b)&&HaoMeterManager.data(b).units()==0&&!JiahaoTimeStopManager.isTimeStopped(c.getWorld()),"Matching playback failure cleans up queued burst");
                        finish();c.runAtTick(c.getTick()+1,c::complete);return true;
                    }check(now()-mark<60,"Queued second player starts after owner death safe="+HaoMeterManager.safe(b)+" ground="+b.isOnGround()+" vel="+b.getVelocity()+" teleport="+((com.shouyun.jiahaomode.dodge.JiahaoDodgeNetworkAccess)b.networkHandler).jiahao$hasPendingTeleport());
                }
            }catch(Throwable e){finish();c.runAtTick(c.getTick()+1,()->c.throwGameTestException(e.toString()+" stage="+stage));return true;}
            return false;
        }
        void finish(){JiahaoTimeStopManager.stopTimeStop(c.getWorld());enemy.discard();for(var a:Arrays.asList(actor,observer,late))if(a!=null){a.player().getServer().getPlayerManager().remove(a.player());a.channel().finishAndReleaseAll();}for(var p:forced)c.getWorld().setChunkForced(p.x,p.z,false);}
    }
}
