// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.network.JiahaoQuoteRequestPayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
public final class JiahaoQuoteKeyBindings {
 public static void initialize(){
  var key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jiahao-mode.quote",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.jiahao-mode"));
  ClientTickEvents.END_CLIENT_TICK.register(c->{
   boolean pressed=false;while(key.wasPressed())pressed=true;
   if(pressed&&c.player!=null&&c.currentScreen==null&&JiahaoStateManager.isJiahao(c.player)&&ClientPlayNetworking.canSend(JiahaoQuoteRequestPayload.ID))ClientPlayNetworking.send(JiahaoQuoteRequestPayload.INSTANCE);
  });
 }
}
