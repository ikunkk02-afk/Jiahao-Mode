// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicCamera;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraCinematicMixin {
    @Shadow private boolean thirdPerson;
    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method = "update", at = @At("TAIL"))
    private void jiahao$orbit(CallbackInfo ci) {
        if (!JiahaoCinematicController.isCameraActive()) {
            double roll = com.shouyun.jiahaomode.client.JiahaoDodgeClientController.cameraRoll();
            if (roll != 0) ((Camera)(Object)this).getRotation().rotateZ((float)Math.toRadians(roll));
            return;
        }
        var frame = JiahaoCinematicCamera.sample((Camera) (Object) this);
        thirdPerson = true;
        setPos(frame.position()); setRotation(frame.yaw(), frame.pitch());
    }
}
