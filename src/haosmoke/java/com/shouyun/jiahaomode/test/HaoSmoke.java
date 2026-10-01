// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.hao.*;
import com.shouyun.jiahaomode.client.*;
import com.shouyun.jiahaomode.client.cinematic.*;
import com.shouyun.jiahaomode.client.gadget.*;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.cinematic.*;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.JiahaoQuoteCategory;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Uses the real renderer, network, sound engine and isolated integrated server. */
public final class HaoSmoke implements ClientModInitializer {
    private HaoBurstPayload replay;
    private int ticks,total,stage,cycle,frames,moving;private boolean done,used,seenMusic;private String failure;
    private double weather,shotPoseAt;private JiahaoPoseType shotPose;private Vec3d lastCamera;private Perspective perspective;private int fov;
    private final Set<JiahaoPoseType> seenPoses=new HashSet<>();private final Set<String> quotes=new HashSet<>(),shots=new HashSet<>();
    private volatile int serverUnits,baseUnits;private volatile long baseTick;private volatile boolean serverCheck,serverBuff;
    public void onInitializeClient(){
        if(Boolean.getBoolean("jiahao.hao.dedicated")||Boolean.getBoolean("jiahao.gadget.smoke")||Boolean.getBoolean("jiahao.discs.smoke"))return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(context->{
            if(done||stage!=5||!JiahaoCinematicController.isCameraActive())return;
            try {
                var c=MinecraftClient.getInstance();frames++;
                check(JiahaoTimeStopClientState.isTimeStopped(c.world),"Client world freeze");
                check(Math.abs(JiahaoTimeStopClientState.getWorldAnimationTime()-weather)<.00001,"Rain/cloud visual clock freezes");
                check(c.options.getPerspective()==perspective&&c.options.getFov().getValue()==fov,"Perspective and FOV unchanged");
                var camera=context.camera().getPos();if(lastCamera!=null&&camera.squaredDistanceTo(lastCamera)>1e-8)moving++;lastCamera=camera;
                seenPoses.add(JiahaoCinematicController.poseType(c.player));
            }catch(Throwable e){failure=e.toString();}
        });
        HudRenderCallback.EVENT.register((draw,counter)->{
            if(stage==5&&cycle==0&&JiahaoCinematicController.isCameraActive()){
                double age=JiahaoCinematicController.elapsedTicks();
                var currentPose=JiahaoCinematicController.poseType(MinecraftClient.getInstance().player);if(currentPose!=shotPose){shotPose=currentPose;shotPoseAt=age;}
                if(age>22&&age<200&&age-shotPoseAt>=6){String shot="hao-"+JiahaoCinematicController.poseType(MinecraftClient.getInstance().player).name().toLowerCase(Locale.ROOT)+".png";if(shots.add(shot))HaoCapture.pending=shot;}
            }
        });
    }
    private void tick(MinecraftClient c){
        if(done)return;
        try {
            if(failure!=null)throw new AssertionError(failure);
            check(++total<2400,"Client watchdog stage="+stage);ticks++;
            if(stage==0){
                if(c.player==null||c.world==null||c.getServer()==null||c.getOverlay()!=null||ticks<100)return;
                if(!c.player.isAlive()){c.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket(net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));return;}
                c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(90);c.setScreen(null);
                ClientPlayNetworking.send(new JiahaoMomentPreferencePayload(false));configure(c);stage=1;ticks=0;return;
            }
            if(stage==1&&ticks>70){
                if(cycle==0){c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoMarketScreen,"Real right click opens Market item="+c.player.getMainHandStack()+" form="+JiahaoStateManager.isJiahao(c.player)+" lock="+JiahaoCinematicController.locksInput()+" dodge="+JiahaoDodgeClientController.locksMovement());stage=2;ticks=0;return;}
                arm(c);return;
            }
            if(stage==2){
                if(ticks==10){var screen=(JiahaoMarketScreen)c.currentScreen;var buttons=screen.children().stream().filter(x->x instanceof ButtonWidget).map(x->(ButtonWidget)x).toList();baseline(c);buttons.getFirst().onPress();}
                if(ticks==45){snapshot(c);}
                if(ticks==55){check(serverCheck&&serverBuff,"Real Market completion rewards +2 and status effect units="+serverUnits);c.setScreen(null);var uuid=c.player.getUuid();c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(uuid);HaoMeterManager.set(p,0);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_CODE_EDITOR));});stage=3;ticks=0;}
            }
            if(stage==3){
                if(ticks==15){c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoCodeScreen,"Real right click opens Code");}
                if(ticks==22){var screen=(JiahaoCodeScreen)c.currentScreen;var input=(TextFieldWidget)screen.children().stream().filter(x->x instanceof TextFieldWidget).findFirst().orElseThrow();baseline(c);input.setText("print(hao)");screen.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);}
                if(ticks==65)snapshot(c);
                if(ticks==75){check(serverCheck&&serverBuff,"Real Code completion rewards +2 and effect units="+serverUnits);c.setScreen(null);arm(c);}
            }
            if(stage==4){
                if(JiahaoCinematicController.cameraType()==CinematicType.HAO_BURST){
                    frames=moving=0;lastCamera=null;seenMusic=false;used=false;seenPoses.clear();quotes.clear();weather=JiahaoTimeStopClientState.getWorldAnimationTime();perspective=c.options.getPerspective();fov=c.options.getFov().getValue();stage=5;ticks=0;
                }else check(ticks<100,"set 99 automatically starts without a key");
            }
            if(stage==5){
                double age=JiahaoCinematicController.elapsedTicks();
                if(JiahaoCinematicController.isCameraActive()){
                    check(JiahaoCinematicController.locksInput(),"Full-session input lock");
                    var q=JiahaoQuoteClientState.local();if(q!=null&&q.quote.category()==JiahaoQuoteCategory.HAO_BURST)quotes.add(q.quote.translationKey());
                    var sound=sound();if(age>25&&age<210){check(sound!=null&&c.getSoundManager().isPlaying(sound),"Real resource sound instance is playing");seenMusic=true;}
                    if(cycle==0&&age>30&&!used){used=true;ClientPlayNetworking.send(JiahaoTimeTogglePayload.INSTANCE);
                        var soundBefore=sound();double elapsedBefore=JiahaoCinematicController.elapsedTicks();
                        replay=new HaoBurstPayload(c.player.getUuid(),c.world.getRegistryKey().getValue(),JiahaoCinematicController.sessionId(),true,0,List.of(JiahaoPoseType.POINT_SKY,JiahaoPoseType.THINKING_HAO,JiahaoPoseType.RAIN_EMBRACE),c.player.getPos(),c.player.getYaw());
                        JiahaoCinematicController.onBurstSync(replay);check(JiahaoCinematicController.elapsedTicks()>=elapsedBefore&&sound()==soundBefore,"Duplicate/outdated sync never restarts camera or music");}
                    if(cycle>0&&age>35&&!used){
                        used=true;var server=c.getServer();var uuid=c.player.getUuid();int mode=cycle;
                        server.execute(()->{var p=server.getPlayerManager().getPlayer(uuid);if(mode==1)JiahaoStateManager.setJiahao(p,false);else if(mode==2)p.teleportTo(new TeleportTarget(server.getWorld(World.NETHER),new Vec3d(0,80,0),Vec3d.ZERO,0,0,TeleportTarget.NO_OP));else {p.setHealth(0);p.onDeath(p.getServerWorld().getDamageSources().generic());}});
                    }
                    return;
                }
                check(sound()==null&&!JiahaoCinematicController.locksInput(),"End/abort removes music and control lock");check(!JiahaoTimeStopClientState.isTimeStopped(c.world),"End/abort resumes world");
                if(cycle==0){JiahaoCinematicController.onBurstSync(replay);check(!JiahaoCinematicController.isCameraActive()&&sound()==null,"Ended session packet never replays");check(frames>40&&moving>30,"Real camera moves on rendered frames");check(seenPoses.size()>=3&&seenPoses.size()<=5,"Multiple real rendered poses");check(quotes.size()>=2&&quotes.size()<=4,"Spaced real translated subtitles");check(seenMusic,"Sound was actually active");}
                JiahaoMode.LOGGER.info("HAO CLIENT CYCLE {} PASSED poses={} quotes={} frames={}",cycle,seenPoses.size(),quotes.size(),frames);
                if(cycle==3){finish(c,true);return;}cycle++;configure(c);stage=1;ticks=0;
            }
        }catch(Throwable e){e.printStackTrace();failure=e.toString();finish(c,false);}
    }
    private void baseline(MinecraftClient c){var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->{baseUnits=HaoMeterManager.data(server.getPlayerManager().getPlayer(uuid)).units();baseTick=com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.getServerTick(server);serverCheck=false;});}
    private void snapshot(MinecraftClient c){var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->{var p=server.getPlayerManager().getPlayer(uuid);serverUnits=HaoMeterManager.data(p).units();serverBuff=!p.getStatusEffects().isEmpty();long extra=serverUnits-baseUnits-(com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.getServerTick(server)-baseTick)*5;serverCheck=extra>=195&&extra<=205;});}
    private void arm(MinecraftClient c){var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->server.getCommandManager().executeWithPrefix(server.getPlayerManager().getPlayer(uuid).getCommandSource().withLevel(4),"jiahao hao set 99"));stage=4;ticks=0;}
    private void configure(MinecraftClient c){
        c.player.getInventory().selectedSlot=0;ClientPlayNetworking.getSender().sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(0));
        var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->{
            var p=server.getPlayerManager().getPlayer(uuid);var w=server.getWorld(World.OVERWORLD);
            w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);w.getGameRules().get(GameRules.NATURAL_REGENERATION).set(false,server);
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){w.setBlockState(new BlockPos(x,179,z),Blocks.STONE.getDefaultState());for(int y=180;y<=186;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
            p.teleportTo(new TeleportTarget(w,new Vec3d(.5,180,.5),Vec3d.ZERO,180,0,TeleportTarget.NO_OP));p.changeGameMode(GameMode.SURVIVAL);p.getAbilities().flying=false;p.sendAbilitiesUpdate();
            p.equipStack(EquipmentSlot.HEAD,new ItemStack(ModItems.JIAHAO_HELMET));p.equipStack(EquipmentSlot.CHEST,new ItemStack(ModItems.JIAHAO_CHESTPLATE));p.equipStack(EquipmentSlot.LEGS,new ItemStack(ModItems.JIAHAO_LEGGINGS));p.equipStack(EquipmentSlot.FEET,new ItemStack(ModItems.JIAHAO_BOOTS));
            p.getInventory().selectedSlot=0;p.setHealth(5);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));JiahaoStateManager.setJiahao(p,true);HaoMeterManager.set(p,0);
            server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4),"fillbiome -8 179 -8 8 186 8 minecraft:plains");w.setWeather(0,12000,true,false);
            var enemy=EntityType.ZOMBIE.create(w);enemy.setPosition(4.5,180,.5);enemy.setAiDisabled(true);enemy.setTarget(p);w.spawnEntity(enemy);com.shouyun.jiahaomode.moment.JiahaoMomentManager.combat(p);
        });
    }
    private static SoundInstance sound(){return JiahaoMusicController.soundInstance();}
    private void finish(MinecraftClient c,boolean success){
        if(done)return;done=true;c.setScreen(null);if(c.getServer()!=null)c.getServer().stop(false);c.disconnect();
        try{check(sound()==null&&!JiahaoCinematicController.isCameraActive(),"Disconnect cleanup");Files.writeString(Path.of("hao-smoke-result.txt"),success?"PASSED":"FAILED stage="+stage+" cycle="+cycle+" "+failure);}catch(Exception e){throw new RuntimeException(e);}
        c.scheduleStop();
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
