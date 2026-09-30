// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.cinematic;

import com.shouyun.jiahaomode.cinematic.*;
import com.shouyun.jiahaomode.client.*;
import com.shouyun.jiahaomode.moment.JiahaoMomentView;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.timestop.JiahaoTimeView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Per-actor poses; a single local camera owner with explicit priority. */
public final class JiahaoCinematicController {
    private static final Map<UUID,Session> ACTORS=new HashMap<>();
    private static final Map<UUID,Boolean> ENDED=new LinkedHashMap<>();
    private static ClientWorld world;
    private static Session time,camera;
    private static boolean returning;
    private static double returnStart,returnWeight,returnBars,rawFrame;
    private static long clientTicks;
    private static final class Session {
        final UUID id,owner;
        final CinematicType type;
        final JiahaoPoseType pose;
        List<JiahaoPoseType> poses;
        final Vec3d origin;
        final float yaw;
        final int arc;
        final CinematicTimeline timeline;
        double elapsed;
        boolean playing=true;
        Session(UUID id,UUID owner,CinematicType type,JiahaoPoseType pose,Vec3d origin,float yaw,int arc,int elapsed) {
            this.id=id;this.owner=owner;this.type=type;this.pose=pose;this.poses=List.of(pose);this.origin=origin;this.yaw=yaw;this.arc=arc;
            timeline=new CinematicTimeline(type.duration);timeline.start(elapsed);this.elapsed=elapsed;playing=elapsed<type.duration;
        }
    }
    private JiahaoCinematicController() {}
    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(client->{
            validate(client);JiahaoCinematicInput.update(client);
            if(!client.isPaused()){clientTicks++;for(var s:ACTORS.values())if(s.playing)s.timeline.tick();}
            publishMovementLock();
        });
    }
    private static boolean local(Session s) {
        var p=MinecraftClient.getInstance().player;return s!=null&&p!=null&&p.getWorld()==world&&p.getUuid().equals(s.owner);
    }
    private static void useWorld(ClientWorld next) {if(world!=next){cleanup();world=next;}}
    private static void start(Session s) {
        ACTORS.put(s.owner,s);
        if(local(s)&&s.playing) {
            if(camera!=null&&(camera.playing||returning)&&camera.type.priority>s.type.priority){s.playing=false;return;}
            camera=s;returning=false;JiahaoCinematicCamera.reset();JiahaoDodgeClientController.suppressCamera();
            var p=MinecraftClient.getInstance().player;p.setVelocity(Vec3d.ZERO);p.stopUsingItem();p.setSprinting(false);
        }
    }
    public static void onStateSync(ClientWorld next,JiahaoTimeStatePayload state) {
        useWorld(next);
        if(state.active()&&state.reason()==com.shouyun.jiahaomode.timestop.TimeStopReason.HAO_BURST){if(time!=null)finish(time,true);time=null;publishMovementLock();return;}
        if(!state.active()||!state.cinematic()) {if(time!=null)finish(time,false);time=null;publishMovementLock();return;}
        if(time==null||!state.session().equals(time.id)) {
            if(time!=null)finish(time,true);
            if(ENDED.containsKey(state.session()))return;
            time=new Session(state.session(),state.owner(),CinematicType.TIME_STOP,state.pose(),state.origin(),state.yaw(),0,state.elapsedTicks());
            for(var s:new ArrayList<>(ACTORS.values()))if(s.type==CinematicType.RANDOM_HAO_MOMENT)finish(s,true);
            start(time);
        } else time.timeline.sync(state.elapsedTicks());
        if(local(time)&&time.playing&&MinecraftClient.getInstance().getCameraEntity()!=MinecraftClient.getInstance().player) {
            if(ClientPlayNetworking.canSend(JiahaoQuotePlaybackFailedPayload.ID))ClientPlayNetworking.send(new JiahaoQuotePlaybackFailedPayload(time.id));
            finish(time,true);
        }
        publishMovementLock();
    }
    public static void onBurstSync(HaoBurstPayload p) {
        var client=MinecraftClient.getInstance();useWorld(client.world);
        if(world==null||!world.getRegistryKey().getValue().equals(p.dimension()))return;
        var old=ACTORS.get(p.player());
        if(!p.active()) {
            if(old!=null&&old.id.equals(p.session()))finish(old,true);
            remember(p.session());publishMovementLock();return;
        }
        if(p.elapsed()<0||p.elapsed()>=240||ENDED.containsKey(p.session()))return;
        if(old!=null&&old.id.equals(p.session()))old.timeline.sync(p.elapsed());
        else {
            if(old!=null)finish(old,true);
            var session=new Session(p.session(),p.player(),CinematicType.HAO_BURST,p.poses().getFirst(),p.origin(),p.yaw(),0,p.elapsed());
            session.poses=p.poses();start(session);
        }
        publishMovementLock();
    }
    public static void onMomentSync(JiahaoMomentStatePayload p) {
        var client=MinecraftClient.getInstance();useWorld(client.world);
        if(world==null||!world.getRegistryKey().getValue().equals(p.dimension()))return;
        var old=ACTORS.get(p.player());
        if(!p.active()) {if(old!=null&&old.id.equals(p.session()))finish(old,true);remember(p.session());publishMovementLock();return;}
        if(p.elapsed()<0||p.elapsed()>=60||ENDED.containsKey(p.session())||p.arc()!=90&&p.arc()!=120&&p.arc()!=180)return;
        if(old!=null&&old.type.priority>CinematicType.RANDOM_HAO_MOMENT.priority)return;
        if(old!=null&&old.id.equals(p.session()))old.timeline.sync(p.elapsed());
        else {
            var s=new Session(p.session(),p.player(),CinematicType.RANDOM_HAO_MOMENT,p.pose(),p.origin(),p.yaw(),p.arc(),p.elapsed());
            if(local(s)&&(!JiahaoClientConfig.enableRandomJiahaoMoments||client.currentScreen!=null||client.getCameraEntity()!=client.player
                    ||isCameraActive()||JiahaoDodgeClientController.locksMovement())){decline(s);return;}
            if(old!=null)finish(old,true);start(s);
        }
        publishMovementLock();
    }
    private static void decline(Session s) {
        remember(s.id);
        if(ClientPlayNetworking.canSend(JiahaoMomentResponsePayload.ID))ClientPlayNetworking.send(new JiahaoMomentResponsePayload(s.id,false));
        finish(s,true);
    }
    public static void onScreenOpened() {
        if(camera!=null&&camera.playing&&camera.type==CinematicType.RANDOM_HAO_MOMENT){decline(camera);publishMovementLock();}
    }
    public static void beginFrame(float delta) {
        var c=MinecraftClient.getInstance();validate(c);
        if(!c.isPaused())rawFrame=Math.max(rawFrame,clientTicks+CinematicTimeline.clamp(delta));
        for(var s:ACTORS.values()) {
            if(s.playing&&!c.isPaused())s.elapsed=s.timeline.sample(delta);
            if(s.elapsed>=s.type.duration)s.playing=false;
        }
        if(camera!=null&&!camera.playing&&!returning)JiahaoCinematicCamera.reset();
        if(returning&&rawFrame-returnStart>=3){returning=false;JiahaoCinematicCamera.reset();}
        publishMovementLock();
    }
    private static void validate(MinecraftClient client) {
        if(world!=null&&(client.world!=world||client.player==null||!client.player.isAlive())){cleanup();return;}
        if(world==null)return;
        if(camera!=null&&camera.playing&&camera.type==CinematicType.RANDOM_HAO_MOMENT
                &&(client.currentScreen!=null||client.getCameraEntity()!=client.player||!JiahaoClientConfig.enableRandomJiahaoMoments))decline(camera);
        if(camera!=null&&camera.playing&&camera.type==CinematicType.HAO_BURST&&client.getCameraEntity()!=client.player)finish(camera,true);
        var iterator=ACTORS.values().iterator();
        while(iterator.hasNext()) {
            var s=iterator.next();
            var p=world.getPlayerByUuid(s.owner);
            if(p!=null&&(!p.isAlive()||p.isRemoved()||!com.shouyun.jiahaomode.state.JiahaoStateManager.isJiahao(p))) {
                s.playing=false;p.setAttached(JiahaoMomentView.LOCKED,false);iterator.remove();
                if(s==camera){returning=false;JiahaoCinematicCamera.reset();if(s.type==CinematicType.HAO_BURST)com.shouyun.jiahaomode.client.HaoMarchSound.stop();}
            }
        }
    }
    private static void finish(Session s,boolean immediate) {
        remember(s.id);
        if(s.type==CinematicType.HAO_BURST&&s.playing&&local(s)&&s.elapsed<240&&world!=null&&ClientPlayNetworking.canSend(HaoReadyPayload.ID))
            ClientPlayNetworking.send(new HaoReadyPayload(s.id,world.getRegistryKey().getValue(),false));
        if(s==camera) {
            if(s.type==CinematicType.HAO_BURST)com.shouyun.jiahaomode.client.HaoMarchSound.stop();
            if(!immediate&&s.playing&&local(s)&&s.elapsed<s.type.duration){returnWeight=cameraWeight();returnBars=barOpacity();returnStart=rawFrame;returning=true;}
            else if(immediate){returning=false;JiahaoCinematicCamera.reset();}
        }
        s.playing=false;ACTORS.remove(s.owner,s);
        if(world!=null){var p=world.getPlayerByUuid(s.owner);if(p!=null)p.setAttached(JiahaoMomentView.LOCKED,false);}
    }
    private static void remember(UUID id){ENDED.put(id,true);if(ENDED.size()>256)ENDED.remove(ENDED.keySet().iterator().next());}
    public static void stop(boolean immediate) {if(camera!=null&&(camera.playing||returning))finish(camera,immediate);else if(time!=null)finish(time,immediate);publishMovementLock();}
    public static void cleanup() {
        if(world!=null)for(var p:world.getPlayers())p.setAttached(JiahaoMomentView.LOCKED,false);
        com.shouyun.jiahaomode.client.HaoMarchSound.stop();
        ACTORS.clear();ENDED.clear();time=camera=null;world=null;returning=false;rawFrame=0;clientTicks=0;
        JiahaoCinematicCamera.reset();JiahaoCinematicInput.reset();
    }
    private static void publishMovementLock() {
        if(world==null)return;
        var view=world.getAttachedOrElse(JiahaoTimeView.CLIENT_VIEW,JiahaoTimeView.INACTIVE);
        var frozenActor=ACTORS.get(view.owner());
        boolean locked=time!=null&&time.playing&&time.elapsed<100 || frozenActor!=null&&frozenActor.playing&&frozenActor.type==CinematicType.HAO_BURST;
        if(view.cinematicLocked()!=locked)world.setAttached(JiahaoTimeView.CLIENT_VIEW,new JiahaoTimeView(view.active(),view.owner(),view.remainingTicks(),locked));
        for(var s:ACTORS.values()) {
            var p=world.getPlayerByUuid(s.owner);if(p==null)continue;
            boolean moment=s.playing&&s.type==CinematicType.RANDOM_HAO_MOMENT;
            if(p.getAttachedOrElse(JiahaoMomentView.LOCKED,false)!=moment)p.setAttached(JiahaoMomentView.LOCKED,moment);
        }
    }
    private static Session selected() {return camera!=null&&(camera.playing||returning)?camera:time!=null?time:camera;}
    public static boolean isLocalOwner() {return local(selected());}
    public static boolean locksInput() {return camera!=null&&camera.playing&&local(camera);}
    public static boolean isCameraActive() {return camera!=null&&(camera.playing||returning)&&local(camera);}
    public static boolean canUseCamera(CinematicType type) {return !isCameraActive()||camera.type.priority<=type.priority;}
    public static CinematicType cameraType() {return isCameraActive()?camera.type:null;}
    public static boolean isPoseActive(PlayerEntity p) {var s=ACTORS.get(p.getUuid());return p.getWorld()==world&&s!=null&&s.playing;}
    public static JiahaoPoseType poseType(PlayerEntity p) {var s=ACTORS.get(p.getUuid());return s==null?JiahaoPoseType.DEFAULT:s.poses.get(com.shouyun.jiahaomode.hao.HaoBurstTimeline.index(s.elapsed,s.poses.size()));}
    public static boolean isBurstPose(PlayerEntity p) {var s=ACTORS.get(p.getUuid());return s!=null&&s.type==CinematicType.HAO_BURST;}
    public static double poseDegrees(PlayerEntity p,int part,int axis) {
        var s=ACTORS.get(p.getUuid());if(s==null)return JiahaoPoseType.DEFAULT.degrees(part,axis);
        if(s.type!=CinematicType.HAO_BURST)return s.pose.degrees(part,axis);
        int index=com.shouyun.jiahaomode.hao.HaoBurstTimeline.index(s.elapsed,s.poses.size());
        var next=s.poses.get(index);var previous=s.poses.get(Math.max(0,index-1));
        double weight=com.shouyun.jiahaomode.hao.HaoBurstTimeline.transition(s.elapsed,s.poses.size());
        return CinematicTimeline.lerp(previous.degrees(part,axis),next.degrees(part,axis),weight);
    }
    public static double poseElapsed(PlayerEntity p) {var s=ACTORS.get(p.getUuid());return s==null?0:s.elapsed;}
    public static double poseDuration(PlayerEntity p) {var s=ACTORS.get(p.getUuid());return s==null?100:s.type.duration;}
    public static float yaw(PlayerEntity p) {var s=ACTORS.get(p.getUuid());return s==null?p.bodyYaw:s.yaw;}
    public static UUID sessionId() {var s=selected();return s==null?null:s.id;}
    public static double elapsedTicks() {var s=selected();return s==null?0:s.elapsed;}
    public static double getProgress() {var s=selected();return s==null?0:s.elapsed/s.type.duration;}
    public static Vec3d origin() {var s=selected();return s==null?Vec3d.ZERO:s.origin;}
    public static float yaw() {var s=selected();return s==null?0:s.yaw;}
    public static double rawFrame() {return rawFrame;}
    public static boolean isReturning() {return returning;}
    public static double cameraWeight() {
        if(camera==null)return 0;
        if(returning)return returnWeight*(1-CinematicTimeline.smooth((rawFrame-returnStart)/3));
        double t=camera.elapsed;
        if(camera.type==CinematicType.HAO_BURST)return com.shouyun.jiahaomode.hao.HaoBurstTimeline.cameraWeight(t);
        return camera.type==CinematicType.TIME_STOP?CinematicTimeline.weight(t):CinematicTimeline.smooth((t-6)/6)*(1-CinematicTimeline.smooth((t-56)/4));
    }
    public static double barOpacity() {
        if(!isCameraActive())return 0;
        if(returning)return returnBars*(1-CinematicTimeline.smooth((rawFrame-returnStart)/3));
        return camera.type==CinematicType.TIME_STOP?CinematicTimeline.bars(camera.elapsed):cameraWeight()*.75;
    }
    public static double orbitAngle(double ticks) {
        var s=selected();if(s==null)return 0;
        if(s.type==CinematicType.HAO_BURST)return burstOrbit(ticks,0);
        if(s.type==CinematicType.TIME_STOP)return CinematicTimeline.angle(ticks)+(s.pose==JiahaoPoseType.RUNNING_LOOK_BACK?135:0);
        double end=s.pose==JiahaoPoseType.RUNNING_LOOK_BACK?135:s.arc*.5;
        return CinematicTimeline.lerp(end-s.arc,end,CinematicTimeline.cubic((ticks-12)/40));
    }
    public static double orbitRadius(double ticks) {if(cameraType()==CinematicType.HAO_BURST)return burstOrbit(ticks,1);return cameraType()==CinematicType.RANDOM_HAO_MOMENT?CinematicTimeline.lerp(3.3,2.5,CinematicTimeline.smooth((ticks-12)/40)):CinematicTimeline.radius(ticks);}
    public static double orbitHeight(double ticks) {if(cameraType()==CinematicType.HAO_BURST)return burstOrbit(ticks,2);return cameraType()==CinematicType.RANDOM_HAO_MOMENT?1.5:CinematicTimeline.height(ticks);}
    private static double preferred(JiahaoPoseType pose,int component) {
        if(component==0)return switch(pose){case RUNNING_FREEZE->100;case RUNNING_LOOK_BACK->210;case POINT_SKY->30;case THINKING_HAO->-25;case RAIN_EMBRACE->65;default->95;};
        if(component==1)return switch(pose){case THINKING_HAO->2.3;case RAIN_EMBRACE->4.5;default->3.3;};
        return switch(pose){case RUNNING_FREEZE,POINT_SKY->.75;case RAIN_EMBRACE->1.8;default->1.4;};
    }
    private static double burstOrbit(double ticks,int component) {
        var s=selected();if(s==null)return 0;
        int index=com.shouyun.jiahaomode.hao.HaoBurstTimeline.index(ticks,s.poses.size());
        double start=com.shouyun.jiahaomode.hao.HaoBurstTimeline.poseStart(index,s.poses.size());
        double length=(170-14)/(double)s.poses.size();
        double from=preferred(s.poses.get(Math.max(0,index-1)),component),to=preferred(s.poses.get(index),component);
        if(component==0){if(index==0)from-=30;to=from+net.minecraft.util.math.MathHelper.wrapDegrees(to-from);}
        return CinematicTimeline.lerp(from,to,CinematicTimeline.smooth((ticks-start)/length));
    }
}
