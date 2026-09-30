// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.hao;

import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.HaoGadgetPayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import com.shouyun.jiahaomode.cinematic.JiahaoCinematicLocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
import static com.shouyun.jiahaomode.network.HaoGadgetPayload.*;

/** Validates a bounded fictional operation, never receives or executes terminal text. */
public final class HaoGadgetManager {
    private static final Map<MinecraftServer,Runtime> SERVERS=new WeakHashMap<>();
    private static final class Runtime { final Map<UUID,Session> sessions=new HashMap<>();final Map<UUID,long[]> cooldowns=new HashMap<>(); }
    private static final class Session {
        final UUID id=UUID.randomUUID();final Identifier dimension;final boolean code;final net.minecraft.util.Hand hand;final net.minecraft.item.ItemStack heldStack;final int selectedSlot;
        long heartbeat,due,expires;UUID operation;Action action;
        Session(ServerPlayerEntity p,boolean code,net.minecraft.util.Hand hand,long tick){this.code=code;this.hand=hand;heldStack=p.getStackInHand(hand);selectedSlot=p.getInventory().selectedSlot;dimension=p.getWorld().getRegistryKey().getValue();heartbeat=tick;expires=tick+600;}
    }
    private HaoGadgetManager() {}
    private static Runtime runtime(MinecraftServer server){if(!server.isOnThread())throw new IllegalStateException("Gadget requires server thread");return SERVERS.computeIfAbsent(server,k->new Runtime());}
    private static long now(ServerPlayerEntity p){return JiahaoTimeStopManager.getServerTick(p.getServer());}
    private static boolean held(ServerPlayerEntity p,boolean code){var item=code?ModItems.JIAHAO_CODE_EDITOR:ModItems.MARKET_VIEWER;return p.getMainHandStack().isOf(item)||p.getOffHandStack().isOf(item);}
    private static boolean valid(ServerPlayerEntity p,Session s){return p.isAlive()&&JiahaoStateManager.isJiahao(p)&&held(p,s.code)
        &&p.getStackInHand(s.hand)==s.heldStack&&(s.hand==net.minecraft.util.Hand.OFF_HAND||p.getInventory().selectedSlot==s.selectedSlot)
        &&s.dimension.equals(p.getWorld().getRegistryKey().getValue())&&!JiahaoCinematicLocks.isLocked(p)&&!JiahaoTimeStopManager.shouldFreeze(p)
        &&now(p)-s.heartbeat<=30&&now(p)<s.expires;}
    public static void initialize(){
        PayloadTypeRegistry.playC2S().register(HaoGadgetPayload.ID,HaoGadgetPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HaoGadgetPayload.ID,HaoGadgetPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HaoGadgetPayload.ID,(packet,context)->receive(context.player(),packet));
        ServerTickEvents.END_SERVER_TICK.register(server->{var r=runtime(server);r.sessions.entrySet().removeIf(e->{var p=server.getPlayerManager().getPlayer(e.getKey());return p==null||!valid(p,e.getValue());});});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->runtime(s).sessions.remove(h.player.getUuid()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p,a,b)->runtime(p.getServer()).sessions.remove(p.getUuid()));
        ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{if(e instanceof ServerPlayerEntity p)runtime(p.getServer()).sessions.remove(p.getUuid());});
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
    }
    public static void open(ServerPlayerEntity p,boolean code,net.minecraft.util.Hand hand){
        if(!p.isAlive()||!JiahaoStateManager.isJiahao(p)||!held(p,code)||JiahaoCinematicLocks.isLocked(p)||!ServerPlayNetworking.canSend(p,HaoGadgetPayload.ID))return;
        var s=new Session(p,code,hand,now(p));runtime(p.getServer()).sessions.put(p.getUuid(),s);
        ServerPlayNetworking.send(p,new HaoGadgetPayload(s.id,NONE,code?Action.CODE:Action.BUY,Phase.OPEN));
    }
    public static void receive(ServerPlayerEntity p,HaoGadgetPayload packet){
        var r=runtime(p.getServer());var s=r.sessions.get(p.getUuid());if(s==null||!s.id.equals(packet.session()))return;
        if(packet.phase()==Phase.CLOSE){r.sessions.remove(p.getUuid());return;}
        if(!valid(p,s)){r.sessions.remove(p.getUuid());return;}
        if(packet.phase()==Phase.PING){s.heartbeat=now(p);s.expires=now(p)+600;return;}
        if(s.code!=(packet.action()==Action.CODE))return;
        if(packet.phase()==Phase.BEGIN){
            if(s.operation!=null)return;
            s.operation=UUID.randomUUID();s.action=packet.action();s.due=now(p)+(s.code?24:20);
            ServerPlayNetworking.send(p,new HaoGadgetPayload(s.id,s.operation,s.action,Phase.GRANT));
        }else if(packet.phase()==Phase.COMMIT&&s.operation!=null&&s.operation.equals(packet.operation())&&s.action==packet.action()&&now(p)>=s.due){
            UUID operation=s.operation;s.operation=null;
            var cooldown=r.cooldowns.computeIfAbsent(p.getUuid(),k->new long[2]);int index=s.code?1:0;
            if(now(p)>=cooldown[index]){cooldown[index]=now(p)+200;HaoMeterManager.gain(p,2);}
            ServerPlayNetworking.send(p,new HaoGadgetPayload(s.id,operation,s.action,Phase.COMPLETE));
        }
    }
}
