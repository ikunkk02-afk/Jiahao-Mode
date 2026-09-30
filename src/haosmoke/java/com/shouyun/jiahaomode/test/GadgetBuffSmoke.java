// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.client.JiahaoMusicController;
import com.shouyun.jiahaomode.client.JiahaoTimeKeyBindings;
import com.shouyun.jiahaomode.client.gadget.*;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import com.shouyun.jiahaomode.hao.HaoMeterManager;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;

/** Real item interaction and R keybinding in an isolated singleplayer fixture. */
public final class GadgetBuffSmoke implements ClientModInitializer {
    private int stage,ticks,total,cycle;private boolean done;
    private volatile boolean checked,ok;
    private net.minecraft.client.sound.SoundInstance voice;
    private float fadeVolume=1;private boolean sawFade;
    public void onInitializeClient(){
        if(!Boolean.getBoolean("jiahao.gadget.smoke"))return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(MinecraftClient c){
        if(done)return;
        try{
            check(++total<2600,"Gadget smoke watchdog stage="+stage);ticks++;
            if(stage==0){
                if(c.player==null||c.world==null||c.getOverlay()!=null||ticks<100)return;
                c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(90);c.setScreen(null);
                ClientPlayNetworking.send(new JiahaoMomentPreferencePayload(false));
                setup(c);stage=1;ticks=0;return;
            }
            if(stage==1){
                if(ticks==45){pressR();check(JiahaoMusicController.soundInstance()==null,"R never plays before acknowledgment");}
                if(ticks==65){check(JiahaoMusicController.soundInstance()==null,"Normal form R rejected");c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoMarketScreen,"Normal Market GUI opens");}
                if(ticks==75)market(c,0);
                if(ticks==110){inspect(c,false);}
                if(ticks==120){check(checked&&ok,"Normal Market has no effect");c.setScreen(null);item(c,false,true);stage=2;ticks=0;}
            }
            if(stage==2){
                if(ticks==20){c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoCodeScreen,"Normal Code GUI opens");}
                String[] inputs={"hello","cmd","powershell","rm -rf","shutdown"};
                if(ticks>=30&&(ticks-30)%45==0&&(ticks-30)/45<inputs.length)code(c,inputs[(ticks-30)/45]);
                if(ticks==245){inspect(c,false);}
                if(ticks==255){check(checked&&ok,"Dangerous-looking strings stay fictional and normal form gets no buffs");c.setScreen(null);item(c,true,false);stage=3;ticks=0;}
            }
            if(stage==3){
                if(ticks==20)c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);
                if(ticks==30)market(c,0);
                if(ticks==65)inspect(c,true);
                if(ticks==75){check(checked&&ok,"Real BUY status effect synced");market(c,1);}
                if(ticks==110){c.setScreen(null);item(c,true,true);stage=4;ticks=0;}
            }
            if(stage==4){
                if(ticks==20)c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);
                if(ticks==30)code(c,"hello");
                if(ticks==65)inspect(c,true);
                if(ticks==75){check(checked&&ok,"Real Code status effect");code(c,"cmd");}
                if(ticks==120){c.setScreen(null);stage=5;ticks=0;}
            }
            if(stage==5){
                if(ticks==20){pressR();check(JiahaoMusicController.soundInstance()==null,"Transformed R waits for server");}
                if(ticks==35){check(JiahaoMusicController.isPlaying(),"R actual sound engine playback");voice=JiahaoMusicController.soundInstance();}
                if(ticks==140){check(JiahaoMusicController.soundInstance()==voice&&JiahaoMusicController.isPlaying(),"R continues after five-second camera");fadeVolume=voice.getVolume();pressR();}
                if(ticks>=143&&ticks<165&&JiahaoMusicController.isFading()){
                    check(JiahaoMusicController.soundInstance()==voice,"Fade uses original voice");
                    check(voice.getVolume()<=fadeVolume,"Manual fade volume decreases");fadeVolume=voice.getVolume();sawFade=true;
                }
                if(ticks==170){check(sawFade&&JiahaoMusicController.soundInstance()==null&&!JiahaoTimeStopClientState.isTimeStopped(c.world),"Early R fades and unfreezes world");pressR();}
                if(ticks==185)check(JiahaoMusicController.soundInstance()==null,"Cooldown R cannot play music");
                if(ticks==330){pressR();stage=6;ticks=0;cycle=0;}
            }
            if(stage==6){
                if(ticks==20){check(JiahaoMusicController.isPlaying(),"Lifecycle R starts");
                    var server=c.getServer();var uuid=c.player.getUuid();int test=cycle;
                    if(test==0)server.execute(()->JiahaoStateManager.setJiahao(server.getPlayerManager().getPlayer(uuid),false));
                    if(test==1)JiahaoMusicController.onResourceReload();
                    if(test==2)server.execute(()->JiahaoTimeStopManager.stopTimeStop(server.getPlayerManager().getPlayer(uuid)));
                }
                if(ticks==45)check(JiahaoMusicController.soundInstance()==null,"Form loss/reload/force cleanup clears voice");
                if(cycle==1&&ticks==65){check(JiahaoMusicController.soundInstance()==null,"Reload suppression survives recurring server sync");var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->JiahaoTimeStopManager.stopTimeStop(server.getPlayerManager().getPlayer(uuid)));}
                if(ticks==235){
                    if(++cycle==3){finish(c,true,null);return;}
                    item(c,true,true);stage=7;ticks=0;
                }
            }
            if(stage==7&&ticks==25){pressR();stage=6;ticks=0;}
        }catch(Throwable e){e.printStackTrace();finish(c,false,e);}
    }
    private void setup(MinecraftClient c){
        c.player.getInventory().selectedSlot=0;
        ClientPlayNetworking.getSender().sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(0));
        var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->{
        var p=server.getPlayerManager().getPlayer(uuid);var world=server.getWorld(World.OVERWORLD);
        JiahaoTimeStopManager.stopTimeStop(p);JiahaoStateManager.setJiahao(p,false);HaoMeterManager.set(p,0);
        world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){world.setBlockState(new BlockPos(x,179,z),Blocks.STONE.getDefaultState());for(int y=180;y<=186;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
        p.teleportTo(new TeleportTarget(world,new Vec3d(.5,180,.5),Vec3d.ZERO,0,0,TeleportTarget.NO_OP));p.changeGameMode(GameMode.CREATIVE);
        p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new ItemStack(ModItems.JIAHAO_HELMET));p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new ItemStack(ModItems.JIAHAO_CHESTPLATE));p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS,new ItemStack(ModItems.JIAHAO_LEGGINGS));p.equipStack(net.minecraft.entity.EquipmentSlot.FEET,new ItemStack(ModItems.JIAHAO_BOOTS));
        p.getInventory().selectedSlot=0;p.clearStatusEffects();p.setHealth(20);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));
    });}
    private void item(MinecraftClient c,boolean form,boolean code){var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->{var p=server.getPlayerManager().getPlayer(uuid);JiahaoStateManager.setJiahao(p,form);HaoMeterManager.set(p,0);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(code?ModItems.JIAHAO_CODE_EDITOR:ModItems.MARKET_VIEWER));});}
    private void inspect(MinecraftClient c,boolean effects){checked=false;var server=c.getServer();var uuid=c.player.getUuid();server.execute(()->{ok=server.getPlayerManager().getPlayer(uuid).getStatusEffects().isEmpty()!=effects;checked=true;});}
    private static void market(MinecraftClient c,int index){var s=(JiahaoMarketScreen)c.currentScreen;s.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast).toList().get(index).onPress();}
    private static void code(MinecraftClient c,String text){var s=(JiahaoCodeScreen)c.currentScreen;var input=(TextFieldWidget)s.children().stream().filter(TextFieldWidget.class::isInstance).findFirst().orElseThrow();input.setText(text);s.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);}
    private static void pressR(){KeyBinding.onKeyPressed(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_R));}
    private void finish(MinecraftClient c,boolean success,Throwable error){done=true;c.setScreen(null);if(c.getServer()!=null)c.getServer().stop(false);c.disconnect();check(JiahaoMusicController.soundInstance()==null,"Disconnect clears music");try{Files.writeString(Path.of("gadget-smoke-result.txt"),success?"PASSED":"FAILED stage="+stage+" ticks="+ticks+" "+error);}catch(Exception e){throw new RuntimeException(e);}c.scheduleStop();}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
