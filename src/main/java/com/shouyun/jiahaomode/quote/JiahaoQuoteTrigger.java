// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.quote;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.ActionResult;
public final class JiahaoQuoteTrigger {
 public static void initialize(){
  ServerTickEvents.END_SERVER_TICK.register(JiahaoQuoteManager::tick);
  ServerPlayConnectionEvents.JOIN.register((h,s,server)->JiahaoQuoteManager.track(h.player));
  ServerPlayConnectionEvents.DISCONNECT.register((h,server)->JiahaoQuoteManager.clear(h.player,true));
  ServerLifecycleEvents.SERVER_STOPPED.register(JiahaoQuoteManager::stopServer);
  ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p,a,b)->{JiahaoQuoteManager.clear(p,false);JiahaoQuoteManager.track(p);});
  ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->{JiahaoQuoteManager.clear(old,false);JiahaoQuoteManager.track(p);});
  ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{
   if(e instanceof ServerPlayerEntity p)JiahaoQuoteManager.clear(p,false);
   if(e instanceof Monster&&source.getAttacker() instanceof ServerPlayerEntity p)JiahaoQuoteManager.killed(p);
  });
  AttackEntityCallback.EVENT.register((p,w,hand,e,hit)->{if(p instanceof ServerPlayerEntity server)JiahaoQuoteManager.activity(server);return ActionResult.PASS;});
  AttackBlockCallback.EVENT.register((p,w,hand,pos,face)->{if(p instanceof ServerPlayerEntity server)JiahaoQuoteManager.activity(server);return ActionResult.PASS;});
 }
}
