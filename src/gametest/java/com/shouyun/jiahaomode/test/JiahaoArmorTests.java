// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.JiahaoMode;
import com.shouyun.jiahaomode.armor.*;
import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ConnectedClientData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.item.trim.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.*;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.util.*;

/** Uses real item use, smithing output slots, server lifecycle ticks and existing sync packets. */
public final class JiahaoArmorTests implements FabricGameTest {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Item[] ITEMS = {ModItems.JIAHAO_HELMET, ModItems.JIAHAO_CHESTPLATE, ModItems.JIAHAO_LEGGINGS, ModItems.JIAHAO_BOOTS};
    private static final List<Cleanup> RUNNING = new ArrayList<>();
    static {
        // Embedded channels are not enrolled in ServerNetworkIo; drive the same handler tick as a real connection.
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for(var test:new ArrayList<>(RUNNING))if(test.p.getServer()==server) {
                test.actor.player().networkHandler.tick();test.observer.player().networkHandler.tick();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> RUNNING.removeIf(test -> test.p.getServer() == server && test.tick()));
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_armor")
    public void registrationAllEquipmentCombinationsAndSmithing(TestContext c) {
        JiahaoTimeStopManager.stopTimeStop(c.getWorld());
        var connection = JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),"ArmorRecipes");
        var p=connection.player();
        try {
            String[] names={"helmet","chestplate","leggings","boots"};
            int[] damage={407,592,555,481}, defense={3,8,6,4};
            for(int i=0;i<4;i++) {
                c.assertTrue(Registries.ITEM.get(JiahaoMode.id("jiahao_"+names[i]))==ITEMS[i],"Registered /give item "+names[i]);
                p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4),"give @s jiahao-mode:jiahao_"+names[i]);
                c.assertTrue(p.getInventory().contains(new ItemStack(ITEMS[i])),"Actual give reaches inventory");
                var armor=(ArmorItem)ITEMS[i];
                c.assertTrue(new ItemStack(armor).getMaxDamage()==damage[i]&&armor.getProtection()==defense[i],"Armor stats "+names[i]);
                c.assertTrue(armor.canRepair(new ItemStack(armor),new ItemStack(Items.DIAMOND))&&!armor.canRepair(new ItemStack(armor),new ItemStack(Items.IRON_INGOT)),"Diamond repair only");
                c.assertTrue(armor.getEnchantability()==12&&armor.getToughness()==2.5f,"Enchantability/toughness");
            }
            c.assertTrue(ModArmorMaterials.JIAHAO.value().knockbackResistance()==.05f,"Small material knockback resistance");
            p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_TRANSFORMER));
            c.getWorld().setWeather(0,1234,false,false);
            c.getWorld().setRainGradient(0);c.getWorld().setThunderGradient(0);
            for(int mask=0;mask<15;mask++) {
                for(int i=0;i<4;i++)p.equipStack(SLOTS[i],(mask&(1<<i))==0?ItemStack.EMPTY:new ItemStack(ITEMS[i]));
                c.assertTrue(!JiahaoArmorUtil.isWearingFullJiahaoArmor(p),"Incomplete subset "+mask);
                ModItems.JIAHAO_TRANSFORMER.use(c.getWorld(),p,Hand.MAIN_HAND);
                c.assertTrue(!JiahaoStateManager.isJiahao(p)&&!c.getWorld().isRaining(),"Rejected subset cannot transform or start rain");
            }
            c.assertTrue(connection.hasActionBar("message.jiahao-mode.armor_required"),"Translated missing-set prompt");
            JiahaoTransformationTests.equipJiahaoArmor(p);
            p.equipStack(EquipmentSlot.HEAD,new ItemStack(ModItems.JIAHAO_BOOTS));
            p.equipStack(EquipmentSlot.FEET,new ItemStack(ModItems.JIAHAO_HELMET));
            c.assertTrue(!JiahaoArmorUtil.isWearingFullJiahaoArmor(p),"Wrong-slot exact items rejected");
            JiahaoStateManager.setJiahao(p,true);
            c.assertTrue(!JiahaoStateManager.isJiahao(p),"Direct state API cannot bypass armor requirement");
            c.assertTrue(JiahaoArmorUtil.isJiahaoArmorPiece(new ItemStack(ModItems.JIAHAO_HELMET))&&!JiahaoArmorUtil.isJiahaoArmorPiece(new ItemStack(Items.DIAMOND_HELMET)),"Exact piece API");

            Item[] bases={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
            var protection=p.getRegistryManager().get(RegistryKeys.ENCHANTMENT).entryOf(Enchantments.PROTECTION);
            var trim=new ArmorTrim(p.getRegistryManager().get(RegistryKeys.TRIM_MATERIAL).entryOf(ArmorTrimMaterials.QUARTZ),
                    p.getRegistryManager().get(RegistryKeys.TRIM_PATTERN).entryOf(ArmorTrimPatterns.SENTRY));
            for(int i=0;i<4;i++) {
                var input=new ItemStack(bases[i]);input.setDamage(123);
                input.set(DataComponentTypes.CUSTOM_NAME,Text.literal("Named armor "+i));
                input.addEnchantment(protection,3);input.set(DataComponentTypes.TRIM,trim);
                var smith=new SmithingScreenHandler(1,p.getInventory());
                smith.getSlot(0).setStack(new ItemStack(Items.BLACK_DYE,2));
                smith.getSlot(1).setStack(input);smith.getSlot(2).setStack(new ItemStack(Items.ECHO_SHARD,2));smith.updateResult();
                ItemStack output=smith.getSlot(3).getStack();
                c.assertTrue(output.isOf(ITEMS[i])&&output.getDamage()==123&&output.getMaxDamage()==damage[i],"Smithing item and damage preservation");
                c.assertTrue(output.getName().equals(input.getName())&&EnchantmentHelper.getLevel(protection,output)==3&&trim.equals(output.get(DataComponentTypes.TRIM)),"Smithing name/enchantment/trim preservation");
                c.assertTrue(((ArmorItem)output.getItem()).getProtection()==defense[i],"Upgraded item uses Jiahao attributes");
                smith.getSlot(3).onTakeItem(p,output);
                c.assertTrue(smith.getSlot(0).getStack().getCount()==1&&smith.getSlot(1).getStack().isEmpty()&&smith.getSlot(2).getStack().getCount()==1,"Output consumes one of each ingredient");
            }
            c.complete();
        } finally {p.getServer().getPlayerManager().remove(p);connection.channel().finishAndReleaseAll();}
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_armor_cleanup",tickLimit=900)
    public void everySlotClearsTimeStopDodgeAndQuotes(TestContext c) { RUNNING.add(new Cleanup(c)); }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_armor_saved")
    public void oldSaveWithoutArmorClearsFlagOnLogin(TestContext c) {
        JiahaoTimeStopManager.stopTimeStop(c.getWorld());
        var server=c.getWorld().getServer();var players=server.getPlayerManager();
        var original=JiahaoTransformationTests.connectTestPlayer(server,c.getWorld(),"ArmorOldSave");
        var old=original.player();JiahaoStateManager.setJiahao(old,true);
        for(var slot:SLOTS)old.equipStack(slot,ItemStack.EMPTY);
        players.saveAllPlayerData();
        var data=ConnectedClientData.createDefault(old.getGameProfile(),false);
        var preview=new ServerPlayerEntity(server,c.getWorld(),data.gameProfile(),data.syncedOptions());
        c.assertTrue(players.loadPlayerData(preview).isPresent()&&preview.writeNbt(new NbtCompound()).getCompound("fabric:attachments").getBoolean("jiahao-mode:jiahao_state"),"Disk contains old enabled flag without armor");
        players.remove(old);original.channel().finishAndReleaseAll();
        c.getWorld().setWeather(0,1234,false,false);c.getWorld().setRainGradient(0);
        var restored=new ServerPlayerEntity(server,c.getWorld(),data.gameProfile(),data.syncedOptions());
        var connection=new ClientConnection(NetworkSide.SERVERBOUND);var channel=new EmbeddedChannel(connection);
        try {
            players.onPlayerConnect(connection,restored,data);
            c.assertTrue(!JiahaoStateManager.isJiahao(restored)&&!JiahaoArmorUtil.isWearingFullJiahaoArmor(restored),"Actual login rejects armorless saved form");
            c.assertTrue(!restored.writeNbt(new NbtCompound()).getCompound("fabric:attachments").getBoolean("jiahao-mode:jiahao_state"),"Login clears persistent flag");
            c.assertTrue(!c.getWorld().isRaining(),"Login does not replay transformation rain");
            JiahaoTransformationTests.equipJiahaoArmor(restored);
            c.assertTrue(!JiahaoStateManager.isJiahao(restored),"Equipping after login never restores invalid saved form");
            c.complete();
        }finally {players.remove(restored);channel.finishAndReleaseAll();}
    }

    private static final class Cleanup {
        final TestContext c;
        final JiahaoTransformationTests.TestPlayerConnection actor,observer;
        final ServerPlayerEntity p;
        final long start;
        final Vec3d origin;
        final List<ChunkPos> forced=new ArrayList<>();
        Cleanup(TestContext c) {
            JiahaoTimeStopManager.stopTimeStop(c.getWorld());
            this.c=c;actor=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),"ArmorCleanup");
            observer=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),"ArmorWitness");p=actor.player();
            var pos=c.getAbsolutePos(new BlockPos(1,2,1));origin=Vec3d.ofBottomCenter(pos.up());
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {c.getWorld().setBlockState(pos.add(x,0,z),Blocks.STONE.getDefaultState());for(int y=1;y<6;y++)c.getWorld().setBlockState(pos.add(x,y,z),Blocks.AIR.getDefaultState());}
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {var chunk=new ChunkPos(pos.add(x*16,0,z*16));if(!c.getWorld().getForcedChunks().contains(chunk.toLong())){c.getWorld().setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}}
            p.changeGameMode(GameMode.SURVIVAL);p.setPosition(origin);p.setOnGround(true);p.setVelocity(0,0,0);
            observer.player().setPosition(origin.add(0,0,3));
            for(var connection:List.of(actor,observer))connection.player().networkHandler.onCustomPayload(new CustomPayloadC2SPacket(
                    new RegistrationPayload(RegistrationPayload.REGISTER,List.of(JiahaoTimeStatePayload.ID.id(),JiahaoQuoteSyncPayload.ID.id(),JiahaoDodgeStatePayload.ID.id(),JiahaoPerfectDodgePayload.ID.id()))));
            start=JiahaoTimeStopManager.getServerTick(p.getServer());
        }
        boolean tick() {
            long age=JiahaoTimeStopManager.getServerTick(p.getServer())-start;int cycle=(int)(age/200),step=(int)(age%200);
            try {
                if(cycle>=4){finish();c.runAtTick(c.getTick()+1,c::complete);return true;}
                if(step==1) {
                    acknowledge();JiahaoTransformationTests.equipJiahaoArmor(p);p.setPosition(origin);p.networkHandler.syncWithPlayerPosition();p.setOnGround(true);p.setVelocity(0,0,0);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_TRANSFORMER));
                    ModItems.JIAHAO_TRANSFORMER.use(c.getWorld(),p,Hand.MAIN_HAND);
                    check(JiahaoStateManager.isJiahao(p)&&c.getWorld().isRaining(),"Full set transforms and starts rain");
                    check(lastQuote(observer).equals(JiahaoQuoteRegistry.TRANSFORM_REVENGE),"Witness gets fixed transform");
                }
                // Vanilla applies equipment attributes during the entity tick; allow the forced chunks to start ticking.
                if(step==10) {
                    check(Math.abs(p.getAttributeValue(EntityAttributes.GENERIC_ARMOR)-21)<.001&&Math.abs(p.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)-10)<.001,"Full set effective attributes: armor="+p.getAttributeValue(EntityAttributes.GENERIC_ARMOR)+", toughness="+p.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)+", removed="+p.isRemoved()+", pos="+p.getPos());
                    check(JiahaoTimeStopManager.startTimeStop(p),"Full set starts time stop");
                    check(JiahaoTimeStopManager.isCinematicLocked(p),"Slot removal scenario runs during an actual cinematic");
                    check(lastQuote(observer).equals(JiahaoQuoteRegistry.TIME_STOP_NOTICE),"Witness gets immediate fixed time notice");
                }
                if(step==12) {
                    p.equipStack(SLOTS[cycle],ItemStack.EMPTY);
                    check(!JiahaoStateManager.isJiahao(p)&&!JiahaoTimeStopManager.startTimeStop(p)&&!JiahaoDodgeManager.canDodge(p)&&!JiahaoQuoteManager.manual(p),"Missing armor immediately gates all new abilities");
                }
                if(step==13) {
                    check(!stored()&&!JiahaoTimeStopManager.isTimeStopped(c.getWorld()),"Next server tick clears saved flag and releases world");
                    var quotePackets=observer.channel().outboundMessages().stream().filter(packet->packet instanceof CustomPayloadS2CPacket custom&&custom.payload() instanceof JiahaoQuoteSyncPayload).map(packet->(JiahaoQuoteSyncPayload)((CustomPayloadS2CPacket)packet).payload()).toList();
                    check(!quotePackets.isEmpty()&&quotePackets.getLast().quote()==null,"Observer's latest quote is canceled in every cycle");
                    var timePackets=observer.channel().outboundMessages().stream().filter(packet->packet instanceof CustomPayloadS2CPacket custom&&custom.payload() instanceof JiahaoTimeStatePayload).map(packet->(JiahaoTimeStatePayload)((CustomPayloadS2CPacket)packet).payload()).toList();
                    check(!timePackets.isEmpty()&&!timePackets.getLast().active(),"Observer's latest time sync is inactive in every cycle");
                    check(!JiahaoTimeStopManager.isCinematicLocked(p),"Server cinematic input unlocks");
                }
                if(step==14) {JiahaoTransformationTests.equipJiahaoArmor(p);check(!JiahaoStateManager.isJiahao(p),"Re-equipping never restores form automatically");}
                if(step==20) {
                    acknowledge();p.setPosition(origin);p.networkHandler.syncWithPlayerPosition();p.setOnGround(true);p.setVelocity(0,0,0);JiahaoStateManager.setJiahao(p,true);
                    check(JiahaoDodgeManager.startDodge(p,0),"Dodge starts for equipped form");
                }
                if(step==21)p.equipStack(SLOTS[cycle],ItemStack.EMPTY);
                if(step==22)check(!stored()&&!JiahaoDodgeManager.isDodging(p),"Equipment loss cancels active dodge and saved flag");
                if(step==25) {JiahaoTransformationTests.equipJiahaoArmor(p);check(!JiahaoStateManager.isJiahao(p),"Full armor alone gives no abilities");}
            } catch(Throwable failure) {failure.printStackTrace();finish();c.runAtTick(c.getTick()+1,()->c.throwGameTestException("age="+age+" "+failure));return true;}
            return false;
        }
        boolean stored(){return p.writeNbt(new NbtCompound()).getCompound("fabric:attachments").getBoolean("jiahao-mode:jiahao_state");}
        void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
        void acknowledge(){actor.channel().runPendingTasks();if(!((com.shouyun.jiahaomode.dodge.JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport())return;var packets=actor.channel().outboundMessages().stream().filter(PlayerPositionLookS2CPacket.class::isInstance).map(PlayerPositionLookS2CPacket.class::cast).toList();if(!packets.isEmpty())p.networkHandler.onTeleportConfirm(new TeleportConfirmC2SPacket(packets.getLast().getTeleportId()));}
        void finish(){JiahaoStateManager.setJiahao(p,false);for(var connection:List.of(actor,observer)){JiahaoQuoteManager.clear(connection.player(),true);p.getServer().getPlayerManager().remove(connection.player());connection.channel().finishAndReleaseAll();}for(var chunk:forced)c.getWorld().setChunkForced(chunk.x,chunk.z,false);}
    }
    private static net.minecraft.util.Identifier lastQuote(JiahaoTransformationTests.TestPlayerConnection connection) {
        var quotes=connection.channel().outboundMessages().stream().filter(packet->packet instanceof CustomPayloadS2CPacket custom&&custom.payload() instanceof JiahaoQuoteSyncPayload q&&q.quote()!=null)
                .map(packet->((JiahaoQuoteSyncPayload)((CustomPayloadS2CPacket)packet).payload()).quote()).toList();
        if(quotes.isEmpty())throw new AssertionError("No quote at "+connection.player().getPos()+", channel="+net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(connection.player(),JiahaoQuoteSyncPayload.ID));
        return quotes.getLast();
    }
}
