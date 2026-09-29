// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicInput;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class ClientActionsCinematicMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void jiahao$drainInput(CallbackInfo ci) { JiahaoCinematicInput.update((MinecraftClient) (Object) this); }
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void jiahao$attack(CallbackInfoReturnable<Boolean> cir) { if (JiahaoCinematicInput.blocksAttack()) cir.setReturnValue(false); }
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void jiahao$mining(CallbackInfo ci) {
        if (JiahaoCinematicInput.blocksAttack()) {
            MinecraftClient client = (MinecraftClient) (Object) this;
            if (client.interactionManager != null) client.interactionManager.cancelBlockBreaking();
            ci.cancel();
        }
    }
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void jiahao$use(CallbackInfo ci) { if (JiahaoCinematicInput.blocksUse()) ci.cancel(); }
}
