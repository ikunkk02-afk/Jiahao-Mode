// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.multiplayer;

import com.shouyun.jiahaomode.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import java.nio.file.*;

/** Two independent JVM clients. Observer asserts remote movement/Pose and unchanged local camera. */
public final class DodgeMultiplayerClient implements ClientModInitializer {
    private int ticks,total,seenAt=-1,movementSamples,initialFov;
    private boolean pressed,pose,perfect,camera,done;
    private double startZ=Double.NaN,lastZ=Double.NaN;
    private static String shot;
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, counter)->{
            var c=MinecraftClient.getInstance();if(c.player==null||c.world==null)return;
            boolean actor=c.player.getName().getString().equals("DodgeActor");
            var target=actor?c.player:c.world.getPlayers().stream().filter(p->p.getName().getString().equals("DodgeActor")).findFirst().orElse(null);
            if(target==null)return;
            var v=JiahaoDodgeClientController.visual(target);
            if(v!=null&&v.weight()>.1) {pose=true;if(shot==null&&seenAt<0)shot=actor?"actor-perfect.png":"observer-dodge-pose.png";}
            if(v!=null&&v.perfect){perfect=true;if(seenAt<0)seenAt=ticks;}
            if(actor&&JiahaoDodgeClientController.fovOffset()>.2)camera=true;
            if(!actor&&(JiahaoDodgeClientController.fovOffset()!=0||JiahaoDodgeClientController.cameraRoll()!=0))throw new AssertionError("Observer camera hijacked");
        });
    }
    private void tick(MinecraftClient c) {
        if(done)return;
        try {
            if(++total>2400)throw new AssertionError("Multiplayer watchdog");
            if(c.player==null||c.world==null||c.getOverlay()!=null)return;
            c.options.pauseOnLostFocus=false;c.setScreen(null);ticks++;
            boolean actor=c.player.getName().getString().equals("DodgeActor");
            var target=actor?c.player:c.world.getPlayers().stream().filter(p->p.getName().getString().equals("DodgeActor")).findFirst().orElse(null);
            if(target==null)return;
            if(Double.isNaN(startZ)){startZ=lastZ=target.getZ();initialFov=c.options.getFov().getValue();}
            if(JiahaoDodgeClientController.visual(target)!=null&&Math.abs(target.getZ()-lastZ)>1e-5) {movementSamples++;lastZ=target.getZ();}
            if(actor) {
                c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                if(!pressed&&ticks>100&&c.world.getPlayers().size()>=2) {
                    startZ=c.player.getZ();KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(JiahaoDodgeKeyBindings.binding));pressed=true;
                }
            }
            if(seenAt>=0&&ticks-seenAt>25) {
                if(!pose||!perfect||actor&&!camera||movementSamples<2||Math.abs(target.getZ()-startZ+2.8)>.15
                        ||c.options.getFov().getValue()!=initialFov||JiahaoDodgeClientController.fovOffset()!=0||JiahaoDodgeClientController.cameraRoll()!=0)
                    throw new AssertionError("Remote/local visual endpoint failed: pose="+pose+" perfect="+perfect+" samples="+movementSamples+" delta="+(target.getZ()-startZ));
                finish(c,"PASSED");
            }
        }catch(Throwable e){e.printStackTrace();finish(c,"FAILED: "+e);}
    }
    public static void capture() {if(shot!=null){var c=MinecraftClient.getInstance();net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,shot,c.getFramebuffer(),t->{});shot=null;}}
    private void finish(MinecraftClient c,String result) {
        done=true;try{Files.writeString(Path.of("dodge-client-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}c.disconnect();c.scheduleStop();
    }
}
