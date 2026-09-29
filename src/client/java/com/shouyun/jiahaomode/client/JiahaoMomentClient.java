// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.network.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
public final class JiahaoMomentClient {
    private static boolean preferenceSent;
    public static void initialize() {
        JiahaoClientConfig.load();
        ClientPlayConnectionEvents.JOIN.register((h,s,c)->preferenceSent=false);
        // Channel registration and the join event may arrive in either order. Send once ready.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(!preferenceSent&&c.world!=null&&ClientPlayNetworking.canSend(JiahaoMomentPreferencePayload.ID)){
                ClientPlayNetworking.send(new JiahaoMomentPreferencePayload(JiahaoClientConfig.enableRandomJiahaoMoments));preferenceSent=true;
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(JiahaoMomentProposalPayload.ID,(p,c)->{
            var client=MinecraftClient.getInstance();
            boolean ready=JiahaoClientConfig.enableRandomJiahaoMoments&&client.world!=null&&client.player!=null
                &&client.world.getRegistryKey().getValue().equals(p.dimension())&&client.currentScreen==null
                &&client.player.isAlive()&&!JiahaoCinematicController.isCameraActive()
                &&!JiahaoDodgeClientController.locksMovement()&&client.getCameraEntity()==client.player;
            ClientPlayNetworking.send(new JiahaoMomentResponsePayload(p.session(),ready));
        });
        ClientPlayNetworking.registerGlobalReceiver(JiahaoMomentStatePayload.ID,(p,c)->JiahaoCinematicController.onMomentSync(p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{preferenceSent=false;JiahaoCinematicController.cleanup();});
    }
    private JiahaoMomentClient() {}
}
