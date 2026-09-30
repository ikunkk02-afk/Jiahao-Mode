// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.cinematic.CinematicType;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.hao.HaoBurstTimeline;
import com.shouyun.jiahaomode.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import java.util.UUID;

/** One relative, resource-backed music instance. Never reads a system audio file. */
public final class HaoMarchSound extends MovingSoundInstance {
    private static HaoMarchSound playing;private static UUID attempted;
    private final UUID session;
    private HaoMarchSound(UUID session) {
        super(ModSounds.JIAHAO_MARCH,SoundCategory.MUSIC,SoundInstance.createRandom());this.session=session;
        relative=true;attenuationType=AttenuationType.NONE;repeat=false;volume=.001f;
    }
    public static void update(MinecraftClient c) {
        boolean active=c.world!=null&&c.player!=null&&c.player.isAlive()&&JiahaoCinematicController.cameraType()==CinematicType.HAO_BURST;
        if(!active){stop();return;}
        var id=JiahaoCinematicController.sessionId();double age=JiahaoCinematicController.elapsedTicks();
        if(playing!=null&&!playing.session.equals(id))stop();
        if(age>=6&&age<220&&!id.equals(attempted)) {
            attempted=id;
            var resource=c.getSoundManager().get(ModSounds.JIAHAO_MARCH.getId());
            if(resource==null||resource.getWeight()==0)return;
            playing=new HaoMarchSound(id);c.getSoundManager().play(playing);
        }
    }
    @Override public void tick() {
        if(!session.equals(JiahaoCinematicController.sessionId())||JiahaoCinematicController.cameraType()!=CinematicType.HAO_BURST){setDone();return;}
        double age=JiahaoCinematicController.elapsedTicks();volume=(float)HaoBurstTimeline.musicVolume(age);if(age>=240)setDone();
    }
    public static void onResourceReload() {
        var c=MinecraftClient.getInstance();c.execute(()->{var session=JiahaoCinematicController.sessionId();stop();attempted=session;});
    }
    public static void stop() {
        if(playing!=null){playing.setDone();MinecraftClient.getInstance().getSoundManager().stop(playing);playing=null;}
        attempted=null;
    }
}
