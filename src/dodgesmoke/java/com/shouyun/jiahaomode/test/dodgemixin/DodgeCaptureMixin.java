package com.shouyun.jiahaomode.test.dodgemixin;
import com.shouyun.jiahaomode.test.DodgeSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.GameRenderer;
@Mixin(GameRenderer.class)
public abstract class DodgeCaptureMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void dodge$capture(CallbackInfo ci) { DodgeSmoke.capture(); }
}
