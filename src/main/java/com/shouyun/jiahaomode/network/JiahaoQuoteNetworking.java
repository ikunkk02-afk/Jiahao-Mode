// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.network;
import com.shouyun.jiahaomode.quote.*;
import net.fabricmc.fabric.api.networking.v1.*;
public final class JiahaoQuoteNetworking {
 public static void initialize(){
  PayloadTypeRegistry.playC2S().register(JiahaoQuoteRequestPayload.ID,JiahaoQuoteRequestPayload.CODEC);
  PayloadTypeRegistry.playC2S().register(JiahaoGadgetQuoteRequestPayload.ID,JiahaoGadgetQuoteRequestPayload.CODEC);
  PayloadTypeRegistry.playC2S().register(JiahaoQuotePlaybackFailedPayload.ID,JiahaoQuotePlaybackFailedPayload.CODEC);
  PayloadTypeRegistry.playS2C().register(JiahaoQuoteSyncPayload.ID,JiahaoQuoteSyncPayload.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(JiahaoQuoteRequestPayload.ID,(p,c)->JiahaoQuoteManager.manual(c.player()));
  ServerPlayNetworking.registerGlobalReceiver(JiahaoGadgetQuoteRequestPayload.ID,(p,c)->JiahaoQuoteManager.gadget(c.player(),p.kind()));
  ServerPlayNetworking.registerGlobalReceiver(JiahaoQuotePlaybackFailedPayload.ID,(p,c)->JiahaoQuoteManager.playbackFailed(c.player(),p.session()));
 }
}
