// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.gadget;

import com.shouyun.jiahaomode.client.JiahaoSubtitleRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.Locale;
import java.util.Random;

public final class JiahaoMarketScreen extends Screen implements JiahaoSubtitleRenderer.QuoteOverlayScreen {
    private final JiahaoMarketModel market=new JiahaoMarketModel(new Random());
    private final int[] xs=new int[JiahaoMarketModel.CAPACITY],ys=new int[JiahaoMarketModel.CAPACITY];
    private int left,top,panelWidth,panelHeight,chartLeft,chartTop,chartWidth,chartHeight,ticks,messageUntil;
    private Text message=Text.empty(),current=Text.empty(),change=Text.empty(),high=Text.empty(),low=Text.empty();
    private Text reward=Text.empty();private int rewardUntil;
    public void rewardMessage(Text text){reward=text;rewardUntil=ticks+80;}
    public JiahaoMarketScreen(){super(t("title"));}
    private static Text t(String suffix){return Text.translatable("screen.jiahao-mode.market."+suffix);}
    @Override protected void init(){
        panelWidth=Math.min(420,width-16);panelHeight=Math.min(258,height-52);left=(width-panelWidth)/2;top=Math.max(8,(height-panelHeight-36)/2);
        chartLeft=left+35;chartTop=top+55;chartWidth=panelWidth-49;chartHeight=Math.max(6,panelHeight-119);
        addDrawableChild(ButtonWidget.builder(t("buy"),button->{HaoGadgetClient.begin(com.shouyun.jiahaomode.network.HaoGadgetPayload.Action.BUY);message=t(market.buy());messageUntil=ticks+60;}).dimensions(left+14,top+panelHeight-36,64,20).build());
        addDrawableChild(ButtonWidget.builder(t("sell"),button->{HaoGadgetClient.begin(com.shouyun.jiahaomode.network.HaoGadgetPayload.Action.SELL);message=t(market.sell());messageUntil=ticks+60;}).dimensions(left+84,top+panelHeight-36,64,20).build());
        refreshChart();
    }
    private static String money(double price){return String.format(Locale.ROOT,"%.2f",price);}
    private void refreshChart(){
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(int i=0;i<market.size();i++){min=Math.min(min,market.get(i));max=Math.max(max,market.get(i));}
        double margin=Math.max(.02,(max-min)*.12);min-=margin;max+=margin;
        for(int i=0;i<market.size();i++){xs[i]=chartLeft+i*chartWidth/(market.size()-1);ys[i]=chartTop+chartHeight-(int)((market.get(i)-min)/(max-min)*chartHeight);}
        current=Text.translatable("screen.jiahao-mode.market.current",money(market.current()));
        change=Text.literal(String.format(Locale.ROOT,"%+.2f%%",market.percent()));
        high=Text.literal(money(max));low=Text.literal(money(min));
    }
    @Override public void tick(){if(++ticks%8==0){market.append();refreshChart();}}
    /** Integer line drawing keeps the chart texture-free and allocates nothing per segment. */
    private static void line(DrawContext d,int x,int y,int endX,int endY,int color){
        int dx=Math.abs(endX-x),dy=-Math.abs(endY-y),sx=x<endX?1:-1,sy=y<endY?1:-1,err=dx+dy;
        while(true){d.fill(x,y,x+1,y+1,color);if(x==endX&&y==endY)break;int e=2*err;if(e>=dy){err+=dy;x+=sx;}if(e<=dx){err+=dx;y+=sy;}}
    }
    @Override public void render(DrawContext d,int mouseX,int mouseY,float delta){
        d.fill(0,0,width,height,0xE8090E14);d.fill(left,top,left+panelWidth,top+panelHeight,0xFF111A22);
        d.fill(left,top,left+panelWidth,top+2,0xFF4CDEA5);
        d.drawText(textRenderer,title,left+12,top+10,0xE8F8F0,false);
        d.drawText(textRenderer,t("name."+market.name()),left+12,top+27,0x80D4B1,false);
        d.drawText(textRenderer,current,left+12,top+43,0xD8E4EE,false);
        int color=market.percent()>=0?0xFF4CDEA5:0xFFEE8C8C;
        d.drawText(textRenderer,change,left+panelWidth-12-textRenderer.getWidth(change),top+43,color,false);
        for(int i=0;i<=4;i++){int y=chartTop+i*chartHeight/4;d.fill(chartLeft,y,chartLeft+chartWidth+1,y+1,0xFF23323F);}
        for(int i=0;i<=6;i++){int x=chartLeft+i*chartWidth/6;d.fill(x,chartTop,x+1,chartTop+chartHeight,0xFF23323F);}
        d.fill(chartLeft-1,chartTop,chartLeft,chartTop+chartHeight+2,0xFF77929F);
        d.fill(chartLeft-1,chartTop+chartHeight+1,chartLeft+chartWidth+1,chartTop+chartHeight+2,0xFF77929F);
        d.drawText(textRenderer,high,left+3,chartTop,0x829BA6,false);d.drawText(textRenderer,low,left+3,chartTop+chartHeight-8,0x829BA6,false);
        for(int i=1;i<market.size();i++){int c=market.get(i)>=market.get(i-1)?0xFF4CDEA5:0xFFEE8C8C;line(d,xs[i-1],ys[i-1],xs[i],ys[i],c);if(i%16==0)d.fill(xs[i]-1,ys[i]-1,xs[i]+2,ys[i]+2,c);}
        d.drawText(textRenderer,t("time"),chartLeft+chartWidth-30,chartTop+chartHeight+5,0x829BA6,false);
        d.drawText(textRenderer,t("fictional"),left+12,top+panelHeight-51,0x829BA6,false);
        if(ticks<messageUntil)d.drawText(textRenderer,message,left+158,top+panelHeight-30,0x8DE1BC,false);
        super.render(d,mouseX,mouseY,delta);JiahaoSubtitleRenderer.render(d);
        if(ticks<rewardUntil)d.drawCenteredTextWithShadow(textRenderer,reward,width/2,top+panelHeight+6,0xADEDBD);
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext d,int mouseX,int mouseY,float delta){} // Custom background is drawn before the content.
    @Override public void removed(){HaoGadgetClient.close();super.removed();}
    public JiahaoMarketModel model(){return market;}
}
