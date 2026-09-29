package com.shouyun.jiahaomode.test.mixin;
import com.shouyun.jiahaomode.test.QuoteSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.GameRenderer;
@Mixin(GameRenderer.class)
public abstract class QuoteCaptureMixin {
 @Inject(method="render",at=@At("TAIL"))
 private void quote$capture(CallbackInfo ci){QuoteSmoke.capture();}
}
