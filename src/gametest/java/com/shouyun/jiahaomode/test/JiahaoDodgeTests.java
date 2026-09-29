// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import java.util.*;

/** Real damage/packet/movement pipeline; all state changes stay on the test server thread. */
public final class JiahaoDodgeTests implements FabricGameTest {
    private static final List<Fixture> RUNNING = new ArrayList<>();
    private record RealTimeCheck(Fixture fixture, long start, java.util.function.LongPredicate check) { }
    private static final List<RealTimeCheck> REAL_TIME_CHECKS = new ArrayList<>();
    static {
        // EmbeddedChannel fixtures are not enrolled in ServerNetworkIo. Drive its real handler tick once per tick.
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (var f : new ArrayList<>(RUNNING)) if (f.p.getServer() == server && !f.p.isRemoved()) f.p.networkHandler.tick();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> REAL_TIME_CHECKS.removeIf(task -> {
            if (task.fixture.p.getServer() != server) return false;
            try { return task.check.test(JiahaoTimeStopManager.getServerTick(server)-task.start); }
            catch (Throwable error) {
                JiahaoTimeStopManager.stopTimeStop(task.fixture.p);
                task.fixture.c.runAtTick(task.fixture.c.getTick()+1, () -> task.fixture.c.throwGameTestException(error.toString()));
                return true;
            }
        }));
    }
    private static final class Fixture {
        final TestContext c;
        final JiahaoTransformationTests.TestPlayerConnection actor, observer;
        final ServerPlayerEntity p;
        final Vec3d origin;
        final List<ChunkPos> forcedChunks=new ArrayList<>();
        Fixture(TestContext c, String name) {
            this.c = c;
            actor = JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(), c.getWorld(), name);
            observer = JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(), c.getWorld(), name+"B");
            p = actor.player(); p.changeGameMode(GameMode.SURVIVAL);
            var block = c.getAbsolutePos(new BlockPos(2,2,2)); origin = Vec3d.ofBottomCenter(block.up());
            // Real vanilla player/entity ticks (including login protection) require ticking chunks.
            var first=new ChunkPos(BlockPos.ofFloored(origin.add(-7,0,-7)));
            var last=new ChunkPos(BlockPos.ofFloored(origin.add(11,0,7)));
            for(int x=first.x;x<=last.x;x++)for(int z=first.z;z<=last.z;z++) {
                var chunk=new ChunkPos(x,z);
                if(!c.getWorld().getForcedChunks().contains(chunk.toLong())) {c.getWorld().setChunkForced(x,z,true);forcedChunks.add(chunk);}
            }
            for(int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) {
                c.getWorld().setBlockState(block.add(x,0,z), Blocks.STONE.getDefaultState());
                for(int y=1;y<5;y++) c.getWorld().setBlockState(block.add(x,y,z), Blocks.AIR.getDefaultState());
            }
            reset(); observer.player().setPosition(origin.add(0,0,4));
            var channels = List.of(JiahaoDodgeStatePayload.ID.id(), JiahaoPerfectDodgePayload.ID.id(), JiahaoQuoteSyncPayload.ID.id());
            for(var connection : List.of(actor,observer)) connection.player().networkHandler.onCustomPayload(
                    new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER, channels)));
            RUNNING.add(this);
        }
        void reset() { acknowledge(); p.setPosition(origin); p.networkHandler.syncWithPlayerPosition(); p.setVelocity(0,-.0784,0); p.setOnGround(true); p.setYaw(0); p.setPitch(0); p.getAbilities().flying=false; p.setHealth(20); p.timeUntilRegen=0; }
        void acknowledge() {
            actor.channel().runPendingTasks();
            if (!((com.shouyun.jiahaomode.dodge.JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport()) return;
            var packets = actor.channel().outboundMessages().stream().filter(PlayerPositionLookS2CPacket.class::isInstance)
                    .map(PlayerPositionLookS2CPacket.class::cast).toList();
            if(!packets.isEmpty()) p.networkHandler.onTeleportConfirm(new TeleportConfirmC2SPacket(packets.getLast().getTeleportId()));
        }
        void done() {
            RUNNING.remove(this);
            JiahaoDodgeManager.clear(p,true);
            c.getWorld().getServer().getPlayerManager().remove(p); c.getWorld().getServer().getPlayerManager().remove(observer.player());
            actor.channel().finishAndReleaseAll(); observer.channel().finishAndReleaseAll(); c.complete();
            for(var chunk:forcedChunks)c.getWorld().setChunkForced(chunk.x,chunk.z,false);
        }
        void check(boolean value,String why) { c.assertTrue(value,why); }
    }
    @GameTest(templateName=EMPTY_STRUCTURE, batchId="jiahao_dodge_math")
    public void directionAndClassification(TestContext c) {
        for(float yaw : new float[]{0,90,180,-90,33}) {
            var forward = JiahaoDodgeManager.direction(1,yaw);
            c.assertTrue(forward.add(JiahaoDodgeManager.direction(0,yaw)).length()<1e-8,"No-input backwards follows yaw");
            c.assertTrue(Math.abs(JiahaoDodgeManager.direction(9,yaw).length()-1)<1e-8,"Diagonal normalized");
            c.assertTrue(JiahaoDodgeManager.direction(4,yaw).add(JiahaoDodgeManager.direction(8,yaw)).length()<1e-8,"Left/right opposite");
            c.assertTrue(JiahaoDodgeManager.direction(15,yaw).equals(JiahaoDodgeManager.direction(0,yaw)),"Conflicting inputs cancel");
        }
        double sum=0, previous=2;
        for(int i=0;i<6;i++) { double speed=JiahaoDodgeManager.stepDistance(i); c.assertTrue(speed<=1.2&&speed<previous,"Speed capped and decreasing"); previous=speed; sum+=speed; }
        c.assertTrue(Math.abs(sum-2.8)<1e-8,"Six steps total 2.8");
        try { JiahaoDodgeManager.direction(16,0); throw new AssertionError("Illegal mask accepted"); } catch(IllegalArgumentException expected) { }
        var d=c.getWorld().getDamageSources();
        for(var source:List.of(d.fall(),d.drown(),d.starve(),d.inWall(),d.freeze(),d.outOfWorld(),d.genericKill(),d.onFire(),d.magic(),d.wither(),d.generic()))
            c.assertTrue(!JiahaoDodgeManager.isDodgeableDamage(source),"Environment and periodic damage rejected: "+source);
        c.assertTrue(JiahaoDodgeManager.isDodgeableDamage(d.explosion(null,null)),"Unattributed explosion supported");
        var registry=c.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE);
        var mob=new ZombieEntity(c.getWorld());
        var mod=registry.entryOf(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,net.minecraft.util.Identifier.of("jiahao-dodge-test","mod_attack")));
        var tagged=registry.entryOf(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,net.minecraft.util.Identifier.of("jiahao-dodge-test","tagged_attack")));
        c.assertTrue(JiahaoDodgeManager.isDodgeableDamage(new DamageSource(mod,mob)),"Unrecognized mod melee with actual attacker supported");
        c.assertTrue(JiahaoDodgeManager.isDodgeableDamage(new DamageSource(mod,new TridentEntity(EntityType.TRIDENT,c.getWorld()),mob)),"Mod projectile source supported without vanilla projectile tag");
        c.assertTrue(JiahaoDodgeManager.isDodgeableDamage(d.indirectMagic(new SmallFireballEntity(EntityType.SMALL_FIREBALL,c.getWorld()),mob)),"Magic projectile source supported");
        c.assertTrue(!JiahaoDodgeManager.isDodgeableDamage(new DamageSource(tagged,mob)),"Data-pack undodgeable tag takes priority over melee rule");
        c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE, batchId="jiahao_dodge_motion",tickLimit=220)
    public void motionPacketsCooldownAndWall(TestContext c) {
        var f=new Fixture(c,"DodgeMotion"); var p=f.p;
        f.check(!JiahaoDodgeManager.startDodge(p,0),"Normal form rejected");
        JiahaoStateManager.setJiahao(p,true);
        f.check(!JiahaoDodgeManager.startDodge(p,255),"Out-of-range mask rejected");
        p.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new JiahaoDodgeRequestPayload(8)));
        f.check(JiahaoDodgeManager.isDodging(p),"C2S starts authority action");
        var s=JiahaoDodgeManager.state(p); f.check(s.direction.x<-.99,"Yaw 0 right is negative X");
        p.networkHandler.onPlayerMove(new PlayerMoveC2SPacket.Full(f.origin.x+100,f.origin.y+100,f.origin.z+100,90,12,false));
        f.check(p.getPos().equals(f.origin)&&p.getYaw()==90,"Forged endpoint rejected; looking allowed");
        f.check(s.direction.x<-.99,"Turning does not change action direction");
        for(int i=0;i<100;i++) f.check(!JiahaoDodgeManager.startDodge(p,1),"Repeated requests rejected");
        long start=JiahaoTimeStopManager.getServerTick(p.getServer());
        c.waitAndRun(8,()->{
            f.check(!JiahaoDodgeManager.isDodging(p),"Action finished");
            f.check(Math.abs(p.getX()-(f.origin.x-2.8))<.02,"Vanilla movement retained by network tick");
            f.check(JiahaoDodgeManager.getCooldownTicks(p)>0,"Cooldown starts after action");
            f.check(actorStates(f.actor).stream().anyMatch(v->v.phase()==JiahaoDodgeStatePayload.Phase.END),"Owner receives END");
            f.acknowledge();
        });
        c.waitAndRun(28,()->{
            f.reset(); f.check(JiahaoDodgeManager.getCooldownTicks(p)==0,"Cooldown expires");
            var wall=BlockPos.ofFloored(f.origin.add(-1,0,0));
            c.getWorld().setBlockState(wall,Blocks.STONE.getDefaultState()); c.getWorld().setBlockState(wall.up(),Blocks.STONE.getDefaultState());
            f.check(JiahaoDodgeManager.startDodge(p,8),"Wall dodge starts");
        });
        c.waitAndRun(36,()->{
            f.check(p.getX()>f.origin.x-.3,"Cannot cross full wall");
            f.check(c.getWorld().isSpaceEmpty(p,p.getBoundingBox().contract(1e-6)),"Bounding box stays outside wall");
            f.acknowledge();
        });
        c.waitAndRun(56,()->{
            f.reset(); var door=BlockPos.ofFloored(f.origin.add(-1,0,0));
            c.getWorld().setBlockState(door,Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING,Direction.EAST));
            c.getWorld().setBlockState(door.up(),Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING,Direction.EAST).with(DoorBlock.HALF,DoubleBlockHalf.UPPER));
            f.check(JiahaoDodgeManager.startDodge(p,8),"Closed-door dodge starts");
        });
        c.waitAndRun(64,()->{
            f.check(p.getX()>f.origin.x-1.5&&c.getWorld().isSpaceEmpty(p,p.getBoundingBox().contract(1e-6)),"Closed door prevents traversal"); f.acknowledge();
        });
        c.waitAndRun(84,()->{
            f.reset();
            for(var wall:List.of(BlockPos.ofFloored(f.origin.add(-1,0,0)),BlockPos.ofFloored(f.origin.add(0,0,-1)))) {
                c.getWorld().setBlockState(wall,Blocks.STONE.getDefaultState());c.getWorld().setBlockState(wall.up(),Blocks.STONE.getDefaultState());
            }
            f.check(JiahaoDodgeManager.startDodge(p,10),"Diagonal into corner starts");
        });
        c.waitAndRun(92,()->{
            f.check(p.getX()>f.origin.x-.3&&p.getZ()>f.origin.z-.3&&c.getWorld().isSpaceEmpty(p,p.getBoundingBox().contract(1e-6)),"Corner clips both horizontal components");
            JiahaoStateManager.setJiahao(p,false);f.check(!JiahaoDodgeManager.isDodging(p),"Form exit clears");f.done();
        });
    }
    static List<JiahaoDodgeStatePayload> actorStates(JiahaoTransformationTests.TestPlayerConnection connection) {
        return connection.channel().outboundMessages().stream().filter(packet->packet instanceof CustomPayloadS2CPacket custom && custom.payload() instanceof JiahaoDodgeStatePayload)
                .map(packet->(JiahaoDodgeStatePayload)((CustomPayloadS2CPacket)packet).payload()).toList();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_damage",tickLimit=180)
    public void perfectDamageAndAdministrativeBypass(TestContext c) {
        var f=new Fixture(c,"DodgeDamage"); var p=f.p; JiahaoStateManager.setJiahao(p,true);
        // Vanilla login protection has to expire before checking real damage.
        c.waitAndRun(63,()->{
            f.reset();var mob=new ZombieEntity(c.getWorld());mob.setPosition(p.getPos().add(0,0,1));
            f.check(p.damage(p.getDamageSources().mobAttack(mob),4)&&p.getHealth()==16,"Damage before a late request cannot be retroactively canceled");
        });
        c.waitAndRun(65,()->{
            f.reset(); var mob=new ZombieEntity(c.getWorld()); mob.setPosition(f.origin.add(0,0,1));
            f.check(JiahaoDodgeManager.startDodge(p,4),"Damage dodge starts");
            f.check(!p.damage(p.getDamageSources().mobAttack(mob),4)&&p.getHealth()==20,"Perfect cancels real melee pipeline");
            f.check(JiahaoDodgeManager.state(p).perfectTriggered,"Opportunity consumed");
            f.check(p.damage(p.getDamageSources().mobAttack(mob),4)&&p.getHealth()==16,"Same action second hit applies");
            f.check(JiahaoDodgeManager.getPerfectDodgeCombo(p)==1,"Combo recorded once");
            f.check(!JiahaoDodgeManager.isPerfectWindow(p),"Consumed window unavailable");
        });
        c.waitAndRun(66,()->f.check(f.actor.channel().outboundMessages().stream().anyMatch(q->q instanceof CustomPayloadS2CPacket z && z.payload() instanceof JiahaoPerfectDodgePayload),"Perfect S2C emitted"));
        c.waitAndRun(94,()->{
            f.reset(); f.check(JiahaoDodgeManager.startDodge(p,0),"Next dodge");
            p.getServer().getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4),"damage @s 2 minecraft:arrow");
            f.check(p.getHealth()==18&&!JiahaoDodgeManager.state(p).perfectTriggered,"Typed administrative damage cannot be dodged: health="+p.getHealth());
        });
        c.waitAndRun(123,()->{
            f.reset(); f.check(JiahaoDodgeManager.startDodge(p,0),"Explosion dodge");
            f.check(!p.damage(p.getDamageSources().explosion(null,null),8)&&p.getHealth()==20,"Explosion damage canceled");
            p.timeUntilRegen=0; f.check(p.damage(p.getDamageSources().fall(),3)&&p.getHealth()==17,"Fall still applies within same action");
        });
        c.waitAndRun(133,f::done);
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_air",tickLimit=100)
    public void airborneAndWindowBoundary(TestContext c) {
        var f=new Fixture(c,"DodgeAir"); var p=f.p; JiahaoStateManager.setJiahao(p,true);
        c.waitAndRun(2,()->{
            // The older random-tick fixture fills an entire adjacent chunk above the ground.
            // Clear the full flight corridor so randomized GameTest origins cannot put the player inside it.
            var air=BlockPos.ofFloored(f.origin);
            for(int x=-2;x<=2;x++)for(int z=-5;z<=5;z++)for(int y=0;y<=34;y++)
                c.getWorld().setBlockState(air.add(x,y,z),Blocks.AIR.getDefaultState(),2);
            f.reset(); p.setPosition(f.origin.add(0,30,0)); p.networkHandler.syncWithPlayerPosition(); p.setVelocity(0,-.1,0); p.setOnGround(false);
            f.check(JiahaoDodgeManager.startDodge(p,1),"One air dodge allowed");
        });
        c.waitAndRun(6,()->f.check(!JiahaoDodgeManager.isPerfectWindow(p),"Tick 4 window expired"));
        c.waitAndRun(30,()->{
            f.acknowledge(); f.check(p.getY()<f.origin.y+30,"Gravity preserved");
            f.check(!JiahaoDodgeManager.startDodge(p,1),"Second air dodge forbidden after cooldown");
            f.reset();
        });
        c.waitAndRun(32,()->{
            f.check(JiahaoDodgeManager.startDodge(p,0),"Landing resets air allowance");
            JiahaoStateManager.setJiahao(p,false); JiahaoStateManager.setJiahao(p,true);
            f.check(!JiahaoDodgeManager.startDodge(p,0),"Form toggles cannot bypass cooldown");
        });
        c.waitAndRun(54,()->{
            // GameTest surrounds its platform with structure blocks; explicitly cut a real open cliff beyond it.
            var edge=BlockPos.ofFloored(f.origin);
            for(int x=6;x<=10;x++)for(int z=-1;z<=1;z++)for(int y=-3;y<=4;y++)
                c.getWorld().setBlockState(edge.add(x,y,z),Blocks.AIR.getDefaultState());
            f.reset();p.setPosition(f.origin.add(4.5,0,0));p.networkHandler.syncWithPlayerPosition();p.setSneaking(true);
            f.check(JiahaoDodgeManager.startDodge(p,4),"Grounded dodge at cliff starts even while sneaking");
        });
        c.waitAndRun(62,()->{
            f.check(p.getX()>f.origin.x+6&&p.getY()<f.origin.y&&!p.isOnGround(),"Cliff is crossed with vanilla gravity, no ledge safety or lift: delta="+p.getPos().subtract(f.origin)+", ground="+p.isOnGround());
            p.setSneaking(false);f.done();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_window",tickLimit=140)
    public void exactDamageWindow(TestContext c) {
        var f=new Fixture(c,"DodgeWindow");var p=f.p;JiahaoStateManager.setJiahao(p,true);
        c.waitAndRun(65,()->{
            f.reset();f.check(JiahaoDodgeManager.startDodge(p,0),"Window start");
            f.check(!p.damage(p.getDamageSources().arrow(new ArrowEntity(EntityType.ARROW,c.getWorld()),p),0)
                    &&!JiahaoDodgeManager.state(p).perfectTriggered,"Zero damage does not consume perfect opportunity");
        });
        c.waitAndRun(68,()->{
            var arrow=new ArrowEntity(EntityType.ARROW,c.getWorld());
            f.check(!p.damage(p.getDamageSources().arrow(arrow,null),4)&&p.getHealth()==20,"Tick 3 arrow canceled");
        });
        c.waitAndRun(94,()->{f.reset();f.check(JiahaoDodgeManager.startDodge(p,0),"Late window start");});
        c.waitAndRun(98,()->{
            f.check(p.damage(p.getDamageSources().arrow(new ArrowEntity(EntityType.ARROW,c.getWorld()),null),4)&&p.getHealth()==16,"Tick 4 arrow applies normal damage");
        });
        c.waitAndRun(105,f::done);
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_gates",tickLimit=200)
    public void cinematicTimeStopAndCleanup(TestContext c) {
        var f=new Fixture(c,"DodgeGates");var p=f.p;JiahaoStateManager.setJiahao(p,true);
        p.getAbilities().flying=true;f.check(!JiahaoDodgeManager.canDodge(p),"Flying rejected");p.getAbilities().flying=false;
        p.setSwimming(true);f.check(!JiahaoDodgeManager.canDodge(p),"Swimming rejected");p.setSwimming(false);
        var block=BlockPos.ofFloored(f.origin);c.getWorld().setBlockState(block,Blocks.STONE.getDefaultState());
        f.check(!JiahaoDodgeManager.canDodge(p),"Inside-block state rejected");c.getWorld().setBlockState(block,Blocks.AIR.getDefaultState());
        f.check(JiahaoTimeStopManager.startTimeStop(p),"Cinematic time stop starts");
        f.check(!JiahaoDodgeManager.startDodge(p,0),"Cinematic blocks dodge on server");
        REAL_TIME_CHECKS.add(new RealTimeCheck(f,JiahaoTimeStopManager.getServerTick(p.getServer()),elapsed -> {
          if (elapsed == 104) {
            f.check(JiahaoTimeStopManager.isTimeStopped(c.getWorld())&&!JiahaoTimeStopManager.isCinematicLocked(p),"Only cinematic ended");
            f.acknowledge();
            p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.SHIELD));p.setCurrentHand(Hand.MAIN_HAND);
            f.check(JiahaoDodgeManager.startDodge(p,0)&&!p.isUsingItem(),"Owner dodges in time stop and stops shield: ground="+p.isOnGround()+", freeze="+JiahaoTimeStopManager.shouldFreeze(p)+", pending="+((com.shouyun.jiahaomode.dodge.JiahaoDodgeNetworkAccess)p.networkHandler).jiahao$hasPendingTeleport()+", space="+p.getServerWorld().isSpaceEmpty(p,p.getBoundingBox().contract(1e-6))+", form="+JiahaoStateManager.isJiahao(p)+", cooldown="+JiahaoDodgeManager.getCooldownTicks(p));
          }
          if (elapsed == 113) {f.acknowledge();JiahaoTimeStopManager.stopTimeStop(p);}
          if (elapsed == 133) {
            f.reset();f.check(JiahaoDodgeManager.startDodge(p,0),"Death-cleanup action starts");
            p.damage(p.getDamageSources().genericKill(),Float.MAX_VALUE);
            f.check(!p.isAlive()&&!JiahaoDodgeManager.isDodging(p),"Kill bypasses dodge and clears active action");
            c.runAtTick(c.getTick()+1,f::done); return true;
          }
          return false;
        }));
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_dimension")
    public void dimensionAndDisconnectCleanup(TestContext c) {
        var f=new Fixture(c,"DodgeCleanup");var p=f.p;JiahaoStateManager.setJiahao(p,true);
        c.waitAndRun(2,()->{f.reset();f.check(JiahaoDodgeManager.startDodge(p,0),"Lifecycle action starts");});
        c.waitAndRun(4,()->{
        f.check(JiahaoDodgeManager.state(p).steps>0,"Travel occurred before dimension switch");
        double vertical=p.getVelocity().y;
        p.teleportTo(new TeleportTarget(p.getServer().getWorld(World.NETHER),new Vec3d(0,100,0),Vec3d.ZERO,0,0,TeleportTarget.NO_OP));
        f.check(!JiahaoDodgeManager.isDodging(p),"Changing dimension clears action");
        f.check(p.getVelocity().horizontalLengthSquared()<1e-8&&Math.abs(p.getVelocity().y-vertical)<1e-8,"Dimension switch removes old dodge motion and preserves vanilla vertical velocity: "+p.getVelocity());
        f.check(JiahaoDodgeManager.getCooldownTicks(p)>0,"Dimension switch preserves cooldown");
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(p.networkHandler,p.getServer());
        f.check(JiahaoDodgeManager.getCooldownTicks(p)==0,"Disconnect forgets old history");f.done();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_cooldown",tickLimit=150)
    public void cooldownQuoteAndComboBoundaries(TestContext c) {
        var f=new Fixture(c,"DodgeCooldown");var p=f.p;JiahaoStateManager.setJiahao(p,true);
        c.waitAndRun(2,()->{f.reset();f.check(JiahaoDodgeManager.startDodge(p,0),"Boundary action starts");});
        c.waitAndRun(10,()->{
            f.acknowledge();f.check(JiahaoQuoteManager.perfectDodge(p),"First eligible perfect quote always plays");
            final boolean[] lastTickSeen={false};
            REAL_TIME_CHECKS.add(new RealTimeCheck(f,JiahaoTimeStopManager.getServerTick(p.getServer()),elapsed -> {
                int remaining=JiahaoDodgeManager.getCooldownTicks(p);
                if(remaining==1) {lastTickSeen[0]=true;f.check(!JiahaoDodgeManager.startDodge(p,0),"Last cooldown tick rejects request");}
                if(remaining==0) {f.check(lastTickSeen[0]&&JiahaoDodgeManager.startDodge(p,0),"Exact cooldown expiry accepts request");return true;}
                return false;
            }));
        });
        c.waitAndRun(65,()->{
            f.reset();f.check(!JiahaoQuoteManager.perfectDodge(p),"Expired subtitle does not bypass 80-tick quote cooldown");
            f.check(JiahaoDodgeManager.startDodge(p,0),"First combo action starts");
            var mob=new ZombieEntity(c.getWorld());mob.setPosition(p.getPos().add(0,0,1));boolean applied=p.damage(p.getDamageSources().mobAttack(mob),3);
            f.check(JiahaoDodgeManager.getPerfectDodgeCombo(p)==1,"First perfect counts once: applied="+applied+", health="+p.getHealth()+", consumed="+JiahaoDodgeManager.state(p).perfectTriggered+", window="+JiahaoDodgeManager.isPerfectWindow(p));
        });
        c.waitAndRun(90,()->{
            f.reset();f.check(JiahaoQuoteManager.perfectDodge(p),"Exactly 80 ticks since successful quote allows playback");
        });
        c.waitAndRun(94,()->{
            f.reset();f.check(JiahaoDodgeManager.startDodge(p,0),"Second combo action starts");
            var mob=new ZombieEntity(c.getWorld());mob.setPosition(p.getPos().add(0,0,1));p.damage(p.getDamageSources().mobAttack(mob),3);
            f.check(JiahaoDodgeManager.getPerfectDodgeCombo(p)==2,"Perfect successes within 60 ticks accumulate");
        });
        c.waitAndRun(104,f::done);
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_dodge_resumed_projectile",tickLimit=200)
    public void resumedProjectileUsesNormalPerfectWindow(TestContext c) {
        var f=new Fixture(c,"DodgeResume");var p=f.p;JiahaoStateManager.setJiahao(p,true);
        f.check(JiahaoTimeStopManager.startTimeStop(p),"Pause for real projectile-resume scenario");
        var arrow=new ArrowEntity(EntityType.ARROW,c.getWorld());
        arrow.setPosition(f.origin.add(0,.8,.1));arrow.setVelocity(0,0,-2.2);c.getWorld().spawnEntity(arrow);
        Vec3d frozen=arrow.getPos();
        REAL_TIME_CHECKS.add(new RealTimeCheck(f,JiahaoTimeStopManager.getServerTick(p.getServer()),elapsed -> {
            if(elapsed==104) {
                f.check(arrow.getPos().equals(frozen)&&arrow.getVelocity().z==-2.2,"Paused arrow retains position and velocity");
                f.reset();JiahaoTimeStopManager.stopTimeStop(p);
                f.check(JiahaoDodgeManager.startDodge(p,0),"C starts immediately after R resumes the world");
            }
            if(elapsed==112) {
                f.check(JiahaoDodgeManager.getPerfectDodgeCombo(p)==1&&p.getHealth()==20,"Resumed real arrow collision consumes the normal perfect window once");
                f.check(!arrow.getPos().equals(frozen)||arrow.isRemoved(),"Vanilla projectile simulation resumed");
                c.runAtTick(c.getTick()+1,f::done);return true;
            }
            return false;
        }));
    }
}
