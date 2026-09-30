// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.haomixin;
import com.shouyun.jiahaomode.test.HaoCapture;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class CaptureMixin {
    @Inject(method="render",at=@At("TAIL"))private void capture(CallbackInfo ci){HaoCapture.capture();}
}
