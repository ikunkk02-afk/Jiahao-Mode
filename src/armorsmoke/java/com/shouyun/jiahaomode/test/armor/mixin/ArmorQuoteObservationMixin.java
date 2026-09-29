// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.armor.mixin;
import com.shouyun.jiahaomode.client.JiahaoQuoteClientState;
import com.shouyun.jiahaomode.network.JiahaoQuoteSyncPayload;
import com.shouyun.jiahaomode.test.armor.ArmorSmokeClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(JiahaoQuoteClientState.class)
public abstract class ArmorQuoteObservationMixin {
 @Inject(method="receive",at=@At("HEAD")) private static void armor$observe(JiahaoQuoteSyncPayload packet,CallbackInfo ci){ArmorSmokeClient.observe(packet);}
}
