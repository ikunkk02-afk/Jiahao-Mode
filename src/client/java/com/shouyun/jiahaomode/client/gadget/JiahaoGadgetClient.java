// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.gadget;

import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.JiahaoGadgetQuoteRequestPayload;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.TypedActionResult;

public final class JiahaoGadgetClient {
    private JiahaoGadgetClient(){}
    public static void initialize(){UseItemCallback.EVENT.register((player,world,hand)->{
        var stack=player.getStackInHand(hand);var client=MinecraftClient.getInstance();
        if(world.isClient()&&player==client.player&&stack.isOf(ModItems.MARKET_VIEWER)){
            client.setScreen(new JiahaoMarketScreen());request(JiahaoGadgetQuoteRequestPayload.Kind.MARKET);
            return TypedActionResult.success(stack);
        }
        if(world.isClient()&&player==client.player&&stack.isOf(ModItems.JIAHAO_CODE_EDITOR)){
            client.setScreen(new JiahaoCodeScreen());return TypedActionResult.success(stack);
        }
        return TypedActionResult.pass(stack);
    });}
    public static void request(JiahaoGadgetQuoteRequestPayload.Kind kind){
        if(ClientPlayNetworking.canSend(JiahaoGadgetQuoteRequestPayload.ID))ClientPlayNetworking.send(new JiahaoGadgetQuoteRequestPayload(kind));
    }
}
