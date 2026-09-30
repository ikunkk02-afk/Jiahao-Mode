// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.hao.*;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import java.nio.file.*;
public final class HaoDedicatedServer implements ModInitializer {
    private int ready,age;private boolean begun,done;
    public void onInitialize(){
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->{
            if(!server.isDedicated())return;var p=h.player;var w=p.getServerWorld();
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){w.setBlockState(new BlockPos(x,179,z),Blocks.STONE.getDefaultState());for(int y=180;y<=186;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());}
            w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);w.getGameRules().get(GameRules.NATURAL_REGENERATION).set(false,server);w.setWeather(0,12000,true,false);
            p.changeGameMode(GameMode.SURVIVAL);p.teleport(w,.5,180,p.getName().getString().equals("HaoActor")?.5:5.5,180,0);
            p.equipStack(EquipmentSlot.HEAD,new ItemStack(ModItems.JIAHAO_HELMET));p.equipStack(EquipmentSlot.CHEST,new ItemStack(ModItems.JIAHAO_CHESTPLATE));p.equipStack(EquipmentSlot.LEGS,new ItemStack(ModItems.JIAHAO_LEGGINGS));p.equipStack(EquipmentSlot.FEET,new ItemStack(ModItems.JIAHAO_BOOTS));
            p.setHealth(5);JiahaoStateManager.setJiahao(p,p.getName().getString().equals("HaoActor"));HaoMeterManager.set(p,0);
            server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4),"fillbiome -8 179 -8 8 186 8 minecraft:plains");
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(!server.isDedicated()||done)return;
            var a=server.getPlayerManager().getPlayer("HaoActor");var b=server.getPlayerManager().getPlayer("HaoObserver");if(a==null||b==null)return;
            try{
                if(!begun){if(++ready==200)server.getCommandManager().executeWithPrefix(a.getCommandSource().withLevel(4),"jiahao hao set 99");
                    if(HaoMeterManager.isBursting(a)){begun=true;age=0;}else if(ready>300)throw new AssertionError("Dedicated burst did not start");
                }else if(HaoMeterManager.isBursting(a)){age++;if(!JiahaoTimeStopManager.shouldFreeze(b))throw new AssertionError("Observer freeze");}
                else {if(age<230||HaoMeterManager.data(a).units()!=0)throw new AssertionError("Dedicated completion");done=true;Files.writeString(Path.of("hao-server-result.txt"),"PASSED");}
            }catch(Throwable e){done=true;try{Files.writeString(Path.of("hao-server-result.txt"),"FAILED: "+e);}catch(Exception ignored){}e.printStackTrace();}
        });
    }
}
