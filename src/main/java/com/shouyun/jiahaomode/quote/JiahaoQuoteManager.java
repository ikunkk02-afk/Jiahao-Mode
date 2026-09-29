// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.quote;
import com.shouyun.jiahaomode.network.JiahaoQuoteSyncPayload;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import static com.shouyun.jiahaomode.quote.JiahaoQuoteCategory.*;

/** Server-thread authority. Display leases and form monitoring are separate from cooldown history. */
public final class JiahaoQuoteManager {
 private static final Map<MinecraftServer,Runtime> SERVERS=new WeakHashMap<>();
 private static final class Runtime {
  long sequence;
  final Map<UUID,State> active=new HashMap<>();
  final Map<UUID,History> histories=new HashMap<>();
 }
 private static final class History {
  long manualUntil, ordinaryUntil, perfectUntil;
  Identifier last;
  final Map<JiahaoQuoteCategory,Identifier> lastCategory=new EnumMap<>(JiahaoQuoteCategory.class);
 }
 private static final class State {
  Identifier dimension; Vec3d position;
  long transformAt=-1,idleAt,until,endAt=-1,cinematicAt;
  int priority; boolean lowLatched,lowPending,cueSent;
  UUID session;
  final Set<UUID> recipients=new HashSet<>();
  State(ServerPlayerEntity p,long tick){dimension=p.getWorld().getRegistryKey().getValue();position=p.getPos();idleAt=tick+600;}
 }
 private static Runtime runtime(MinecraftServer server){
  if(!server.isOnThread())throw new IllegalStateException("Quote authority requires server thread");
  return SERVERS.computeIfAbsent(server,s->new Runtime());
 }
 private static long now(ServerPlayerEntity p){return JiahaoTimeStopManager.getServerTick(p.getServer());}
 private static boolean valid(ServerPlayerEntity p){return p.isAlive()&&!p.isRemoved()&&JiahaoStateManager.isJiahao(p);}
 public static void track(ServerPlayerEntity p){if(valid(p))runtime(p.getServer()).active.computeIfAbsent(p.getUuid(),id->new State(p,now(p)));}
 public static void formChanged(ServerPlayerEntity p,boolean enabled){
  clear(p,false);if(enabled&&valid(p)){track(p);runtime(p.getServer()).active.get(p.getUuid()).transformAt=now(p)+20;}
 }
 public static void clear(ServerPlayerEntity p,boolean forget){
  Runtime r=runtime(p.getServer());State s=r.active.remove(p.getUuid());
  if(s!=null)cancel(p,s,r);
  if(forget)r.histories.remove(p.getUuid());
 }
 public static void stopServer(MinecraftServer server){SERVERS.remove(server);}
 private static void cancel(ServerPlayerEntity p,State s,Runtime r){
  var payload=new JiahaoQuoteSyncPayload(p.getUuid(),s.dimension,null,++r.sequence,null);
  for(UUID id:s.recipients){var recipient=p.getServer().getPlayerManager().getPlayer(id);if(recipient!=null&&ServerPlayNetworking.canSend(recipient,JiahaoQuoteSyncPayload.ID))ServerPlayNetworking.send(recipient,payload);}
  s.recipients.clear();s.until=0;s.priority=0;
 }
 public static void activity(ServerPlayerEntity p){
  State s=runtime(p.getServer()).active.get(p.getUuid());if(s!=null)s.idleAt=now(p)+600;
 }
 public static boolean manual(ServerPlayerEntity p){activity(p);return emit(p,MANUAL,null);}
 public static boolean emit(ServerPlayerEntity p,JiahaoQuoteCategory category,UUID session){
  if(!valid(p))return false;
  Runtime r=runtime(p.getServer());State s=r.active.get(p.getUuid());if(s==null)return false;
  History h=r.histories.computeIfAbsent(p.getUuid(),id->new History());long t=now(p);
  boolean special=category.priority>=30;
  if(s.session!=null&&!s.cueSent&&!special)return false;
  if(t<s.until&&(!special||category.priority<=s.priority))return false;
  if(category==MANUAL&&t<h.manualUntil)return false;
  if(category==PERFECT_DODGE&&(t<h.perfectUntil||JiahaoTimeStopManager.isCinematicLocked(p)))return false;
  if(!special&&category!=MANUAL&&t<h.ordinaryUntil)return false;
  var quote=JiahaoQuoteRegistry.select(category,h.last,h.lastCategory.get(category),bound->p.getRandom().nextInt(bound));
  s.until=t+quote.displayTicks();s.priority=quote.priority();
  h.last=quote.id();h.lastCategory.put(category,quote.id());h.ordinaryUntil=t+60;
  if(category==MANUAL)h.manualUntil=t+50;
  if(category==PERFECT_DODGE)h.perfectUntil=t+80;
  var payload=new JiahaoQuoteSyncPayload(p.getUuid(),s.dimension,quote.id(),++r.sequence,session);
  for(var recipient:p.getServerWorld().getPlayers()) {
   if(recipient.squaredDistanceTo(p)<=64*64&&ServerPlayNetworking.canSend(recipient,JiahaoQuoteSyncPayload.ID)) {
    ServerPlayNetworking.send(recipient,payload);s.recipients.add(recipient.getUuid());
   }
  }
  return true;
 }
 public static void timeStarted(ServerPlayerEntity p,UUID session,boolean cinematic){
  track(p);State s=runtime(p.getServer()).active.get(p.getUuid());if(s==null)return;
  activity(p);s.transformAt=-1;s.endAt=-1;s.session=session;s.cueSent=false;s.cinematicAt=now(p)+74;
  if(cinematic) {cancel(p,s,runtime(p.getServer()));}
  else fallback(p,s);
 }
 private static void fallback(ServerPlayerEntity p,State s){if(!s.cueSent){s.cueSent=true;emit(p,TIME_STOP_START,null);}}
 public static void cinematicStopped(ServerPlayerEntity p,UUID session){
  State s=runtime(p.getServer()).active.get(p.getUuid());if(s!=null&&session.equals(s.session))fallback(p,s);
 }
 public static void playbackFailed(ServerPlayerEntity p,UUID session){
  State s=runtime(p.getServer()).active.get(p.getUuid());
  if(valid(p)&&s!=null&&session.equals(s.session)&&!s.cueSent&&JiahaoTimeStopManager.isOwner(p))fallback(p,s);
 }
 public static void timeEnded(ServerPlayerEntity p,UUID session,boolean speak){
  State s=runtime(p.getServer()).active.get(p.getUuid());if(s==null||!session.equals(s.session))return;
  s.session=null;activity(p);
  if(speak&&valid(p))s.endAt=Math.max(now(p),s.until+5);
  else clear(p,false);
 }
 public static void damage(ServerPlayerEntity p,float actual){
  if(!valid(p))return;activity(p);health(p);
  State s=runtime(p.getServer()).active.get(p.getUuid());
  if(s!=null&&!s.lowPending&&p.getHealth()>=p.getMaxHealth()*.3f&&actual>=2&&p.getRandom().nextFloat()<.18f)emit(p,TAKE_DAMAGE,null);
 }
 public static void killed(ServerPlayerEntity p){activity(p);if(valid(p)&&p.getRandom().nextFloat()<.2f)emit(p,KILL_ENTITY,null);}
 public static boolean perfectDodge(ServerPlayerEntity p){activity(p);return emit(p,PERFECT_DODGE,null);}
 private static void health(ServerPlayerEntity p){
  State s=runtime(p.getServer()).active.get(p.getUuid());if(s==null)return;
  float ratio=p.getHealth()/p.getMaxHealth();
  if(ratio>.5f){s.lowLatched=false;s.lowPending=false;}
  if(ratio<.3f&&!s.lowLatched){s.lowLatched=true;s.lowPending=true;}
  if(s.lowPending&&ratio>=.3f)s.lowPending=false;
  if(s.lowPending&&emit(p,LOW_HEALTH,null))s.lowPending=false;
 }
 public static void tick(MinecraftServer server){
  Runtime r=runtime(server);long tick=JiahaoTimeStopManager.getServerTick(server);
  for(var entry:new ArrayList<>(r.active.entrySet())) {
   var p=server.getPlayerManager().getPlayer(entry.getKey());State s=entry.getValue();
   if(p==null){r.active.remove(entry.getKey());r.histories.remove(entry.getKey());continue;}
   if(!valid(p)||!s.dimension.equals(p.getWorld().getRegistryKey().getValue())){clear(p,false);continue;}
   if(s.until>0&&tick>=s.until){s.until=0;s.priority=0;}
   if(p.getPos().squaredDistanceTo(s.position)>.0001){s.position=p.getPos();activity(p);}
   if(s.session!=null&&!s.cueSent&&tick>=s.cinematicAt){
    s.cueSent=true;emit(p,CINEMATIC,s.session);
   }
   if(s.endAt>=0&&tick>=s.endAt&&emit(p,TIME_STOP_END,null))s.endAt=-1;
   if(s.transformAt>=0&&tick>=s.transformAt){s.transformAt=-1;emit(p,TRANSFORM,null);}
   health(p);
   if(tick>=s.idleAt){s.idleAt=tick+600;if(!JiahaoTimeStopManager.shouldFreeze(p)&&p.getRandom().nextFloat()<.1f)emit(p,IDLE,null);}
  }
 }
 private JiahaoQuoteManager(){}
}
