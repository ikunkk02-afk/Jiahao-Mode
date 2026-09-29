// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.quote.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import java.util.*;
/** One real-time display lease per speaker. No world-time or frozen tick-delta dependency. */
public final class JiahaoQuoteClientState {
 private static ClientWorld world;
 private static long lastEvent;
 private static final Map<UUID,Active> ACTIVE=new HashMap<>();
 private record Queued(JiahaoQuote quote,long after) {}
 private static final Map<UUID,Queued> ENDING=new HashMap<>();
 public static final class Active {
  public final JiahaoQuote quote;public final Text text;public final UUID session;
  public final long received=System.nanoTime();public long started;
  private int width=-1;private String language="";private List<OrderedText> lines=List.of();
  Active(JiahaoQuote q,UUID session,boolean wait){quote=q;text=Text.translatable(q.translationKey());this.session=session;started=wait?0:received;}
  public double age(){return started==0?0:(System.nanoTime()-started)/1e9;}
  public float alpha(){return opacity(age(),quote.displayTicks()/20.0);}
  public List<OrderedText> lines(int width){
   var c=MinecraftClient.getInstance();String language=c.options.language;
   if(this.width!=width||!this.language.equals(language)){this.width=width;this.language=language;lines=c.textRenderer.wrapLines(text,width);}
   return lines;
  }
 }
 public static float opacity(double age,double duration){return (float)Math.max(0,Math.min(1,Math.min(age/.15,(duration-age)/.25)));}
 public static void initialize(){
  ClientPlayNetworking.registerGlobalReceiver(JiahaoQuoteSyncPayload.ID,(p,c)->receive(p));
  ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear());
  ClientTickEvents.END_CLIENT_TICK.register(c->update());
 }
 public static void receive(JiahaoQuoteSyncPayload p){
  update();var c=MinecraftClient.getInstance();
  if(world==null||!world.getRegistryKey().getValue().equals(p.dimension())||p.event()<=lastEvent)return;
  if(p.quote()==null){lastEvent=p.event();ACTIVE.remove(p.player());ENDING.remove(p.player());return;}
  var quote=JiahaoQuoteRegistry.get(p.quote());if(quote==null)return;
  lastEvent=p.event();
  var current=ACTIVE.get(p.player());
  if(current!=null&&current.quote.priority()>quote.priority()) {
   if(quote.category()==JiahaoQuoteCategory.TIME_STOP_END)ENDING.put(p.player(),new Queued(quote,(current.started==0?System.nanoTime():current.started)+(long)((current.quote.displayTicks()/20.0+.25)*1e9)));
   return;
  }
  ENDING.remove(p.player());boolean local=c.player!=null&&p.player().equals(c.player.getUuid());
  ACTIVE.put(p.player(),new Active(quote,p.session(),local&&p.session()!=null));
 }
 public static void update(){
  var c=MinecraftClient.getInstance();
  if(world!=c.world){clear();world=c.world;}
  if(world==null)return;
  ACTIVE.entrySet().removeIf(entry->{
   var state=entry.getValue();var player=world.getPlayerByUuid(entry.getKey());
   if(player!=null&&(!player.isAlive()||player.isRemoved()))return true;
   if(state.started==0){
    if(state.session.equals(JiahaoCinematicController.sessionId())&&JiahaoCinematicController.getProgress()>=.74)state.started=System.nanoTime();
    else if(System.nanoTime()-state.received>2_000_000_000L)return true;
   }
   return state.started!=0&&state.age()>=state.quote.displayTicks()/20.0;
  });
  ENDING.entrySet().removeIf(entry->{
   if(!ACTIVE.containsKey(entry.getKey())&&System.nanoTime()>=entry.getValue().after()) {
    ACTIVE.put(entry.getKey(),new Active(entry.getValue().quote(),null,false));return true;
   }
   return false;
  });
 }
 public static Active local(){update();var p=MinecraftClient.getInstance().player;return p==null?null:ACTIVE.get(p.getUuid());}
 public static Map<UUID,Active> active(){update();return Collections.unmodifiableMap(ACTIVE);}
 public static void clear(){ACTIVE.clear();ENDING.clear();lastEvent=0;world=null;}
 private JiahaoQuoteClientState(){}
}
