package com.shouyun.jiahaomode.test.phase7.mixin;
import com.shouyun.jiahaomode.test.phase7.Phase7Client;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;import net.minecraft.client.render.GameRenderer;
@Mixin(GameRenderer.class) public abstract class CaptureMixin {@Inject(method="render",at=@At("TAIL"))private void capture(CallbackInfo ci){Phase7Client.capture();}}
