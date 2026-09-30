// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.gadget;
import com.shouyun.jiahaomode.network.HaoGadgetPayload;
import com.shouyun.jiahaomode.network.JiahaoGadgetResultPayload;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import java.util.UUID;
import static com.shouyun.jiahaomode.network.HaoGadgetPayload.*;

public final class HaoGadgetClient {
    private static UUID session,operation;private static Action kind,queued,action;private static int ticks,elapsed;
    private static boolean beginning;
    private HaoGadgetClient() {}
    private static boolean screen(MinecraftClient c){return kind==Action.CODE?c.currentScreen instanceof JiahaoCodeScreen:c.currentScreen instanceof JiahaoMarketScreen;}
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(HaoGadgetPayload.ID,(p,c)->{
            if(p.phase()==Phase.OPEN){session=p.session();operation=null;kind=p.action();ticks=0;beginning=false;if(queued!=null){var next=queued;queued=null;begin(next);}}
            else if(session!=null&&session.equals(p.session())&&p.phase()==Phase.GRANT){operation=p.operation();action=p.action();elapsed=0;beginning=false;}
            else if(session!=null&&session.equals(p.session())&&p.phase()==Phase.COMPLETE&&p.operation().equals(operation)){operation=null;}
        });
        ClientPlayNetworking.registerGlobalReceiver(JiahaoGadgetResultPayload.ID,(p,c)->{
            if(!p.session().equals(session)||!p.operation().equals(operation)||!screen(c.client()))return;
            var result=p.result();
            Text message=switch(result.status()){
                case NOT_IN_FORM -> Text.translatable("message.jiahao-mode.buff.form_required");
                case COOLDOWN -> Text.translatable("message.jiahao-mode.buff.cooldown");
                case INVALID -> Text.translatable("message.jiahao-mode.buff.invalid");
                case PRESERVED -> Text.translatable("message.jiahao-mode.buff.preserved");
                case GRANTED -> Text.translatable("message.jiahao-mode.buff.granted",
                        Registries.STATUS_EFFECT.get(result.effect()).getName(),result.amplifier()+1,result.durationTicks()/20);
            };
            if(c.client().currentScreen instanceof JiahaoMarketScreen market)market.rewardMessage(message);
            if(c.client().currentScreen instanceof JiahaoCodeScreen code)code.rewardMessage(message,result.override());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(session==null)return;
            if(c.world==null||c.player==null||!screen(c)){close();return;}
            if(++ticks%10==0)send(new HaoGadgetPayload(session,NONE,kind,Phase.PING));
            if(operation!=null&&++elapsed>=(action==Action.CODE?24:20)&&elapsed%5==0
                    &&(!(c.currentScreen instanceof JiahaoCodeScreen code)||!code.model().running()))
                send(new HaoGadgetPayload(session,operation,action,Phase.COMMIT));
        });
    }
    public static void begin(Action next){
        if(session==null){queued=next;return;}
        if(!screen(MinecraftClient.getInstance())||operation!=null||beginning)return;
        beginning=true;
        send(new HaoGadgetPayload(session,NONE,next,Phase.BEGIN));
    }
    public static void close(){if(session!=null)send(new HaoGadgetPayload(session,NONE,kind,Phase.CLOSE));reset();}
    private static void send(HaoGadgetPayload p){if(ClientPlayNetworking.canSend(HaoGadgetPayload.ID))ClientPlayNetworking.send(p);}
    private static void reset(){session=operation=null;kind=queued=action=null;ticks=elapsed=0;beginning=false;}
}
