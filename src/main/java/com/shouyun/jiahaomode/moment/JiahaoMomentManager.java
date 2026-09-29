// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.moment;

import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
import com.shouyun.jiahaomode.dodge.*;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Independent server clock, per-player deadlines and transient sessions. Never freezes the world. */
public final class JiahaoMomentManager {
    public static final int MIN_INTERVAL=900, MAX_INTERVAL=2400, DURATION=60, COMBAT_GRACE=200;
    private static final Map<MinecraftServer,Runtime> SERVERS=new WeakHashMap<>();
    private static final class Runtime { final Map<UUID,State> players=new HashMap<>(); }
    private static final class State {
        boolean enabled;
        long nextAt=-1, combatUntil, proposalUntil;
        UUID proposal;
        Session active;
    }
    private static final class Session {
        final UUID id;
        final long start;
        final Identifier dimension;
        final Vec3d origin;
        final float yaw;
        final JiahaoPoseType pose;
        final int arc;
        boolean cue;
        final Set<UUID> recipients=new HashSet<>();
        Session(ServerPlayerEntity p,UUID id,long tick) {
            this.id=id;start=tick;dimension=p.getWorld().getRegistryKey().getValue();origin=p.getPos();yaw=p.getYaw();
            pose=JiahaoPoseType.moment(bound->p.getRandom().nextInt(bound));
            int choice=p.getRandom().nextInt(3);arc=choice==0?90:choice==1?120:180;
        }
    }
    private JiahaoMomentManager() {}
    private static Runtime runtime(MinecraftServer s) {
        if(!s.isOnThread())throw new IllegalStateException("Moment authority requires server thread");
        return SERVERS.computeIfAbsent(s,k->new Runtime());
    }
    private static State state(ServerPlayerEntity p) { return runtime(p.getServer()).players.computeIfAbsent(p.getUuid(),k->new State()); }
    private static long now(ServerPlayerEntity p) {return JiahaoTimeStopManager.getServerTick(p.getServer());}
    private static void schedule(ServerPlayerEntity p,State s) {s.nextAt=now(p)+MIN_INTERVAL+p.getRandom().nextInt(MAX_INTERVAL-MIN_INTERVAL+1);}
    public static long nextWindow(ServerPlayerEntity p) {return state(p).nextAt;}
    public static UUID sessionId(ServerPlayerEntity p) {var a=state(p).active;return a==null?null:a.id;}
    public static boolean isLocked(Entity e) {
        if(e instanceof ServerPlayerEntity p) {
            var r=SERVERS.get(p.getServer());var s=r==null?null:r.players.get(p.getUuid());return s!=null&&s.active!=null;
        }
        return e.getWorld().isClient()&&e.getAttachedOrElse(JiahaoMomentView.LOCKED,false);
    }
    public static void initialize() {
        JiahaoMomentView.initialize();
        ServerTickEvents.END_SERVER_TICK.register(JiahaoMomentManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->clear(h.player,true));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p,a,b)->clear(p,false));
        ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->clear(old,false));
        ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{if(e instanceof ServerPlayerEntity p)clear(p,false);});
        ServerWorldEvents.UNLOAD.register((server,w)->cancelWorld(w));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var p:server.getPlayerManager().getPlayerList())clear(p,false);});
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerPlayConnectionEvents.JOIN.register((h,sender,server)->syncObserver(h.player));
        S2CPlayChannelEvents.REGISTER.register((h,sender,server,channels)->{if(channels.contains(JiahaoMomentStatePayload.ID.id()))syncObserver(h.player);});
        AttackEntityCallback.EVENT.register((p,w,hand,e,hit)->{if(p instanceof ServerPlayerEntity actor)combat(actor);return ActionResult.PASS;});
    }
    public static void preference(ServerPlayerEntity p,boolean enabled) {
        var s=state(p);if(s.enabled==enabled)return;s.enabled=enabled;
        if(!enabled){cancel(p,s);s.nextAt=-1;}else if(JiahaoStateManager.isJiahao(p))schedule(p,s);
    }
    public static void combat(ServerPlayerEntity p) {var s=state(p);s.combatUntil=now(p)+COMBAT_GRACE;cancel(p,s);}
    public static boolean safe(ServerPlayerEntity p) {
        return JiahaoStateManager.isJiahao(p)&&JiahaoTimeStopManager.isStableForCinematic(p)
            &&!p.isSleeping()&&!p.isSwimming()&&!p.isClimbing()&&!p.isUsingItem()&&p.getHealth()>=p.getMaxHealth()*.5f
            &&!JiahaoTimeStopManager.isTimeStopped(p.getWorld())&&!JiahaoDodgeManager.isDodging(p)
            &&p.currentScreenHandler==p.playerScreenHandler
            &&!((JiahaoDodgeInteractionAccess)p.interactionManager).jiahao$isMining()
            &&!((JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport()
            &&now(p)>=state(p).combatUntil
            &&p.getServerWorld().getEntitiesByClass(MobEntity.class,p.getBoundingBox().expand(16),m->m.isAlive()&&m.getTarget()==p).isEmpty();
    }
    /** Called only when the precomputed server deadline arrives. Client cannot choose a deadline. */
    private static void propose(ServerPlayerEntity p,State s) {
        schedule(p,s);
        if(!s.enabled||s.active!=null||!safe(p)||!JiahaoQuoteManager.canStartMoment(p)
            ||!ServerPlayNetworking.canSend(p,JiahaoMomentProposalPayload.ID)||!ServerPlayNetworking.canSend(p,JiahaoMomentStatePayload.ID))return;
        s.proposal=UUID.randomUUID();s.proposalUntil=now(p)+20;
        ServerPlayNetworking.send(p,new JiahaoMomentProposalPayload(s.proposal,p.getWorld().getRegistryKey().getValue()));
    }
    public static void respond(ServerPlayerEntity p,JiahaoMomentResponsePayload response) {
        var s=state(p);
        if(!response.ready()&&s.active!=null&&s.active.id.equals(response.session())){cancel(p,s);return;}
        if(s.proposal==null||!s.proposal.equals(response.session()))return;
        UUID proposal=s.proposal;s.proposal=null;
        if(!response.ready()||now(p)>=s.proposalUntil||!s.enabled||s.active!=null||!safe(p)||!JiahaoQuoteManager.canStartMoment(p))return;
        s.active=new Session(p,proposal,now(p));
        p.setVelocity(Vec3d.ZERO);p.velocityModified=true;p.stopUsingItem();p.setSprinting(false);
        p.networkHandler.requestTeleport(p.getX(),p.getY(),p.getZ(),p.getYaw(),p.getPitch());
        send(p,s.active,true);
        p.sendMessage(Text.translatable("message.jiahao-mode.moment.loading"),true);
    }
    public static void clear(ServerPlayerEntity p,boolean forget) {
        var r=runtime(p.getServer());var s=r.players.get(p.getUuid());if(s==null)return;
        cancel(p,s);s.nextAt=-1;if(forget)r.players.remove(p.getUuid());
    }
    public static void cancelWorld(ServerWorld w) {
        var r=runtime(w.getServer());
        for(var p:w.getPlayers()){var s=r.players.get(p.getUuid());if(s!=null)cancel(p,s);}
    }
    private static void cancel(ServerPlayerEntity p,State s) {
        s.proposal=null;
        if(s.active==null)return;
        var a=s.active;s.active=null;
        send(p,a,false);JiahaoQuoteManager.cancelSession(p,a.id);schedule(p,s);
    }
    private static JiahaoMomentStatePayload payload(ServerPlayerEntity p,Session a,boolean active) {
        return new JiahaoMomentStatePayload(p.getUuid(),a.dimension,a.id,active,(int)Math.max(0,now(p)-a.start),a.pose,a.origin,a.yaw,a.arc);
    }
    private static void send(ServerPlayerEntity p,Session a,boolean active) {
        var packet=payload(p,a,active);
        if(active)for(var recipient:p.getServerWorld().getPlayers()) {
            if(recipient.squaredDistanceTo(a.origin)<=64*64&&ServerPlayNetworking.canSend(recipient,JiahaoMomentStatePayload.ID)) {
                ServerPlayNetworking.send(recipient,packet);a.recipients.add(recipient.getUuid());
            }
        }
        else for(var id:a.recipients){var recipient=p.getServer().getPlayerManager().getPlayer(id);
            if(recipient!=null&&ServerPlayNetworking.canSend(recipient,JiahaoMomentStatePayload.ID))ServerPlayNetworking.send(recipient,packet);}
    }
    private static void syncObserver(ServerPlayerEntity observer) {
        if(!ServerPlayNetworking.canSend(observer,JiahaoMomentStatePayload.ID))return;
        for(var actor:observer.getServerWorld().getPlayers()) {
            var r=SERVERS.get(actor.getServer());var s=r==null?null:r.players.get(actor.getUuid());
            if(s!=null&&s.active!=null&&observer.squaredDistanceTo(s.active.origin)<=64*64) {
                ServerPlayNetworking.send(observer,payload(actor,s.active,true));s.active.recipients.add(observer.getUuid());
            }
        }
    }
    private static void tick(MinecraftServer server) {
        var r=runtime(server);
        for(var p:server.getPlayerManager().getPlayerList()) {
            var s=r.players.get(p.getUuid());if(s==null)continue;
            if(!s.enabled||!JiahaoStateManager.isJiahao(p)||!p.isAlive()){cancel(p,s);s.nextAt=-1;continue;}
            if(s.nextAt<0)schedule(p,s);
            if(s.proposal!=null&&now(p)>=s.proposalUntil)s.proposal=null;
            var a=s.active;
            if(a!=null) {
                long age=now(p)-a.start;
                if(age>=DURATION||!a.dimension.equals(p.getWorld().getRegistryKey().getValue())
                    ||p.getPos().squaredDistanceTo(a.origin)>.0025||!JiahaoTimeStopManager.isStableForCinematic(p)
                    ||p.currentScreenHandler!=p.playerScreenHandler||JiahaoTimeStopManager.isTimeStopped(p.getWorld())
                    ||(age%5==0&&!safe(p))){cancel(p,s);continue;}
                p.setVelocity(Vec3d.ZERO);
                if(age>=26&&!a.cue){a.cue=true;JiahaoQuoteManager.emit(p,JiahaoQuoteCategory.RANDOM_MOMENT,a.id);}
                if(age%10==0)send(p,a,true);
            } else if(now(p)>=s.nextAt)propose(p,s);
        }
    }
}
