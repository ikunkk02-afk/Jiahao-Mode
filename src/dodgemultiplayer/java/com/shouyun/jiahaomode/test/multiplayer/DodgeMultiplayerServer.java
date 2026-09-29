// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test.multiplayer;

import com.shouyun.jiahaomode.dodge.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.*;

public final class DodgeMultiplayerServer implements ModInitializer {
    private JiahaoDodgeState last;
    private boolean passed;
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{
            if(!server.isDedicated())return;
            var p=h.player;var w=p.getServerWorld();
            w.setTimeOfDay(6000);
            w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);
            for(int x=-8;x<=8;x++)for(int z=-10;z<=10;z++) {
                w.setBlockState(new BlockPos(x,180,z),Blocks.OBSIDIAN.getDefaultState());
                for(int y=181;y<=185;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
            }
            boolean actor=p.getGameProfile().getName().equals("DodgeActor");
            p.changeGameMode(GameMode.SURVIVAL);p.getAbilities().flying=false;
            p.teleport(w,.5,181,actor?.5:6.5,actor?0:180,0);
            if(actor)equipArmor(p); JiahaoStateManager.setJiahao(p,actor);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(!server.isDedicated()||passed)return;
            var p=server.getPlayerManager().getPlayer("DodgeActor");if(p==null)return;
            try {
                var state=JiahaoDodgeManager.state(p);
                if(state!=null&&last==null) {
                    last=state;var zombie=new ZombieEntity(p.getServerWorld());zombie.setPosition(p.getPos().add(0,0,1));
                    if(p.damage(p.getDamageSources().mobAttack(zombie),4))throw new AssertionError("Perfect did not cancel multiplayer damage");
                }
                if(last!=null&&state==null) {
                    if(last.steps!=6||p.getPos().distanceTo(last.origin.add(last.direction.multiply(2.8)))>.05||p.getHealth()!=20)
                        throw new AssertionError("Authoritative endpoint/health failed: "+p.getPos()+" steps="+last.steps);
                    Files.writeString(Path.of("dodge-server-result.txt"),"PASSED");passed=true;
                    System.out.println("DODGE MULTIPLAYER SERVER PASSED");
                }
            } catch(Throwable e) {passed=true;try{Files.writeString(Path.of("dodge-server-result.txt"),"FAILED: "+e);}catch(Exception ignored){}e.printStackTrace();}
        });
    }

 private static void equipArmor(net.minecraft.server.network.ServerPlayerEntity p) {
  p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_HELMET));
  p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_CHESTPLATE));
  p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_LEGGINGS));
  p.equipStack(net.minecraft.entity.EquipmentSlot.FEET,new net.minecraft.item.ItemStack(com.shouyun.jiahaomode.item.ModItems.JIAHAO_BOOTS));
 }
}
