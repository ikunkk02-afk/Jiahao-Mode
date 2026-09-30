// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.hao;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks;
import com.shouyun.jiahaomode.dodge.*;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/** All authority lives on the server thread; world freezes never pause this clock. */
public final class HaoMeterManager {
    private static final Map<MinecraftServer,Runtime> SERVERS=new WeakHashMap<>();
    private static final class Runtime { long sequence; final Map<UUID,State> players=new HashMap<>(); }
    private static final class State { long pendingOrder=-1,retryAt,proposalUntil; UUID proposal; Session active; }
    private static final class Session {
        final UUID id;final Identifier dimension;final long start;final Vec3d origin;final float yaw;
        final List<com.shouyun.jiahaomode.cinematic.JiahaoPoseType> poses;
        final List<Integer> quotes;final int[] cues;final Set<UUID> recipients=new HashSet<>();int nextCue;
        Session(ServerPlayerEntity p,UUID id,long start) {
            this.id=id;this.start=start;dimension=p.getWorld().getRegistryKey().getValue();origin=p.getPos();yaw=p.getYaw();
            poses=HaoBurstTimeline.poses(bound->p.getRandom().nextInt(bound));
            int count=2+p.getRandom().nextInt(3);var pool=new ArrayList<Integer>();for(int i=1;i<=20;i++)pool.add(i);
            var chosen=new ArrayList<Integer>();for(int i=0;i<count;i++)chosen.add(pool.remove(p.getRandom().nextInt(pool.size())));
            quotes=List.copyOf(chosen);cues=HaoBurstTimeline.quoteTicks(count);
        }
    }
    private HaoMeterManager() {}
    private static Runtime runtime(MinecraftServer s) { if(!s.isOnThread())throw new IllegalStateException("Hao requires server thread");return SERVERS.computeIfAbsent(s,k->new Runtime()); }
    private static State state(ServerPlayerEntity p) { return runtime(p.getServer()).players.computeIfAbsent(p.getUuid(),k->new State()); }
    private static long now(ServerPlayerEntity p) { return JiahaoTimeStopManager.getServerTick(p.getServer()); }
    public static HaoState data(ServerPlayerEntity p) { return p.getAttachedOrElse(HaoState.STORAGE,HaoState.EMPTY); }
    private static void store(ServerPlayerEntity p,HaoState d) { runtime(p.getServer());p.setAttached(HaoState.STORAGE,d); }
    public static boolean isBursting(ServerPlayerEntity p) { var r=SERVERS.get(p.getServer());var s=r==null?null:r.players.get(p.getUuid());return s!=null&&s.active!=null; }
    public static boolean isPending(ServerPlayerEntity p) { return data(p).units()>=HaoState.MAX&&!isBursting(p); }
    public static UUID sessionId(ServerPlayerEntity p) { return state(p).active==null?null:state(p).active.id; }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(HaoMeterManager::tick);
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{if(data(h.player).bursting())store(h.player,new HaoState(0,100,false));state(h.player);sync(h.player);syncObserver(h.player);});
        S2CPlayChannelEvents.REGISTER.register((h,s,server,channels)->{if(channels.contains(HaoMeterPayload.ID.id()))sync(h.player);if(channels.contains(HaoBurstPayload.ID.id()))syncObserver(h.player);});
        EntityTrackingEvents.START_TRACKING.register((entity,observer)->{if(entity instanceof ServerPlayerEntity actor&&isBursting(actor))sendTo(actor,state(actor).active,observer);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,server)->{abort(h.player);runtime(server).players.remove(h.player.getUuid());});
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p,a,b)->{abort(p);sync(p);syncObserver(p);});
        ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{
            if(e instanceof ServerPlayerEntity p)abort(p);
            if(e instanceof Monster&&source.getAttacker() instanceof ServerPlayerEntity p)gain(p,6);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->{runtime(p.getServer()).players.remove(old.getUuid());state(p);sync(p);});
        ServerWorldEvents.UNLOAD.register((server,w)->{for(var p:server.getPlayerManager().getPlayerList()){var s=state(p);if(s.active!=null&&s.active.dimension.equals(w.getRegistryKey().getValue()))abort(p);}});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var p:server.getPlayerManager().getPlayerList())abort(p);});
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->dispatcher.register(literal("jiahao").then(literal("hao").requires(s->s.hasPermissionLevel(2))
            .then(literal("set").then(argument("value",IntegerArgumentType.integer(0,100)).executes(c->{set(c.getSource().getPlayerOrThrow(),IntegerArgumentType.getInteger(c,"value")*100);return 1;})))
            .then(literal("add").then(argument("value",IntegerArgumentType.integer(0,100)).executes(c->{var p=c.getSource().getPlayerOrThrow();set(p,data(p).units()+IntegerArgumentType.getInteger(c,"value")*100);return 1;})))
            .then(literal("burst").executes(c->{set(c.getSource().getPlayerOrThrow(),HaoState.MAX);return 1;})))));
    }
    public static void set(ServerPlayerEntity p,int units) {
        if(isBursting(p))return;
        store(p,new HaoState(units,0,false));updatePending(p);sync(p);
    }
    public static void gain(ServerPlayerEntity p,int points) {
        var d=data(p);if(points<=0||!p.isAlive()||!JiahaoStateManager.isJiahao(p)||d.bursting()||d.gainLock()>0)return;
        store(p,new HaoState((int)Math.min(HaoState.MAX,d.units()+(long)points*100),0,false));updatePending(p);sync(p);
    }
    private static void updatePending(ServerPlayerEntity p) {
        var s=state(p);if(isPending(p)){if(s.pendingOrder<0)s.pendingOrder=runtime(p.getServer()).sequence++;}
        else {s.pendingOrder=-1;s.proposal=null;}
    }
    /** Deliberately excludes combat grace, player health and nearby enemies. */
    public static boolean safe(ServerPlayerEntity p) {
        return p.isAlive()&&JiahaoStateManager.isJiahao(p)&&JiahaoTimeStopManager.isStableForCinematic(p)
            &&!p.isSleeping()&&!p.isClimbing()&&!p.isUsingItem()&&!JiahaoDodgeManager.isDodging(p)
            &&!JiahaoTimeStopManager.isTimeStopped(p.getWorld())&&!JiahaoCinematicLocks.isLocked(p)
            &&p.currentScreenHandler==p.playerScreenHandler&&!((JiahaoDodgeInteractionAccess)p.interactionManager).jiahao$isMining()
            &&!((JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport();
    }
    public static void ready(ServerPlayerEntity p,HaoReadyPayload reply) {
        var s=state(p);
        if(s.active!=null&&!reply.ready()&&s.active.id.equals(reply.token())&&s.active.dimension.equals(reply.dimension())){abort(p);return;}
        if(s.proposal==null||!s.proposal.equals(reply.token()))return;
        s.proposal=null;s.retryAt=now(p)+10;
        if(!reply.ready()||now(p)>=s.proposalUntil||!reply.dimension().equals(p.getWorld().getRegistryKey().getValue())||!isPending(p)||!safe(p))return;
        if(!ServerPlayNetworking.canSend(p,HaoBurstPayload.ID))return;
        if(!JiahaoTimeStopManager.startHaoBurst(p))return;
        s.active=new Session(p,JiahaoTimeStopManager.getSession(p.getServerWorld()),now(p));
        store(p,new HaoState(HaoState.MAX,0,true));s.pendingOrder=-1;
        JiahaoQuoteManager.beginHaoBurst(p);send(p,s.active,true);sync(p);
        p.sendMessage(Text.translatable("message.jiahao-mode.hao.full"),true);
    }
    public static void abort(ServerPlayerEntity p) {
        var s=state(p);s.proposal=null;
        if(s.active!=null) {JiahaoTimeStopManager.stopTimeStop(p);if(s.active!=null)ended(p);}
    }
    /** Called by the existing time-stop release path for every normal/abnormal end. */
    public static void ended(ServerPlayerEntity p) {
        var s=state(p);var a=s.active;if(a==null)return;s.active=null;s.proposal=null;s.pendingOrder=-1;
        store(p,new HaoState(0,100,false));send(p,a,false);JiahaoQuoteManager.cancelSession(p,a.id);sync(p);
    }
    public static void sync(ServerPlayerEntity p) {
        if(ServerPlayNetworking.canSend(p,HaoMeterPayload.ID))ServerPlayNetworking.send(p,new HaoMeterPayload(data(p).units(),isPending(p),isBursting(p)));
    }
    private static HaoBurstPayload payload(ServerPlayerEntity p,Session a,boolean active) { return new HaoBurstPayload(p.getUuid(),a.dimension,a.id,active,(int)Math.max(0,now(p)-a.start),a.poses,a.origin,a.yaw); }
    private static void sendTo(ServerPlayerEntity actor,Session a,ServerPlayerEntity observer) {
        if(ServerPlayNetworking.canSend(observer,HaoBurstPayload.ID)){ServerPlayNetworking.send(observer,payload(actor,a,true));a.recipients.add(observer.getUuid());}
    }
    private static void send(ServerPlayerEntity p,Session a,boolean active) {
        if(active)for(var observer:p.getServerWorld().getPlayers())sendTo(p,a,observer);
        else for(var id:a.recipients){var observer=p.getServer().getPlayerManager().getPlayer(id);if(observer!=null&&ServerPlayNetworking.canSend(observer,HaoBurstPayload.ID))ServerPlayNetworking.send(observer,payload(p,a,false));}
    }
    private static void syncObserver(ServerPlayerEntity observer) { for(var actor:observer.getServerWorld().getPlayers()){var s=state(actor);if(s.active!=null)sendTo(actor,s.active,observer);} }
    private static void tick(MinecraftServer server) {
        var players=new ArrayList<>(server.getPlayerManager().getPlayerList());
        for(var p:players) {
            var s=state(p);var d=data(p);long tick=now(p);
            if(s.active!=null) {
                var a=s.active;int age=(int)(tick-a.start);
                if(!p.isAlive()||!JiahaoStateManager.isJiahao(p)||!a.dimension.equals(p.getWorld().getRegistryKey().getValue())||!JiahaoTimeStopManager.isOwner(p)
                    ||!JiahaoTimeStopManager.isStableForCinematic(p)||p.getPos().squaredDistanceTo(a.origin)>.0025) {abort(p);continue;}
                if(age==10)p.sendMessage(Text.translatable("message.jiahao-mode.hao.uncontrollable"),true);
                while(a.nextCue<a.cues.length&&age>=a.cues[a.nextCue])JiahaoQuoteManager.haoBurstQuote(p,a.id,a.quotes.get(a.nextCue++));
                if(age%10==0)send(p,a,true);
            } else {
                if(d.gainLock()>0)store(p,new HaoState(d.units(),d.gainLock()-1,false));
                else if(p.isAlive()&&JiahaoStateManager.isJiahao(p)&&d.units()<HaoState.MAX)store(p,new HaoState(d.units()+5,0,false));
                updatePending(p);
                if(s.proposal!=null&&tick>=s.proposalUntil)s.proposal=null;
            }
            if(tick%20==0||d.units()<HaoState.MAX&&data(p).units()==HaoState.MAX)sync(p);
        }
        players.sort(Comparator.comparingLong(p->state(p).pendingOrder<0?Long.MAX_VALUE:state(p).pendingOrder));
        var proposed=new HashSet<Identifier>();
        for(var p:players) {
            var s=state(p);var dimension=p.getWorld().getRegistryKey().getValue();
            if(!isPending(p)||!safe(p)||now(p)<s.retryAt||!ServerPlayNetworking.canSend(p,HaoReadyPayload.ID)||!ServerPlayNetworking.canSend(p,HaoBurstPayload.ID))continue;
            if(!proposed.add(dimension))continue;
            if(s.proposal==null){s.proposal=UUID.randomUUID();s.proposalUntil=now(p)+20;ServerPlayNetworking.send(p,new HaoReadyPayload(s.proposal,dimension,false));}
        }
    }
}
