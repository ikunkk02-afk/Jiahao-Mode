// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.cinematic.CinematicTimeline;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererCinematicMixin {
    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void jiahao$frame(RenderTickCounter counter, CallbackInfo ci) {
        JiahaoCinematicController.beginFrame(counter.getTickDelta(false));
    }
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void jiahao$fov(Camera camera, float delta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov && JiahaoCinematicController.isCameraActive()) {
            double reduction = 10 * CinematicTimeline.smooth(JiahaoCinematicController.elapsedTicks() / 84)
                    * JiahaoCinematicController.cameraWeight();
            cir.setReturnValue(Math.max(10, cir.getReturnValue() - reduction));
        }
    }
    @Inject(method = {"renderHand", "bobView", "tiltViewWhenHurt"}, at = @At("HEAD"), cancellable = true)
    private void jiahao$stableCamera(CallbackInfo ci) {
        if (JiahaoCinematicController.isCameraActive()) ci.cancel();
    }
}
