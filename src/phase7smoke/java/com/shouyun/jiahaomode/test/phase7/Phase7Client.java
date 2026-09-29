package com.shouyun.jiahaomode.test.phase7;
import com.mojang.authlib.GameProfile;
import com.shouyun.jiahaomode.JiahaoMode;import com.shouyun.jiahaomode.client.*;import com.shouyun.jiahaomode.client.cinematic.*;import com.shouyun.jiahaomode.client.gadget.*;
import com.shouyun.jiahaomode.cinematic.*;import com.shouyun.jiahaomode.network.*;import com.shouyun.jiahaomode.quote.*;import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.client.visual.JiahaoTimeStopClientState;
import net.fabricmc.api.ClientModInitializer;import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;import net.minecraft.client.network.OtherClientPlayerEntity;import net.minecraft.client.gui.widget.*;import net.minecraft.client.model.*;import net.minecraft.client.render.entity.model.*;import net.minecraft.client.texture.NativeImage;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.util.Hand;import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;import java.nio.file.*;import java.util.*;

/** Real clients, vanilla screen events and renderer; test-only and absent from release JAR. */
public final class Phase7Client implements ClientModInitializer {
 private int stage,ticks,total,command;private UUID actorId,observerId;private boolean acked,done,seen,quoted,timeSeen;private long worldTime;private Vec3d position;
 private static String pendingShot;private static final Set<String> SHOTS=new HashSet<>();
 public void onInitializeClient(){ClientPlayNetworking.registerGlobalReceiver(Phase7Stage.ID,(p,c)->{stage=p.stage();actorId=p.actor();observerId=p.observer();ticks=command=0;acked=seen=quoted=timeSeen=false;});ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 private void tick(MinecraftClient c){
  if(done)return;
  try{
   check(++total<7600,"Watchdog stage "+stage);if(c.player==null||c.world==null||c.getOverlay()!=null||stage==0)return;
   c.options.pauseOnLostFocus=false;c.options.getMaxFps().setValue(90);ticks++;
   boolean actor=c.player.getUuid().equals(actorId);var a=c.world.getPlayerByUuid(actorId);var b=c.world.getPlayerByUuid(observerId);if(a==null||b==null&&stage<9)return;
   if(acked)return;
   switch(stage){
    case 1 -> market(c,actor);
    case 2 -> code(c,actor);
    case 3 -> {
     if(JiahaoCinematicController.isPoseActive(a)){
      if(!seen){seen=true;worldTime=c.world.getTimeOfDay();position=a.getPos();}
      check(!JiahaoTimeStopClientState.isTimeStopped(c.world),"Random world not frozen");check(a.squaredDistanceTo(position)<.01,"Actor stops in place");
      check(actor==JiahaoCinematicController.isCameraActive()&&actor==JiahaoCinematicController.locksInput(),"Observer never gets actor camera or input lock");
      check(!JiahaoCinematicController.canUseCamera(CinematicType.PERFECT_DODGE)||!actor,"Perfect effect yields to random");
      var q=JiahaoQuoteClientState.active().get(actorId);if(q!=null&&q.quote.category()==JiahaoQuoteCategory.RANDOM_MOMENT&&q.started!=0){quoted=true;if(q.age()>.35)shot(actor?"actor-natural-moment.png":"observer-natural-pose.png");}
     }else if(seen){check(quoted,"Natural moment quote appears");check(c.world.getTimeOfDay()>worldTime+40,"World continues ticking");check(!JiahaoCinematicController.locksInput()&&!JiahaoCinematicController.isCameraActive(),"Moment restores control");ack();}
    }
    case 4 -> {
     if(ticks==5&&actor)c.setScreen(new JiahaoMarketScreen());
     check(!JiahaoCinematicController.isPoseActive(a),"GUI declines proposal before any lock");
     if(ticks==55&&actor){c.setScreen(null);JiahaoClientConfig.enableRandomJiahaoMoments=false;ClientPlayNetworking.send(new JiahaoMomentPreferencePayload(false));}
     if(ticks>=80)ack();
    }
    case 5 -> {
     if(ticks==1){JiahaoClientConfig.enableRandomJiahaoMoments=true;ClientPlayNetworking.send(new JiahaoMomentPreferencePayload(true));}
     if(JiahaoCinematicController.isPoseActive(a)&&JiahaoCinematicController.isPoseActive(b)){
      seen=true;check(JiahaoCinematicController.locksInput()&&JiahaoCinematicController.cameraType()==CinematicType.RANDOM_HAO_MOMENT,"Each participant owns one camera");
      var q=JiahaoQuoteClientState.local();if(q!=null&&q.quote.category()==JiahaoQuoteCategory.RANDOM_MOMENT){quoted=true;if(q.age()>.35)shot(actor?"actor-simultaneous.png":"observer-simultaneous.png");}
     }else if(seen&&!JiahaoCinematicController.isPoseActive(a)&&!JiahaoCinematicController.isPoseActive(b)){check(quoted,"Each local participant gets its own quote");check(!JiahaoCinematicController.locksInput(),"Both controls released");ack();}
    }
    case 6 -> {
     if(JiahaoCinematicController.isPoseActive(a)&&!JiahaoTimeStopClientState.isTimeStopped(c.world))seen=true;
     if(JiahaoTimeStopClientState.isTimeStopped(c.world)){
      timeSeen=true;check(seen,"Time stop follows random performance");
      if(JiahaoCinematicController.isPoseActive(a)){
       if(actor){check(JiahaoCinematicController.cameraType()==CinematicType.TIME_STOP,"Highest priority owns camera");check(!JiahaoCinematicController.canUseCamera(CinematicType.RANDOM_HAO_MOMENT)&&!JiahaoCinematicController.canUseCamera(CinematicType.PERFECT_DODGE),"Lower camera effects denied");}
       else check(!JiahaoCinematicController.isCameraActive()&&!JiahaoCinematicController.locksInput(),"Observer unchanged during time stop");
       if(JiahaoCinematicController.poseElapsed(a)>=20)shot(actor?"actor-time-stop-priority.png":"observer-time-stop-pose.png");
      }else check(!JiahaoCinematicController.locksInput(),"Free time stop releases controls");
     }else if(timeSeen){check(!JiahaoCinematicController.isCameraActive()&&!JiahaoCinematicController.locksInput(),"End clears all camera state");ack();}
    }
    case 7 -> {
     if(JiahaoCinematicController.isPoseActive(a))seen=true;
     else if(seen&&ticks>120){check(!JiahaoCinematicController.isCameraActive()&&!JiahaoCinematicController.locksInput(),"Damage restores control");var q=JiahaoQuoteClientState.active().get(actorId);check(q==null||q.quote.category()!=JiahaoQuoteCategory.RANDOM_MOMENT,"Session subtitle canceled with hurt");ack();}
    }
    case 8 -> {
     if(JiahaoCinematicController.isPoseActive(a)){seen=true;if(actor&&JiahaoCinematicController.poseElapsed(a)>=28){c.setScreen(new JiahaoMarketScreen());check(!JiahaoCinematicController.locksInput()&&!JiahaoCinematicController.isCameraActive(),"Opening GUI releases camera immediately");}}
     else if(seen&&ticks>300){var q=JiahaoQuoteClientState.active().get(actorId);check(q==null||q.quote.category()!=JiahaoQuoteCategory.RANDOM_MOMENT,"GUI cancellation clears matching session subtitle");if(c.currentScreen!=null)c.currentScreen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);check(c.currentScreen==null&&!JiahaoCinematicController.locksInput(),"GUI exit restores input");ack();}
    }
    case 9 -> {
     if(actor&&ticks==7)c.inGameHud.getChatHud().clear(false);
     if(actor&&command<7){
      var pose=JiahaoPoseType.values()[command+1];
      if(ticks%16==1){JiahaoCinematicController.cleanup();JiahaoCinematicController.onMomentSync(new JiahaoMomentStatePayload(actorId,c.world.getRegistryKey().getValue(),UUID.randomUUID(),true,20,pose,c.player.getPos(),c.player.getYaw(),120));}
      if(ticks%16==13)shot("pose-"+pose.name().toLowerCase(Locale.ROOT)+".png");
      if(ticks%16==0){JiahaoCinematicController.cleanup();command++;}return;
     }
     c.disconnect();check(!JiahaoCinematicController.isCameraActive()&&!JiahaoCinematicController.locksInput(),"Disconnect clears camera/pose");finish(c,"PASSED");
    }
    default -> throw new AssertionError("Unexpected stage "+stage);
   }
  }catch(Throwable e){e.printStackTrace();if(ClientPlayNetworking.canSend(Phase7Ack.ID))ClientPlayNetworking.send(new Phase7Ack(stage,e.toString()));finish(c,"FAILED: stage "+stage+" "+e);}
 }
 private void market(MinecraftClient c,boolean actor)throws Exception{
  if(ticks==5){c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoMarketScreen,"Right click opens market in either form");check(JiahaoStateManager.isJiahao(c.player)==actor,"GUI usable without transformation");check(!c.currentScreen.shouldPause(),"Market does not pause");}
  if(ticks>=6&&ticks<=32){check(c.currentScreen instanceof JiahaoMarketScreen,"Market remains open");var screen=(JiahaoMarketScreen)c.currentScreen;if(ticks==8){double price=screen.model().current();for(var child:screen.children())if(child instanceof ButtonWidget button)button.onPress();check(price==screen.model().current(),"BUY SELL only presentation");}if(ticks==24){check(screen.model().size()>=82,"Chart appends every eight ticks");shot(actor?"market-hao.png":"market-normal.png");}}
  if(ticks==33){var screen=c.currentScreen;screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);check(c.currentScreen==null,"Market ESC normal close");c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoMarketScreen&&((JiahaoMarketScreen)c.currentScreen).model().size()==80,"Reopening generates fresh market");c.currentScreen.close();validateIcons(c);poseTests(c);}
  if(ticks>=35){var q=JiahaoQuoteClientState.local();check(actor?q!=null&&q.quote.category()==JiahaoQuoteCategory.MARKET:q==null,"Gadget quote restricted to form");ack();}
 }
 private void code(MinecraftClient c,boolean actor)throws Exception{
  var currentQuote=JiahaoQuoteClientState.local();if(currentQuote!=null&&currentQuote.quote.category()==JiahaoQuoteCategory.CODE)quoted=true;
  if(!actor)check(currentQuote==null,"Normal form receives no gadget quote");
  if(ticks==5){c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(c.currentScreen instanceof JiahaoCodeScreen,"Right click opens code in either form");check(!c.currentScreen.shouldPause(),"Code does not pause");var random=JiahaoCodeScreen.class.getDeclaredField("random");random.setAccessible(true);((Random)random.get(c.currentScreen)).setSeed(4096);}
  if(ticks>=6&&command<7){
   check(c.currentScreen instanceof JiahaoCodeScreen,"Code stays open");var screen=(JiahaoCodeScreen)c.currentScreen;var field=(TextFieldWidget)screen.children().stream().filter(e->e instanceof TextFieldWidget).findFirst().orElseThrow();
   if(!screen.model().running()){
    String value=new String[]{"test","dir","cmd","powershell","rm","bash","time.stop()"}[command];field.setText("");for(char ch:value.toCharArray())screen.charTyped(ch,0);check(field.getText().equals(value),"Vanilla input letters/symbols");screen.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
    check(screen.model().history(command).equals(value)&&!screen.model().row(screen.model().outputSize()-2).translation(),"Input displayed only as literal");command++;
   }
   if(command>=1&&screen.model().running()&&screen.model().progress()>30)shot(actor?"code-hao.png":"code-normal.png");
  }
  if(command==7&&ticks>195){var screen=(JiahaoCodeScreen)c.currentScreen;var field=(TextFieldWidget)screen.children().stream().filter(e->e instanceof TextFieldWidget).findFirst().orElseThrow();field.setText("x".repeat(1000));check(field.getText().length()==128,"Real input widget limits length");field.setText("hello 233 !@#$%^&*() ");check(field.getText().endsWith(" "),"Symbols digits and spaces allowed");screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);check(c.currentScreen==null&&screen.model().historySize()==0&&screen.model().outputSize()==0,"ESC clears terminal buffers");c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);check(((JiahaoCodeScreen)c.currentScreen).model().historySize()==0,"Reopening clean history");c.currentScreen.close();command=8;}
  if(command==8){check(actor?quoted:!quoted,"Code quote follows server form validation");ack();}
 }
 private static void validateIcons(MinecraftClient c)throws Exception{
  for(String name:new String[]{"jiahao_transformer","market_viewer","jiahao_code_editor"})try(var in=c.getResourceManager().getResource(JiahaoMode.id("textures/item/"+name+".png")).orElseThrow().getInputStream();var image=NativeImage.read(in)){
   check(image.getWidth()==16&&image.getHeight()==16,"16x16 packaged item icon");int transparent=0,opaque=0;for(int y=0;y<16;y++)for(int x=0;x<16;x++){int alpha=image.getColor(x,y)>>>24;check(alpha==0||alpha==255,"Hard pixel alpha");if(alpha==0)transparent++;else opaque++;}check(transparent>0&&opaque>20,"Real transparent sprite");
  }
 }
 private static ModelPart[] parts(PlayerEntityModel<?> m){return new ModelPart[]{m.head,m.body,m.rightArm,m.leftArm,m.rightLeg,m.leftLeg,m.hat,m.jacket,m.rightSleeve,m.leftSleeve,m.rightPants,m.leftPants};}
 private static ModelTransform[] transforms(PlayerEntityModel<?> m){return Arrays.stream(parts(m)).map(ModelPart::getTransform).toArray(ModelTransform[]::new);}
 private static void equal(ModelTransform[] a,ModelTransform[] b,String why){for(int i=0;i<a.length;i++){check(Math.abs(a[i].pitch-b[i].pitch)<.00001&&Math.abs(a[i].yaw-b[i].yaw)<.00001&&Math.abs(a[i].roll-b[i].roll)<.00001&&Math.abs(a[i].pivotX-b[i].pivotX)<.00001&&Math.abs(a[i].pivotY-b[i].pivotY)<.00001&&Math.abs(a[i].pivotZ-b[i].pivotZ)<.00001,why+" part "+i);}}
 private static void poseTests(MinecraftClient c){
  var fixture=new OtherClientPlayerEntity(c.world,new GameProfile(UUID.randomUUID(),"StaticPoseFixture"));
  var model=new PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity>(c.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER),false);
  for(var pose:JiahaoPoseType.values()){
   model.setAngles(fixture,0,0,0,0,0);var vanilla=transforms(model);UUID id=UUID.randomUUID();
   var payload=new JiahaoMomentStatePayload(fixture.getUuid(),c.world.getRegistryKey().getValue(),id,true,20,pose,c.player.getPos(),0,120);
   JiahaoCinematicController.onMomentSync(payload);JiahaoCinematicController.beginFrame(0);JiahaoPoseController.apply(model,fixture);var first=transforms(model);
   var armor=new BipedEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity>(c.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR));model.copyBipedStateTo(armor);check(armor.rightArm.pitch==model.rightArm.pitch&&armor.body.yaw==model.body.yaw,"Armor follows "+pose);
   JiahaoPoseController.restore(model);equal(vanilla,transforms(model),"Rendering restores all model parts "+pose);
   JiahaoCinematicController.onMomentSync(new JiahaoMomentStatePayload(fixture.getUuid(),payload.dimension(),id,true,40,pose,payload.origin(),0,120));JiahaoCinematicController.beginFrame(.5f);
   model.setAngles(fixture,2,1,80,70,35);equal(first,transforms(model),"Hold ignores walk/breath/head animation "+pose);JiahaoPoseController.restore(model);
   JiahaoCinematicController.onMomentSync(new JiahaoMomentStatePayload(fixture.getUuid(),payload.dimension(),id,false,60,pose,payload.origin(),0,120));
   JiahaoCinematicController.onMomentSync(payload);check(!JiahaoCinematicController.isPoseActive(fixture),"Expired active packet ignored");
   check(!JiahaoCinematicController.isCameraActive(),"Remote test never takes camera");
  }
  System.out.println("PHASE7 ALL 8 POSES STATIC, ARMOR AND MODEL RESTORATION PASSED");
 }
 private void ack(){if(!acked){acked=true;ClientPlayNetworking.send(new Phase7Ack(stage,"PASSED"));System.out.println("PHASE7 CLIENT STAGE "+stage+" PASSED");}}
 private static void shot(String name){if(!SHOTS.contains(name)&&pendingShot==null)pendingShot=name;}
 public static void capture(){if(pendingShot!=null){var c=MinecraftClient.getInstance();String name=pendingShot;net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,name,c.getFramebuffer(),t->{});SHOTS.add(name);pendingShot=null;}}
 private void finish(MinecraftClient c,String result){done=true;try{Files.writeString(Path.of("phase7-client-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}c.disconnect();c.scheduleStop();}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
