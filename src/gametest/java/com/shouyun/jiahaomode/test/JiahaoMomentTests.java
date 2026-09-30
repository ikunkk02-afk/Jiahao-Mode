// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks;
import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import com.shouyun.jiahaomode.moment.JiahaoMomentManager;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import java.util.*;

public final class JiahaoMomentTests implements FabricGameTest {
    private static final List<Scenario> RUNNING=new ArrayList<>();
    static {
        var phase=com.shouyun.jiahaomode.JiahaoMode.id("moment_assertions");
        ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE,phase);
        ServerTickEvents.END_SERVER_TICK.register(phase,server->RUNNING.removeIf(s->s.actor.player().getServer()==server&&s.tick()));
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_moments",tickLimit=4000)
    public void schedulingSafetyAndLifecycle(TestContext c){RUNNING.add(new Scenario(c));}
    /** Test-only clock fixture: shortens additional windows without adding a production trigger API. */
    public static void nextTickWindow(ServerPlayerEntity p) {
        try {
            var method=JiahaoMomentManager.class.getDeclaredMethod("state",ServerPlayerEntity.class);method.setAccessible(true);
            Object state=method.invoke(null,p);var field=state.getClass().getDeclaredField("nextAt");field.setAccessible(true);
            field.setLong(state,JiahaoTimeStopManager.getServerTick(p.getServer())+1);
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final class Scenario {
        final TestContext c;
        final JiahaoTransformationTests.TestPlayerConnection actor,observer;
        JiahaoTransformationTests.TestPlayerConnection late;
        final Vec3d origin;
        final List<ChunkPos> forced=new ArrayList<>();
        final long due;
        long started,worldTime,observerAge;
        int stage;
        UUID session;
        Scenario(TestContext c) {
            this.c=c;JiahaoTimeStopManager.stopTimeStop(c.getWorld());
            actor=connect("MomentActor");observer=connect("MomentWitness");
            var base=c.getAbsolutePos(new BlockPos(1,2,1));origin=Vec3d.ofBottomCenter(base.up());
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){c.getWorld().setBlockState(base.add(x,0,z),Blocks.STONE.getDefaultState());for(int y=1;y<6;y++)c.getWorld().setBlockState(base.add(x,y,z),Blocks.AIR.getDefaultState());}
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var pos=new ChunkPos(base.add(x*16,0,z*16));if(!c.getWorld().getForcedChunks().contains(pos.toLong())){c.getWorld().setChunkForced(pos.x,pos.z,true);forced.add(pos);}}
            var p=actor.player();ack(actor);p.setPosition(origin);p.networkHandler.syncWithPlayerPosition();p.setOnGround(true);p.setVelocity(Vec3d.ZERO);
            observer.player().setPosition(origin.add(2,0,0));observer.player().setNoGravity(true);
            JiahaoStateManager.setJiahao(p,true);
            p.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new JiahaoMomentPreferencePayload(true)));
            due=JiahaoMomentManager.nextWindow(p);
            long gap=due-now();check(gap>=900&&gap<=2400,"First window is 45-120 seconds");
            JiahaoMomentManager.respond(p,new JiahaoMomentResponsePayload(UUID.randomUUID(),true));check(!JiahaoMomentManager.isLocked(p),"Forged ready cannot start");
        }
        long now(){return JiahaoTimeStopManager.getServerTick(actor.player().getServer());}
        JiahaoTransformationTests.TestPlayerConnection connect(String name){
            var connection=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),name);
            if(origin!=null)connection.player().setPosition(origin.add(3,0,0));
            connection.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER,
                List.of(JiahaoMomentProposalPayload.ID.id(),JiahaoMomentStatePayload.ID.id(),JiahaoQuoteSyncPayload.ID.id(),JiahaoTimeStatePayload.ID.id()))));return connection;
        }
        List<Object> packets(JiahaoTransformationTests.TestPlayerConnection connection){return connection.channel().outboundMessages().stream().filter(x->x instanceof CustomPayloadS2CPacket).map(x->(Object)((CustomPayloadS2CPacket)x).payload()).toList();}
        JiahaoMomentProposalPayload proposal(){return packets(actor).stream().filter(x->x instanceof JiahaoMomentProposalPayload).map(x->(JiahaoMomentProposalPayload)x).reduce((a,b)->b).orElseThrow(()->new AssertionError("No proposal stage="+stage+" now="+now()+" due="+due+" next="+JiahaoMomentManager.nextWindow(actor.player())+" safe="+JiahaoMomentManager.safe(actor.player())+" ground="+actor.player().isOnGround()+" pos="+actor.player().getPos()+" velocity="+actor.player().getVelocity()+" stable="+JiahaoTimeStopManager.isStableForCinematic(actor.player())+" quote="+JiahaoQuoteManager.canStartMoment(actor.player())));}
        void ready(){var p=actor.player();var offer=proposal();p.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new JiahaoMomentResponsePayload(offer.session(),true)));session=offer.session();ack(actor);check(JiahaoMomentManager.isLocked(p),"Valid ready locks actor");}
        boolean tick(){
            try {
                var p=actor.player();
                // Keep this pre-existing long scheduler fixture focused on random moments. Hao has separate integration coverage.
                if(com.shouyun.jiahaomode.hao.HaoMeterManager.isPending(p))com.shouyun.jiahaomode.hao.HaoMeterManager.set(p,0);
                if(stage==0&&now()<due){check(!JiahaoMomentManager.isLocked(p),"No early trigger");return false;}
                if(stage==0){ready();started=now();worldTime=c.getWorld().getTime();observerAge=observer.player().age;stage=1;return false;}
                long age=now()-started;
                if(stage==1&&age<60) {
                    check(JiahaoCinematicLocks.isLocked(p)&&!JiahaoTimeStopManager.isTimeStopped(c.getWorld()),"Only actor is locked");
                    p.move(MovementType.SELF,new Vec3d(.1,0,0));check(p.getPos().equals(origin),"Actor travel is held");
                    check(!JiahaoDodgeManager.canDodge(p),"Dodge cannot start during moment");
                    if(age==27)check(packets(observer).stream().anyMatch(x->x instanceof JiahaoQuoteSyncPayload q&&q.quote()!=null&&JiahaoQuoteRegistry.get(q.quote()).category()==JiahaoQuoteCategory.RANDOM_MOMENT),"Witness receives moment quote at cue");
                    if(age==40){late=connect("MomentLate");check(packets(late).stream().anyMatch(x->x instanceof JiahaoMomentStatePayload s&&s.session().equals(session)&&s.elapsed()==40),"Late join gets current session progress");}
                    return false;
                }
                if(stage==1) {
                    check(age==60&&!JiahaoMomentManager.isLocked(p),"Moment ends at 60 ticks");
                    check(c.getWorld().getTime()>worldTime&&observer.player().age>observerAge,"World and observer continue");
                    var states=packets(observer).stream().filter(x->x instanceof JiahaoMomentStatePayload).map(x->(JiahaoMomentStatePayload)x).toList();
                    check(!states.getLast().active()&&states.stream().allMatch(x->x.session().equals(session)&&x.pose()==states.getFirst().pose()),"One pose and shared end");
                    check(JiahaoMomentManager.nextWindow(p)-now()>=900,"Fresh full interval after finish");
                    JiahaoMomentManager.combat(p);nextTickWindow(p);started=now();stage=2;return false;
                }
                if(stage==2&&age==1){check(!JiahaoMomentManager.isLocked(p)&&JiahaoMomentManager.nextWindow(p)-now()>=900,"Combat cancels window and reschedules");}
                if(stage==2&&age<201)return false;
                if(stage==2) {
                    p.currentScreenHandler=new HopperScreenHandler(1,p.getInventory());check(!JiahaoMomentManager.safe(p),"Server GUI blocks trigger");p.currentScreenHandler=p.playerScreenHandler;
                    p.setHealth(5);check(!JiahaoMomentManager.safe(p),"Low health blocks trigger");p.setHealth(20);
                    var zombie=net.minecraft.entity.EntityType.ZOMBIE.create(c.getWorld());zombie.setPosition(origin.add(3,0,0));zombie.setTarget(p);c.getWorld().spawnEntity(zombie);
                    check(!JiahaoMomentManager.safe(p),"Threat targeting player blocks trigger");zombie.discard();
                    JiahaoMomentManager.preference(p,false);nextTickWindow(p);stage=3;return false;
                }
                if(stage==3){check(JiahaoMomentManager.nextWindow(p)==-1&&!JiahaoMomentManager.isLocked(p),"Personal disable suppresses schedule");JiahaoMomentManager.preference(p,true);nextTickWindow(p);stage=4;return false;}
                if(stage==4){ready();p.damage(c.getWorld().getDamageSources().generic(),1);check(!JiahaoMomentManager.isLocked(p),"Damage callback releases lock even before travel");nextTickWindow(p);started=now();stage=5;return false;}
                if(stage==5&&age<201)return false;
                if(stage==5){nextTickWindow(p);stage=6;return false;}
                if(stage==6){ready();check(JiahaoTimeStopManager.startTimeStop(p),"Time stop preempts moment");check(!JiahaoMomentManager.isLocked(p)&&JiahaoTimeStopManager.isCinematicLocked(p),"Priority replaces moment lock");JiahaoTimeStopManager.stopTimeStop(p);ack(actor);started=now();stage=7;return false;}
                if(stage==7&&age<201)return false;
                if(stage==7){nextTickWindow(p);stage=8;return false;}
                if(stage==8){ready();p.setPosition(origin.add(1,0,0));stage=9;return false;}
                if(stage==9){check(!JiahaoMomentManager.isLocked(p),"External teleport cancels moment");p.setPosition(origin);p.networkHandler.syncWithPlayerPosition();p.setOnGround(true);nextTickWindow(p);stage=10;return false;}
                if(stage==10){ready();JiahaoStateManager.setJiahao(p,false);check(!JiahaoMomentManager.isLocked(p)&&JiahaoMomentManager.nextWindow(p)==-1,"Leaving form immediately clears lock and schedule");JiahaoStateManager.setJiahao(p,true);started=now();stage=11;return false;}
                if(stage==11&&age<80)return false;
                if(stage==11){nextTickWindow(p);stage=12;return false;}
                if(stage==12){session=proposal().session();JiahaoMomentManager.respond(p,new JiahaoMomentResponsePayload(UUID.randomUUID(),true));check(!JiahaoMomentManager.isLocked(p),"Wrong session never acquires lock");started=now();stage=13;return false;}
                if(stage==13&&age<21)return false;
                if(stage==13){JiahaoMomentManager.respond(p,new JiahaoMomentResponsePayload(session,true));check(!JiahaoMomentManager.isLocked(p),"Expired response never starts a moment");nextTickWindow(p);stage=14;return false;}
                if(stage==14){ready();p.setHealth(0);p.onDeath(p.getServerWorld().getDamageSources().generic());check(!JiahaoMomentManager.isLocked(p),"Death releases moment immediately");finish();c.runAtTick(c.getTick()+1,c::complete);return true;}
            }catch(Throwable error){finish();c.runAtTick(c.getTick()+1,()->c.throwGameTestException(error.toString()));return true;}
            return false;
        }
        void finish(){JiahaoTimeStopManager.stopTimeStop(c.getWorld());for(var connection:Arrays.asList(actor,observer,late))if(connection!=null){JiahaoMomentManager.clear(connection.player(),true);connection.player().getServer().getPlayerManager().remove(connection.player());connection.channel().finishAndReleaseAll();}for(var pos:forced)c.getWorld().setChunkForced(pos.x,pos.z,false);}
        void ack(JiahaoTransformationTests.TestPlayerConnection connection){connection.channel().outboundMessages().stream().filter(x->x instanceof PlayerPositionLookS2CPacket).map(x->(PlayerPositionLookS2CPacket)x).reduce((a,b)->b).ifPresent(x->connection.player().networkHandler.onTeleportConfirm(new TeleportConfirmC2SPacket(x.getTeleportId())));}
    }
}
