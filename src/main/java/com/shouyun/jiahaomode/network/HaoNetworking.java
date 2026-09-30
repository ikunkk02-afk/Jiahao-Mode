// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.hao.HaoMeterManager;
import net.fabricmc.fabric.api.networking.v1.*;
public final class HaoNetworking {
    private HaoNetworking() {}
    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(HaoMeterPayload.ID,HaoMeterPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HaoBurstPayload.ID,HaoBurstPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HaoReadyPayload.ID,HaoReadyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HaoReadyPayload.ID,HaoReadyPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HaoReadyPayload.ID,(p,c)->HaoMeterManager.ready(c.player(),p));
    }
}
