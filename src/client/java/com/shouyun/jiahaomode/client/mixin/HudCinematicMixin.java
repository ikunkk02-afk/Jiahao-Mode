// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class HudCinematicMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void jiahao$crosshair(CallbackInfo ci) { if (JiahaoCinematicController.isCameraActive()) ci.cancel(); }
    @Inject(method = "render", at = @At("HEAD"))
    private void jiahao$bars(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
        int alpha = (int) (255 * JiahaoCinematicController.barOpacity());
        if (alpha <= 0) return;
        int width = context.getScaledWindowWidth(), height = context.getScaledWindowHeight();
        int bar = Math.round(height * .06F);
        context.fill(0, 0, width, bar, alpha << 24);
        context.fill(0, height - bar, width, height, alpha << 24);
    }
}
