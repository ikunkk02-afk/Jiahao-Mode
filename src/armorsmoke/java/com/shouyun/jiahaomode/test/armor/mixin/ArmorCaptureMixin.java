// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.armor.mixin;
import com.shouyun.jiahaomode.test.armor.ArmorSmokeClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class ArmorCaptureMixin {
 @Inject(method="render",at=@At("TAIL")) private void armor$capture(CallbackInfo ci){ArmorSmokeClient.capture();}
}
