// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.hao.HaoMeterManager;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import java.nio.file.*;

/** Real networked right-clicks and native jukebox sound playback in an isolated world. */
public final class MusicDiscSmoke implements ClientModInitializer {
    private final Item[] discs={ModItems.MUSIC_DISC_JIAHAO_MARCH,ModItems.MUSIC_DISC_NEVADA,ModItems.MUSIC_DISC_SPECTRE};
    private final String[] songs={"jiahao_march","nevada","spectre"};
    private final BlockPos jukebox=new BlockPos(1,180,0);
    private int total,ticks,stage,cycle;private boolean done;private SoundInstance voice;
    public void onInitializeClient() {
        if(!Boolean.getBoolean("jiahao.discs.smoke"))return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(MinecraftClient c) {
        if(done)return;
        try {
            check(++total<1000,"Disc smoke watchdog stage="+stage);ticks++;
            if(stage==0) {
                if(c.world==null||c.player==null||c.getOverlay()!=null||ticks<100)return;
                c.getSoundManager().registerListener((sound,set,range)->{
                    if(cycle<songs.length&&sound.getId().equals(JiahaoMode.id("music_disc."+songs[cycle])))voice=sound;
                });
                c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(90);
                c.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(1.0);
                c.options.getSoundVolumeOption(SoundCategory.RECORDS).setValue(1.0);
                c.setScreen(null);var id=c.player.getUuid();
                c.getServer().execute(()->{
                    var p=c.getServer().getPlayerManager().getPlayer(id);var world=p.getServerWorld();
                    JiahaoTimeStopManager.stopTimeStop(p);JiahaoStateManager.setJiahao(p,false);HaoMeterManager.set(p,0);
                    p.getInventory().clear();p.changeGameMode(GameMode.SURVIVAL);p.setHealth(20);p.setNoGravity(true);
                    p.teleport(world,.5,180,2.5,java.util.Set.of(),180,0);
                    world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,c.getServer());
                    for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                        world.setBlockState(new BlockPos(x,179,z),Blocks.STONE.getDefaultState());
                        for(int y=180;y<185;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                    }
                    world.setBlockState(jukebox,Blocks.JUKEBOX.getDefaultState());
                    for(int i=0;i<discs.length;i++)p.getInventory().setStack(i,new ItemStack(discs[i]));
                    p.currentScreenHandler.sendContentUpdates();
                });stage=1;ticks=0;return;
            }
            if(stage==1) {
                if(ticks==25){c.player.getInventory().selectedSlot=cycle;c.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(cycle));}
                if(ticks==35){
                    check(c.player.getMainHandStack().isOf(discs[cycle]),"Disc item synced and selected");
                    var resource=c.getSoundManager().get(JiahaoMode.id("music_disc."+songs[cycle]));
                    check(resource!=null&&resource.getWeight()>0,"Bundled disc sound resource loaded");
                    voice=null;interact(c);stage=2;ticks=0;
                }
            } else if(stage==2) {
                if(ticks==20){
                    check(c.world.getBlockState(jukebox).get(JukeboxBlock.HAS_RECORD),"Right-click inserts disc on server");
                    check(voice!=null&&voice.getCategory()==SoundCategory.RECORDS&&c.getSoundManager().isPlaying(voice),"Native jukebox sound engine is playing "+songs[cycle]);
                    check(c.player.getMainHandStack().isEmpty(),"Survival player consumes one inventory disc");
                    HaoCapture.pending="music-disc-"+songs[cycle]+".png";
                }
                if(ticks==60)interact(c);
                if(ticks==75){
                    check(!c.world.getBlockState(jukebox).get(JukeboxBlock.HAS_RECORD),"Right-click ejects disc");
                    check(!c.getSoundManager().isPlaying(voice),"Ejection stops native music voice");
                    JiahaoMode.LOGGER.info("MUSIC DISC CLIENT {} PASSED",songs[cycle]);
                    if(++cycle==discs.length){finish(c,null);return;}
                    stage=1;ticks=0;
                }
            }
        } catch(Throwable error) {error.printStackTrace();finish(c,error);}
    }
    private void interact(MinecraftClient c) {
        c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(jukebox),Direction.UP,jukebox,false));
    }
    private void finish(MinecraftClient c,Throwable error) {
        done=true;try {Files.writeString(Path.of("music-discs-result.txt"),error==null?"PASSED":"FAILED "+error);}
        catch(Exception e){throw new RuntimeException(e);}
        if(c.getServer()!=null)c.getServer().stop(false);
        c.disconnect();c.scheduleStop();
    }
    private static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
