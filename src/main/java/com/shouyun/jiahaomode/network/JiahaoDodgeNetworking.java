// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;

import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class JiahaoDodgeNetworking {
    private JiahaoDodgeNetworking() { }
    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(JiahaoDodgeRequestPayload.ID, JiahaoDodgeRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JiahaoDodgeStatePayload.ID, JiahaoDodgeStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JiahaoPerfectDodgePayload.ID, JiahaoPerfectDodgePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JiahaoDodgeRequestPayload.ID,
                (p, c) -> JiahaoDodgeManager.startDodge(c.player(), p.inputMask()));
    }
}
