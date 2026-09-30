// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.client.*;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.cinematic.JiahaoPoseType;
import com.shouyun.jiahaomode.quote.JiahaoQuoteCategory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.shouyun.jiahaomode.network.JiahaoMomentPreferencePayload;
import net.minecraft.client.MinecraftClient;
import java.nio.file.*;
import java.util.*;

public final class HaoDedicatedClient implements ClientModInitializer {
    private int ticks,after;private boolean seen,done,preference;private double weather;
    private final Set<JiahaoPoseType> poses=new HashSet<>();private final Set<String> quotes=new HashSet<>(),shots=new HashSet<>();
    public void onInitializeClient(){
        if(!Boolean.getBoolean("jiahao.hao.dedicated"))return;
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(done)return;
            try{
                if(++ticks>2000)throw new AssertionError("Dedicated client watchdog");if(c.player==null||c.world==null||c.getOverlay()!=null)return;
                c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(60);c.setScreen(null);
                if(!preference&&ClientPlayNetworking.canSend(JiahaoMomentPreferencePayload.ID)){ClientPlayNetworking.send(new JiahaoMomentPreferencePayload(false));preference=true;}
                var actor=c.world.getPlayers().stream().filter(p->p.getName().getString().equals("HaoActor")).findFirst().orElse(null);if(actor==null)return;
                boolean local=c.player==actor;boolean frozen=JiahaoTimeStopClientState.isTimeStopped(c.world);
                if(frozen){
                    if(!seen){seen=true;weather=JiahaoTimeStopClientState.getWorldAnimationTime();}
                    check(Math.abs(JiahaoTimeStopClientState.getWorldAnimationTime()-weather)<.00001,"Weather stays fixed for both clients");
                    if(JiahaoCinematicController.isPoseActive(actor)){check(JiahaoCinematicController.isCameraActive()==local&&JiahaoCinematicController.locksInput()==local,"Only actor owns camera/input during active pose timeline");poses.add(JiahaoCinematicController.poseType(actor));}
                    if(!local)check(!JiahaoCinematicController.isCameraActive()&&!JiahaoCinematicController.locksInput(),"Observer never owns camera even across start/end packets");
                    var q=JiahaoQuoteClientState.active().get(actor.getUuid());if(q!=null&&q.quote.category()==JiahaoQuoteCategory.HAO_BURST)quotes.add(q.quote.translationKey());
                    var soundField=HaoMarchSound.class.getDeclaredField("playing");soundField.setAccessible(true);if(!local)check(soundField.get(null)==null,"Observer never hears actor march");
                }else if(seen&&++after>20){check(poses.size()>=3&&poses.size()<=5,"Both clients see multiple matching poses");check(quotes.size()>=2&&quotes.size()<=4,"Actor subtitles and observer speech bubbles");check(!JiahaoCinematicController.isCameraActive()&&!JiahaoCinematicController.locksInput(),"Clean end");finish(c,"PASSED");}
            }catch(Throwable e){e.printStackTrace();finish(c,"FAILED: "+e);}
        });
        HudRenderCallback.EVENT.register((draw,counter)->{
            var c=MinecraftClient.getInstance();if(c.player==null||c.world==null)return;
            var actor=c.world.getPlayers().stream().filter(p->p.getName().getString().equals("HaoActor")).findFirst().orElse(null);
            if(actor!=null&&JiahaoCinematicController.isPoseActive(actor)&&JiahaoCinematicController.poseElapsed(actor)>24){var pose=JiahaoCinematicController.poseType(actor);String name="dedicated-"+pose.name().toLowerCase(Locale.ROOT)+".png";if(shots.add(name))HaoCapture.pending=name;}
        });
    }
    private void finish(MinecraftClient c,String result){done=true;try{Files.writeString(Path.of("hao-client-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}c.disconnect();c.scheduleStop();}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
}
