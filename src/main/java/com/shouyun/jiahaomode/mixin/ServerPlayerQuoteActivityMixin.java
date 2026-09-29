// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;
import com.shouyun.jiahaomode.quote.JiahaoQuoteManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Include missed swings in the idle timer; tail executes after main-thread dispatch. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayerQuoteActivityMixin {
 @Shadow public ServerPlayerEntity player;
 @Inject(method="onHandSwing",at=@At("TAIL"))
 private void jiahao$quoteActivity(CallbackInfo ci){JiahaoQuoteManager.activity(player);}
}
