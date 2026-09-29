// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shouyun.jiahaomode.quote.JiahaoQuoteManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerQuoteDamageMixin {
 @WrapMethod(method="damage")
 private boolean jiahao$actualDamage(DamageSource source,float amount,Operation<Boolean> original){
  var p=(ServerPlayerEntity)(Object)this;
  float before=p.getHealth()+p.getAbsorptionAmount();
  boolean result=original.call(source,amount);
  float actual=before-p.getHealth()-p.getAbsorptionAmount();
  if(result&&actual>0)JiahaoQuoteManager.damage(p,actual);
  return result;
 }
}
