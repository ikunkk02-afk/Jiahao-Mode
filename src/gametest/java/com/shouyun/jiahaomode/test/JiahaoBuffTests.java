// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.buff.*;
import com.shouyun.jiahaomode.hao.HaoGadgetManager;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.entity.effect.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import java.util.*;
import java.util.function.IntUnaryOperator;
import static com.shouyun.jiahaomode.network.HaoGadgetPayload.*;
import static com.shouyun.jiahaomode.buff.JiahaoBuffManager.Status.*;

public final class JiahaoBuffTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="buff_pools")
    public void weightedBoundariesDurationsAndOverride(TestContext c) {
        for(var source:JiahaoBuffSource.values()) {
            var pool=source==JiahaoBuffSource.MARKET_VIEWER?JiahaoBuffPool.MARKET:JiahaoBuffPool.CODE;
            int start=0;
            for(var entry:pool) {
                for(int roll:new int[]{start,start+entry.weight()-1}) {
                    for(boolean longest:new boolean[]{false,true}) {
                        var values=new ArrayDeque<Integer>();
                        if(source==JiahaoBuffSource.CODE_EDITOR)values.add(5);
                        values.add(roll);values.add(longest?entry.maxSeconds()-entry.minSeconds():0);
                        var reward=JiahaoBuffPool.select(source,b->values.remove());
                        check(reward.effect()==entry.effect()&&reward.amplifier()==0&&!reward.override(),"Pool boundary");
                        check(reward.durationTicks()==20*(longest?entry.maxSeconds():entry.minSeconds()),"Inclusive duration bounds");
                    }
                }
                start+=entry.weight();
            }
            check(start==100,"Pool weights total 100");
        }
        for(int probability:new int[]{0,4})for(int index=0;index<3;index++) {
            var values=new ArrayDeque<>(List.of(probability,index));
            var reward=JiahaoBuffPool.select(JiahaoBuffSource.CODE_EDITOR,b->values.remove());
            check(reward.override()&&reward.amplifier()==1,"Exactly five override slots");
            check(reward.durationTicks()==(index==1?400:300),"Override duration");
            check(reward.effect()==(index==0?StatusEffects.SPEED:index==1?StatusEffects.HASTE:StatusEffects.JUMP_BOOST),"Override never grants strength or resistance II");
        }
        c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="buff_merge")
    public void strongerLongerInfiniteAndUpgrade(TestContext c) {
        var a=connect(c,"BuffMerge");var p=a.player();
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,1200,2));
        check(!JiahaoBuffManager.applyReward(p,new JiahaoBuffPool.Reward(StatusEffects.SPEED,0,400,false)),"Stronger effect retained");
        check(p.getStatusEffect(StatusEffects.SPEED).getDuration()==1200,"Stronger duration retained");
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,1200,0));
        check(!JiahaoBuffManager.applyReward(p,new JiahaoBuffPool.Reward(StatusEffects.HASTE,0,600,false)),"Longer same level retained");
        check(JiahaoBuffManager.applyReward(p,new JiahaoBuffPool.Reward(StatusEffects.HASTE,0,1400,false)),"Same level can extend");
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,-1,0));
        check(!JiahaoBuffManager.applyReward(p,new JiahaoBuffPool.Reward(StatusEffects.NIGHT_VISION,0,900,false)),"Infinite retained");
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,1200,0));
        check(JiahaoBuffManager.applyReward(p,new JiahaoBuffPool.Reward(StatusEffects.JUMP_BOOST,1,300,true)),"Vanilla upgrade applies");
        check(p.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier()==1,"Upgrade level");
        check(p.getStatusEffect(StatusEffects.NIGHT_VISION).isInfinite()&&p.getStatusEffect(StatusEffects.SPEED).getAmplifier()==2,"Unrelated effects untouched");
        p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));JiahaoStateManager.setJiahao(p,true);
        p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        check(JiahaoBuffManager.tryGrantRandomBuff(p,JiahaoBuffSource.MARKET_VIEWER).status()==INVALID,"Spectators cannot claim rewards");
        close(a);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="buff_devices",tickLimit=160)
    public void normalGuiValidOperationsReplaySpamAndIndependentRewards(TestContext c) {
        JiahaoTimeStopManager.stopTimeStop(c.getWorld());var a=connect(c,"BuffDevices");var p=a.player();
        check(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,JiahaoGadgetResultPayload.ID),"Result channel registered");
        p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));
        open(p,false);var session=last(a,Phase.OPEN).session();
        HaoGadgetManager.receive(p,new HaoGadgetPayload(session,NONE,Action.BUY,Phase.BEGIN));
        var op=last(a,Phase.GRANT);
        HaoGadgetManager.receive(p,new HaoGadgetPayload(session,op.operation(),Action.BUY,Phase.COMMIT));
        check(results(a).isEmpty(),"Premature completion rejected");
        c.runAtTick(21,()->{
            HaoGadgetManager.receive(p,new HaoGadgetPayload(session,op.operation(),Action.BUY,Phase.COMMIT));
            check(result(a).status()==NOT_IN_FORM&&p.getStatusEffects().isEmpty(),"Normal GUI completes without buffs");
            JiahaoStateManager.setJiahao(p,true);
            HaoGadgetManager.receive(p,new HaoGadgetPayload(session,NONE,Action.SELL,Phase.BEGIN));
        });
        c.runAtTick(30,()->ping(p,session,Action.SELL));
        c.runAtTick(42,()->{
            var sell=last(a,Phase.GRANT);var commit=new HaoGadgetPayload(session,sell.operation(),Action.SELL,Phase.COMMIT);
            HaoGadgetManager.receive(p,commit);check(result(a).status()==GRANTED,"SELL grants Market buff");
            long count=results(a).size();for(int i=0;i<100;i++)HaoGadgetManager.receive(p,commit);
            check(results(a).size()==count,"100 replayed packets issue no new results");
            for(int i=0;i<100;i++)HaoGadgetManager.receive(p,new HaoGadgetPayload(session,NONE,Action.BUY,Phase.BEGIN));
        });
        c.runAtTick(50,()->ping(p,session,Action.BUY));
        c.runAtTick(63,()->{
            var buy=last(a,Phase.GRANT);HaoGadgetManager.receive(p,new HaoGadgetPayload(session,buy.operation(),Action.BUY,Phase.COMMIT));
            check(result(a).status()==COOLDOWN,"BUY shares SELL Buff cooldown; GUI still completes");
            p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_CODE_EDITOR));open(p,true);
            var code=last(a,Phase.OPEN);HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),NONE,Action.CODE,Phase.BEGIN));
        });
        c.runAtTick(86,()->{
            var code=last(a,Phase.OPEN);var grant=last(a,Phase.GRANT);long count=results(a).size();
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),grant.operation(),Action.CODE,Phase.COMMIT));
            check(results(a).size()==count,"Code must wait 24 server ticks");
        });
        c.runAtTick(88,()->{
            var code=last(a,Phase.OPEN);var grant=last(a,Phase.GRANT);
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),grant.operation(),Action.CODE,Phase.COMMIT));
            check(result(a).status()==GRANTED||result(a).status()==PRESERVED,"Code cooldown independent of Market: "+result(a));
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),NONE,Action.CODE,Phase.BEGIN));
            p.setStackInHand(Hand.OFF_HAND,p.getMainHandStack());p.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        });
        c.runAtTick(114,()->{
            var code=last(a,Phase.OPEN);var grant=last(a,Phase.GRANT);long count=results(a).size();
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),grant.operation(),Action.CODE,Phase.COMMIT));
            check(results(a).size()==count,"Moved original item invalidates session");
            HaoGadgetManager.receive(p,new HaoGadgetPayload(UUID.randomUUID(),UUID.randomUUID(),Action.CODE,Phase.COMMIT));
            check(results(a).size()==count,"Forged session invalid");close(a);c.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="buff_cooldowns",tickLimit=650)
    public void cooldownBoundaryAndReconnect(TestContext c) {
        var a=connect(c,"BuffCooldown");var holder=new java.util.concurrent.atomic.AtomicReference<>(a);
        c.runAtTick(1,()->{
            var p=holder.get().player();p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));JiahaoStateManager.setJiahao(p,true);
            for(var entry:JiahaoBuffPool.MARKET)p.addStatusEffect(new StatusEffectInstance(entry.effect(),-1,2));
            check(JiahaoBuffManager.tryGrantRandomBuff(p,JiahaoBuffSource.MARKET_VIEWER).status()==PRESERVED,"Preserved reward consumes cooldown");
            var profile=p.getGameProfile();var server=p.getServer();close(holder.get());
            var data=net.minecraft.server.network.ConnectedClientData.createDefault(profile,false);
            var next=new net.minecraft.server.network.ServerPlayerEntity(server,c.getWorld(),profile,data.syncedOptions());
            var connection=new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND);
            var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);server.getPlayerManager().onPlayerConnect(connection,next,data);
            JiahaoTransformationTests.equipJiahaoArmor(next);JiahaoStateManager.setJiahao(next,true);next.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));
            holder.set(new JiahaoTransformationTests.TestPlayerConnection(next,channel));
            check(JiahaoBuffManager.tryGrantRandomBuff(next,JiahaoBuffSource.MARKET_VIEWER).status()==COOLDOWN,"Reconnect same UUID retains cooldown");
            var respawn=server.getPlayerManager().respawnPlayer(next,false,net.minecraft.entity.Entity.RemovalReason.KILLED);
            JiahaoTransformationTests.equipJiahaoArmor(respawn);JiahaoStateManager.setJiahao(respawn,true);respawn.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));
            holder.set(new JiahaoTransformationTests.TestPlayerConnection(respawn,channel));respawn.setNoGravity(true);
            check(JiahaoBuffManager.tryGrantRandomBuff(respawn,JiahaoBuffSource.MARKET_VIEWER).status()==COOLDOWN,"Respawn retains cooldown");
            respawn.teleportTo(new net.minecraft.world.TeleportTarget(server.getWorld(net.minecraft.world.World.NETHER),new net.minecraft.util.math.Vec3d(0,80,0),net.minecraft.util.math.Vec3d.ZERO,0,0,net.minecraft.world.TeleportTarget.NO_OP));
            check(JiahaoBuffManager.tryGrantRandomBuff(respawn,JiahaoBuffSource.MARKET_VIEWER).status()==COOLDOWN,"Dimension change retains cooldown");
        });
        c.runAtTick(600,()->check(JiahaoBuffManager.tryGrantRandomBuff(holder.get().player(),JiahaoBuffSource.MARKET_VIEWER).status()==COOLDOWN,"599 ticks still cooling down"));
        c.runAtTick(601,()->{
            var result=JiahaoBuffManager.tryGrantRandomBuff(holder.get().player(),JiahaoBuffSource.MARKET_VIEWER);
            check(result.status()==GRANTED||result.status()==PRESERVED,"600 ticks permits next reward");close(holder.get());c.complete();
        });
    }
    private static JiahaoTransformationTests.TestPlayerConnection connect(TestContext c,String name) {
        var a=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),name);
        a.player().setNoGravity(true);a.player().getAbilities().flying=true;
        a.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER,List.of(HaoGadgetPayload.ID.id(),JiahaoGadgetResultPayload.ID.id()))));
        return a;
    }
    private static void open(net.minecraft.server.network.ServerPlayerEntity p,boolean code){HaoGadgetManager.open(p,code,Hand.MAIN_HAND);}
    private static void ping(net.minecraft.server.network.ServerPlayerEntity p,UUID session,Action action){HaoGadgetManager.receive(p,new HaoGadgetPayload(session,NONE,action,Phase.PING));}
    private static HaoGadgetPayload last(JiahaoTransformationTests.TestPlayerConnection a,Phase phase){a.channel().runPendingTasks();a.channel().flush();return a.channel().outboundMessages().stream().filter(x->x instanceof CustomPayloadS2CPacket p&&p.payload() instanceof HaoGadgetPayload h&&h.phase()==phase).map(x->(HaoGadgetPayload)((CustomPayloadS2CPacket)x).payload()).reduce((x,y)->y).orElseThrow(()->new AssertionError("Missing gadget "+phase+" serverTick="+JiahaoTimeStopManager.getServerTick(a.player().getServer())+" alive="+a.player().isAlive()+" held="+a.player().getMainHandStack()+" frozen="+JiahaoTimeStopManager.shouldFreeze(a.player())));}
    private static List<JiahaoGadgetResultPayload> results(JiahaoTransformationTests.TestPlayerConnection a){a.channel().runPendingTasks();a.channel().flush();return a.channel().outboundMessages().stream().filter(x->x instanceof CustomPayloadS2CPacket p&&p.payload() instanceof JiahaoGadgetResultPayload).map(x->(JiahaoGadgetResultPayload)((CustomPayloadS2CPacket)x).payload()).toList();}
    private static JiahaoBuffManager.Result result(JiahaoTransformationTests.TestPlayerConnection a){var packets=results(a);if(packets.isEmpty())throw new IllegalStateException("Missing buff result packets="+a.channel().outboundMessages().stream().map(x->x.getClass().getSimpleName()).toList()+" tick="+JiahaoTimeStopManager.getServerTick(a.player().getServer())+" held="+a.player().getMainHandStack()+" last="+last(a,Phase.GRANT));return packets.getLast().result();}
    private static void close(JiahaoTransformationTests.TestPlayerConnection a){a.player().getServer().getPlayerManager().remove(a.player());a.channel().finishAndReleaseAll();}
    private static void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
}
