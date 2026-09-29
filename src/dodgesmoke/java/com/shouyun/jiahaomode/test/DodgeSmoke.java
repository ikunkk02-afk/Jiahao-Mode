// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.client.*;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.dodge.JiahaoDodgeManager;
import com.shouyun.jiahaomode.quote.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.item.*;
import java.nio.file.*;
import java.util.*;

/** Actual integrated world, vanilla creatures, key binding -> C2S -> server -> S2C -> renderer. */
public final class DodgeSmoke implements ClientModInitializer {
    private int stage, ticks, total, fov;
    private Perspective perspective;
    private boolean motionSeen, poseSeen, cameraSeen, rollSeen, quoteSeen;
    private volatile String failure;
    private volatile boolean ready, serverPass, scenarioStarted;
    private int scenario;
    private long serverStart;
    private PigEntity explosionWitness;
    private BlockPos explosionBlock;
    private Vec3d origin = new Vec3d(.5,181,.5);
    private static String pendingShot;
    private final Set<String> shots = new HashSet<>();
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!scenarioStarted || server.getPlayerManager().getPlayerList().isEmpty()) return;
            try {
                var p=server.getPlayerManager().getPlayerList().getFirst();
                var state=JiahaoDodgeManager.state(p);
                if(state!=null&&serverStart<0) {
                    serverStart=state.startTick;
                    if (scenario >= 2) {
                        explosionWitness = new PigEntity(EntityType.PIG,p.getServerWorld());
                        explosionWitness.setPosition(p.getPos().add(-2,0,1.5)); explosionWitness.setAiDisabled(true);
                        p.getServerWorld().spawnEntity(explosionWitness);
                        explosionBlock = BlockPos.ofFloored(p.getPos().add(0,1,1.5));
                        p.getServerWorld().setBlockState(explosionBlock,Blocks.WHITE_WOOL.getDefaultState());
                    }
                    if(scenario==0) {
                        var zombie=new ZombieEntity(p.getServerWorld()); zombie.setPosition(p.getPos().add(1.3,0,0)); zombie.setAiDisabled(true);
                        p.getServerWorld().spawnEntity(zombie); check(!zombie.tryAttack(p),"Zombie's actual attack canceled");
                    } else if(scenario==1) {
                        var skeleton=new SkeletonEntity(EntityType.SKELETON,p.getServerWorld()); skeleton.setPosition(p.getPos().add(0,0,1)); skeleton.setAiDisabled(true); p.getServerWorld().spawnEntity(skeleton);
                        var arrow=new ArrowEntity(p.getServerWorld(), skeleton, new ItemStack(Items.ARROW), null);
                        arrow.setPosition(p.getPos().add(0,.8,.1)); arrow.setVelocity(0,0,-.2); p.getServerWorld().spawnEntity(arrow);
                    } else if(scenario==2) {
                        var tnt=new TntEntity(p.getServerWorld(),p.getX(),p.getY(),p.getZ()+1.5,p);tnt.setFuse(0);p.getServerWorld().spawnEntity(tnt);
                    } else {
                        var creeper=new CreeperEntity(EntityType.CREEPER,p.getServerWorld());
                        var nbt=new net.minecraft.nbt.NbtCompound();nbt.putShort("Fuse",(short)1);creeper.readNbt(nbt);
                        creeper.setPosition(p.getPos().add(0,0,1.5));creeper.setAiDisabled(true);creeper.ignite();p.getServerWorld().spawnEntity(creeper);
                    }
                }
                if(serverStart>=0 && com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager.getServerTick(server)-serverStart>=12) {
                    check(JiahaoDodgeManager.getPerfectDodgeCombo(p)>0,"Scenario "+scenario+" perfect triggered");
                    check(p.getHealth()==20,"Scenario "+scenario+" canceled health loss");
                    if (scenario >= 2) {
                        check(!explosionWitness.isAlive() || explosionWitness.getHealth()<explosionWitness.getMaxHealth(),"Explosion still damages another entity");
                        check(p.getServerWorld().getBlockState(explosionBlock).isAir(),"Explosion still destroys nearby blocks");
                        check(p.getPos().distanceTo(origin.add(0,0,-2.8))>.05,"Explosion knockback remains in addition to dodge motion");
                    }
                    serverPass=true; scenarioStarted=false;
                }
            } catch(Throwable e) { failure=e.toString(); scenarioStarted=false; }
        });
        HudRenderCallback.EVENT.register((draw,counter)->{
            var c=MinecraftClient.getInstance(); if(c.player==null) return;
            var v=JiahaoDodgeClientController.local();
            if(v!=null) {
                if(v.weight()>.85) { poseSeen=true; shot("dodge-pose-"+scenario+".png"); }
                if(v.perfect&&JiahaoDodgeClientController.fovOffset()>3.5) { cameraSeen=true; shot("dodge-perfect-"+scenario+".png"); }
                if(v.perfect&&scenario==0&&JiahaoDodgeClientController.cameraRoll() < -3.5) rollSeen=true;
                if(v.perfect&&scenario>0) check(JiahaoDodgeClientController.cameraRoll()==0,"Pure backward perfect has no camera roll");
            }
            var q=JiahaoQuoteClientState.local(); if(q!=null&&q.quote.category()==JiahaoQuoteCategory.PERFECT_DODGE) { quoteSeen=true; if(q.age()>.2) shot("dodge-quote.png"); }
        });
    }
    private void tick(MinecraftClient c) {
        try {
            if(failure!=null) throw new AssertionError(failure);
            check(++total<2000,"Client watchdog"); if(c.isPaused()||c.getOverlay()!=null) return; ticks++;
            if(stage==0) {
                if(c.player==null||c.world==null||c.getServer()==null||ticks<60) return;
                fov=c.options.getFov().getValue(); perspective=c.options.getPerspective(); c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                c.getServer().execute(()->{
                    try {
                        var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid()); var w=c.getServer().getWorld(World.OVERWORLD);
                        p.teleportTo(new TeleportTarget(w,origin,Vec3d.ZERO,0,0,TeleportTarget.NO_OP)); w.setTimeOfDay(6000);
                        for(int x=-12;x<=12;x++) for(int z=-12;z<=12;z++) {
                            w.setBlockState(new BlockPos(x,180,z),Blocks.OBSIDIAN.getDefaultState());
                            for(int y=181;y<187;y++) w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                        }
                        p.changeGameMode(GameMode.SURVIVAL); p.teleport(w,origin.x,origin.y,origin.z,0,0); p.setHealth(20);
                        p.getAbilities().flying=false; JiahaoStateManager.setJiahao(p,false); ready=true;
                    } catch(Throwable e) { failure=e.toString(); }
                }); stage=1; ticks=0;
            } else if(stage==1&&ready&&ticks>30) {
                pressC(c); stage=2; ticks=0;
            } else if(stage==2&&ticks>15) {
                check(!JiahaoDodgeClientController.locksMovement(),"Normal form cannot dodge");
                c.getServer().execute(()->JiahaoStateManager.setJiahao(c.getServer().getPlayerManager().getPlayer(c.player.getUuid()),true)); stage=3;ticks=0;
            } else if(stage==3&&ticks>35) {
                c.options.leftKey.setPressed(true); pressC(c); stage=4;ticks=0;
            } else if(stage==4) {
                if(JiahaoDodgeClientController.locksMovement()) motionSeen=true;
                if(ticks>12) {
                    c.options.leftKey.setPressed(false); check(motionSeen,"C binding started actual networked movement");
                    check(c.player.getX()>origin.x+2.5,"Yaw-based left move visible"); stage=5;ticks=0;
                }
            } else if(stage==5&&ticks>100) {
                scenarioStarted=false;serverPass=false;serverStart=-1;
                c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());p.teleport(p.getServerWorld(),origin.x,origin.y,origin.z,0,0);p.setHealth(20);p.timeUntilRegen=0;}); stage=6;ticks=0;
            } else if(stage==6&&ticks>12) {
                scenarioStarted=true; if(scenario==0)c.options.leftKey.setPressed(true); pressC(c); stage=7;ticks=0;
            } else if(stage==7) {
              if(ticks>8)c.options.leftKey.setPressed(false);
              if(serverPass&&ticks>22) {
                check(c.options.getFov().getValue()==fov,"Options FOV never changed");
                check(JiahaoDodgeClientController.fovOffset()<.001&&Math.abs(JiahaoDodgeClientController.cameraRoll())<.001,"Camera effect restored");
                if(++scenario<4) { stage=5;ticks=0; } else {
                    check(poseSeen&&cameraSeen&&rollSeen&&quoteSeen,"Pose, FOV, left roll and reused quote seen in render frames");
                    finish(c,true);
                }
              }
            }
        } catch(Throwable e) { failure=e.toString(); e.printStackTrace(); finish(c,false); }
    }
    private void pressC(MinecraftClient c) {
        KeyBinding.onKeyPressed(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(JiahaoDodgeKeyBindings.binding));
    }
    private void shot(String name) { if(shots.add(name)) pendingShot=name; }
    public static void capture() { if(pendingShot!=null) { var c=MinecraftClient.getInstance(); net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,pendingShot,c.getFramebuffer(),t->{}); pendingShot=null; } }
    private void finish(MinecraftClient c,boolean passed) {
        if(stage==9)return;stage=9;c.options.leftKey.setPressed(false); if(perspective!=null)c.options.setPerspective(perspective);
        if(c.getServer()!=null)c.getServer().stop(false);c.disconnect();
        try { Files.writeString(Path.of("dodge-smoke-result.txt"),passed?"PASSED":"FAILED: "+failure); } catch(Exception e) {throw new RuntimeException(e);}
        c.scheduleStop();
    }
    private static void check(boolean condition,String why) {if(!condition)throw new AssertionError(why);}
}
