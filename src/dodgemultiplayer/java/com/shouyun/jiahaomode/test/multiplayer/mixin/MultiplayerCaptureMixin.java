package com.shouyun.jiahaomode.test.multiplayer.mixin;
import com.shouyun.jiahaomode.test.multiplayer.DodgeMultiplayerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.GameRenderer;
@Mixin(GameRenderer.class)
public abstract class MultiplayerCaptureMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void dodge$capture(CallbackInfo ci) { DodgeMultiplayerClient.capture(); }
}
