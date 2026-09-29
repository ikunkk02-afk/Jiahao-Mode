// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;
import com.shouyun.jiahaomode.client.gadget.*;
import java.util.Random;
public final class GadgetModelTests {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        var market=new JiahaoMarketModel(new Random(42));check(market.size()==80,"initial chart");double initial=market.current();
        for(int i=0;i<100000;i++){market.append();check(market.size()<=128,"bounded chart");check(market.current()>0&&Double.isFinite(market.current()),"positive finite prices");}
        check(market.size()==128&&initial!=market.current(),"live ring");double price=market.current();
        market.buy();market.sell();check(market.current()==price&&market.size()==128,"buttons have no model side effects");
        var terminal=new JiahaoCodeModel(new Random(42));check(!terminal.submit(" "),"blank rejected");
        for(String input:new String[]{"dir","cmd","powershell","rm","bash","time.stop()","hello 233 !@#$%^&*()"}){
            check(terminal.submit(input),"accept literal");check(terminal.history(terminal.historySize()-1).equals(input),"literal unchanged");
            check(!terminal.row(terminal.outputSize()-2).translation(),"input never a translation/command");
            check(!terminal.submit("overlap"),"no overlapping fake runs");for(int tick=0;tick<24;tick++)terminal.tick();check(!terminal.running(),"1.2 second effect complete");
        }
        check(terminal.submit("x".repeat(9999)),"long input accepted safely");check(terminal.history(terminal.historySize()-1).length()==128,"128 char bound");for(int tick=0;tick<24;tick++)terminal.tick();
        for(int i=0;i<200;i++){check(terminal.submit("text "+i),"repeat");for(int tick=0;tick<24;tick++)terminal.tick();}
        check(terminal.historySize()==20&&terminal.outputSize()==80,"bounded history/output");check(terminal.history(0).equals("text 180"),"oldest evicted");
        terminal.clear();check(terminal.historySize()==0&&terminal.outputSize()==0&&!terminal.running(),"close clears all text");
        System.out.println("Gadget model tests passed: fictional chart, literal inputs, bounded buffers, fake completion");
    }
}
