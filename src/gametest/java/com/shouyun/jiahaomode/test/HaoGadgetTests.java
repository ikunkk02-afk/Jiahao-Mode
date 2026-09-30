// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.hao.*;
import com.shouyun.jiahaomode.item.ModItems;
import com.shouyun.jiahaomode.network.*;
import com.shouyun.jiahaomode.state.JiahaoStateManager;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.test.*;
import java.util.*;
import static com.shouyun.jiahaomode.network.HaoGadgetPayload.*;

public final class HaoGadgetTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="hao_gadget",tickLimit=200)
    public void validatedCompletionsAndReplay(TestContext c){
        var a=JiahaoTransformationTests.connectTestPlayer(c.getWorld().getServer(),c.getWorld(),"HaoGadget");var p=a.player();
        p.setNoGravity(true);p.getAbilities().flying=true;
        p.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(new RegistrationPayload(RegistrationPayload.REGISTER,List.of(HaoGadgetPayload.ID.id()))));
        p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.MARKET_VIEWER));JiahaoStateManager.setJiahao(p,true);HaoMeterManager.set(p,0);
        HaoGadgetManager.receive(p,new HaoGadgetPayload(UUID.randomUUID(),UUID.randomUUID(),Action.BUY,Phase.COMMIT));
        c.assertTrue(HaoMeterManager.data(p).units()==0,"Forged completion gives no reward");
        ModItems.MARKET_VIEWER.use(c.getWorld(),p,Hand.MAIN_HAND);var open=last(a,Phase.OPEN);
        HaoGadgetManager.receive(p,new HaoGadgetPayload(open.session(),NONE,Action.BUY,Phase.BEGIN));var grant=last(a,Phase.GRANT);
        var commit=new HaoGadgetPayload(open.session(),grant.operation(),Action.BUY,Phase.COMMIT);
        HaoGadgetManager.receive(p,commit);c.assertTrue(HaoMeterManager.data(p).units()==0,"Too-early completion rejected");
        c.runAtTick(20,()->{
            int before=HaoMeterManager.data(p).units();HaoGadgetManager.receive(p,commit);c.assertTrue(HaoMeterManager.data(p).units()==before+200,"Verified market +2");
            HaoGadgetManager.receive(p,commit);c.assertTrue(HaoMeterManager.data(p).units()==before+200,"Operation token cannot be replayed");
            HaoGadgetManager.receive(p,new HaoGadgetPayload(open.session(),NONE,Action.SELL,Phase.BEGIN));
        });
        c.runAtTick(30,()->HaoGadgetManager.receive(p,new HaoGadgetPayload(open.session(),NONE,Action.SELL,Phase.PING)));
        c.runAtTick(40,()->{
            var second=last(a,Phase.GRANT);int before=HaoMeterManager.data(p).units();
            HaoGadgetManager.receive(p,new HaoGadgetPayload(open.session(),second.operation(),Action.SELL,Phase.COMMIT));c.assertTrue(HaoMeterManager.data(p).units()==before,"Shared BUY/SELL ten-second cooldown");
            HaoGadgetManager.receive(p,new HaoGadgetPayload(open.session(),NONE,Action.SELL,Phase.CLOSE));
            p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.JIAHAO_CODE_EDITOR));ModItems.JIAHAO_CODE_EDITOR.use(c.getWorld(),p,Hand.MAIN_HAND);

        });
        c.runAtTick(41,()->{
            var code=last(a,Phase.OPEN);c.assertTrue(code.action()==Action.CODE,"Server issues code session");
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),NONE,Action.CODE,Phase.BEGIN));
        });
        c.runAtTick(66,()->{
            var code=last(a,Phase.OPEN);var op=last(a,Phase.GRANT);int before=HaoMeterManager.data(p).units();
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),op.operation(),Action.CODE,Phase.COMMIT));c.assertTrue(HaoMeterManager.data(p).units()==before+200,"Code +2 with independent cooldown before="+before+" after="+HaoMeterManager.data(p).units()+" packet="+op);
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),NONE,Action.CODE,Phase.BEGIN));p.setStackInHand(Hand.OFF_HAND,p.getMainHandStack());p.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        });
        c.runAtTick(88,()->{
            var code=last(a,Phase.OPEN);var op=last(a,Phase.GRANT);int before=HaoMeterManager.data(p).units();
            HaoGadgetManager.receive(p,new HaoGadgetPayload(code.session(),op.operation(),Action.CODE,Phase.COMMIT));c.assertTrue(HaoMeterManager.data(p).units()==before,"Moving original gadget to other hand invalidates operation");
            p.getServer().getPlayerManager().remove(p);a.channel().finishAndReleaseAll();c.complete();
        });
    }
    private static HaoGadgetPayload last(JiahaoTransformationTests.TestPlayerConnection c,Phase phase){c.channel().runPendingTasks();return c.channel().outboundMessages().stream().filter(x->x instanceof CustomPayloadS2CPacket p&&p.payload() instanceof HaoGadgetPayload h&&h.phase()==phase).map(x->(HaoGadgetPayload)((CustomPayloadS2CPacket)x).payload()).reduce((a,b)->b).orElseThrow();}
}
