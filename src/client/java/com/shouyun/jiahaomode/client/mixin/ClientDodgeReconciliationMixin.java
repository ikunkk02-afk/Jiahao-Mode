// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.JiahaoDodgeClientController;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientDodgeReconciliationMixin {
    @Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
    private void jiahao$reconciled(CallbackInfo ci) { JiahaoDodgeClientController.reconciled(); }
}
