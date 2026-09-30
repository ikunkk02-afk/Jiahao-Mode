// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.hao.HaoBurstTimeline;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public final class HaoClient {
    private static int target;private static double displayed;private static boolean pending,bursting;
    private static long frame;private static ClientWorld world;
    private HaoClient() {}
    public static void initialize() {
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
            public net.minecraft.util.Identifier getFabricId(){return com.shouyun.jiahaomode.JiahaoMode.id("hao_music_cleanup");}
            public void reload(net.minecraft.resource.ResourceManager resources){JiahaoMusicController.onResourceReload();}
        });
        ClientPlayNetworking.registerGlobalReceiver(HaoMeterPayload.ID,(p,c)->{useWorld(c.client().world);target=Math.max(0,Math.min(10000,p.units()));pending=p.pending();bursting=p.bursting();});
        ClientPlayNetworking.registerGlobalReceiver(HaoBurstPayload.ID,(p,c)->{JiahaoCinematicController.onBurstSync(p);JiahaoMusicController.onBurstState(p);});
        ClientPlayNetworking.registerGlobalReceiver(HaoReadyPayload.ID,(p,c)->{
            var client=c.client();boolean ready=client.world!=null&&client.player!=null&&client.player.isAlive()
                &&client.world.getRegistryKey().getValue().equals(p.dimension())&&client.currentScreen==null
                &&client.getOverlay()==null&&client.getCameraEntity()==client.player&&!JiahaoCinematicController.isCameraActive()&&!JiahaoDodgeClientController.locksMovement();
            ClientPlayNetworking.send(new HaoReadyPayload(p.token(),p.dimension(),ready));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{reset();JiahaoMusicController.stopJiahaoMarch();});
        ClientTickEvents.END_CLIENT_TICK.register(c->{useWorld(c.world);JiahaoMusicController.update(c);});
        HudRenderCallback.EVENT.register((draw,counter)->{
            var c=MinecraftClient.getInstance();useWorld(c.world);
            if(c.player==null||c.options.hudHidden||!JiahaoStateManager.isJiahao(c.player)||c.currentScreen!=null)return;
            long current=System.nanoTime();double dt=frame==0?0:Math.min(.1,(current-frame)/1e9);frame=current;
            displayed+=(target-displayed)*(1-Math.exp(-8*dt));
            int w=Math.min(182,draw.getScaledWindowWidth()-24),x=(draw.getScaledWindowWidth()-w)/2,y=draw.getScaledWindowHeight()-86;
            int percent=target==10000?100:(int)Math.floor(displayed/100);
            double pulse=target>=9000?.9+.1*Math.sin(current/1e9*2):1;
            int alpha=(int)(220*pulse),white=(alpha<<24)|0xffffff;
            var label=Text.translatable(pending||bursting?"hud.jiahao-mode.hao.max":"hud.jiahao-mode.hao",percent);
            draw.drawText(c.textRenderer,label,x,y-11,white,true);
            draw.fill(x,y,x+w,y+4,0xD0202020);draw.fill(x,y,x+(int)(w*displayed/10000),y+4,white);
            if(target>=8000&&!pending&&!bursting)draw.drawText(c.textRenderer,Text.translatable("hud.jiahao-mode.hao.gathering"),x,y-22,0xFFB8B8B8,true);
        });
    }
    private static void useWorld(ClientWorld next) { if(world!=next){reset();world=next;} }
    private static void reset() { target=0;displayed=0;pending=bursting=false;frame=0;world=null; }
}
