// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Vanilla keeps one binding per key in a HashMap. C also belongs to save-toolbar by default. */
@Mixin(KeyBinding.class)
public abstract class KeyBindingDodgeMixin implements JiahaoDodgeKeyAccess {
    @Shadow @Final private static Map<InputUtil.Key, KeyBinding> KEY_TO_BINDINGS;
    @Shadow private int timesPressed;
    public void jiahao$pressDodgeKey() { timesPressed++; }
    @Inject(method="onKeyPressed",at=@At("TAIL"))
    private static void jiahao$sharedKey(InputUtil.Key key,CallbackInfo ci) {
        var dodge=JiahaoDodgeKeyBindings.binding;
        if(dodge!=null&&KEY_TO_BINDINGS.get(key)!=dodge&&KeyBindingHelper.getBoundKeyOf(dodge).equals(key))
            ((JiahaoDodgeKeyAccess)dodge).jiahao$pressDodgeKey();
    }
}
