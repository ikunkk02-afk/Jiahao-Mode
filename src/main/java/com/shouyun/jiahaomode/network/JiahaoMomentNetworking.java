// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.moment.JiahaoMomentManager;
import net.fabricmc.fabric.api.networking.v1.*;
public final class JiahaoMomentNetworking {
    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(JiahaoMomentPreferencePayload.ID,JiahaoMomentPreferencePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(JiahaoMomentResponsePayload.ID,JiahaoMomentResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JiahaoMomentProposalPayload.ID,JiahaoMomentProposalPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JiahaoMomentStatePayload.ID,JiahaoMomentStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JiahaoMomentPreferencePayload.ID,(p,c)->JiahaoMomentManager.preference(c.player(),p.enabled()));
        ServerPlayNetworking.registerGlobalReceiver(JiahaoMomentResponsePayload.ID,(p,c)->JiahaoMomentManager.respond(c.player(),p));
    }
    private JiahaoMomentNetworking() {}
}
