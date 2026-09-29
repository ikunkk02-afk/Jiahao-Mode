// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.gadget;

import com.shouyun.jiahaomode.client.JiahaoSubtitleRenderer;
import com.shouyun.jiahaomode.network.JiahaoGadgetQuoteRequestPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public final class JiahaoCodeScreen extends Screen implements JiahaoSubtitleRenderer.QuoteOverlayScreen {
    private final Random random=new Random();
    private final JiahaoCodeModel terminal=new JiahaoCodeModel(random);
    private final Text[] lines=new Text[JiahaoCodeModel.OUTPUT_LIMIT];
    private TextFieldWidget input;
    private int left,top,panelWidth,panelHeight,ticks,lastRevision=-1,historyCursor;
    private Text progress=Text.empty();
    public JiahaoCodeScreen(){super(t("title"));}
    private static Text t(String suffix){return Text.translatable("screen.jiahao-mode.code."+suffix);}
    @Override protected void init(){
        String old=input==null?"":input.getText();
        panelWidth=Math.min(440,width-16);panelHeight=Math.min(280,height-52);left=(width-panelWidth)/2;top=Math.max(8,(height-panelHeight-36)/2);
        input=new TextFieldWidget(textRenderer,left+25,top+panelHeight-27,panelWidth-38,18,t("input"));
        input.setMaxLength(JiahaoCodeModel.INPUT_LIMIT);input.setDrawsBackground(false);input.setEditableColor(0xADEDBD);input.setText(old);
        addDrawableChild(input);setInitialFocus(input);historyCursor=terminal.historySize();refresh();
    }
    private void refresh(){
        if(lastRevision!=terminal.revision()){lastRevision=terminal.revision();for(int i=0;i<terminal.outputSize();i++){var row=terminal.row(i);lines[i]=row.translation()?Text.translatable(row.content()):Text.literal(row.content());}}
        int filled=terminal.progress()/5;
        progress=Text.translatable("screen.jiahao-mode.code.progress","#".repeat(filled)+"-".repeat(20-filled),terminal.progress());
    }
    private void submit(){
        if(!terminal.submit(input.getText()))return;
        input.setText("");historyCursor=terminal.historySize();refresh();
        if(random.nextDouble()<.35)JiahaoGadgetClient.request(JiahaoGadgetQuoteRequestPayload.Kind.CODE);
    }
    @Override public void tick(){ticks++;terminal.tick();refresh();}
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){submit();return true;}
        if(key==GLFW.GLFW_KEY_UP&&terminal.historySize()>0){historyCursor=Math.max(0,historyCursor-1);input.setText(terminal.history(historyCursor));return true;}
        if(key==GLFW.GLFW_KEY_DOWN&&terminal.historySize()>0){historyCursor=Math.min(terminal.historySize(),historyCursor+1);input.setText(historyCursor==terminal.historySize()?"":terminal.history(historyCursor));return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public void render(DrawContext d,int mouseX,int mouseY,float delta){
        d.fill(0,0,width,height,0xEF070C0D);d.fill(left,top,left+panelWidth,top+panelHeight,0xFF101A17);
        d.fill(left,top,left+panelWidth,top+2,0xFF6FC997);d.drawText(textRenderer,title,left+12,top+11,0xDBF1E3,false);
        d.drawText(textRenderer,t("fictional"),left+12,top+27,0x79988A,false);
        int visible=Math.max(1,(panelHeight-88)/11),start=Math.max(0,terminal.outputSize()-visible);
        d.enableScissor(left+10,top+42,left+panelWidth-10,top+panelHeight-43);
        for(int i=start;i<terminal.outputSize();i++)d.drawText(textRenderer,lines[i],left+12,top+43+(i-start)*11,0xADEDBD,false);
        d.disableScissor();
        if(terminal.running())d.drawText(textRenderer,progress,left+12,top+panelHeight-43,0xD9EEDF,false);
        else if(ticks%20<10)d.drawText(textRenderer,Text.literal("_"),left+panelWidth-20,top+panelHeight-43,0x8CCC9C,false);
        // Faint stationary scan lines and a small status accent avoid severe flicker.
        for(int y=top+42;y<top+panelHeight-38;y+=3)d.fill(left+10,y,left+panelWidth-10,y+1,0x09000000);
        if(terminal.running()&&ticks%12<2)d.fill(left+panelWidth-3,top+42,left+panelWidth-1,top+54,0x606FC997);
        d.drawText(textRenderer,Text.literal(">"),left+12,top+panelHeight-26,0xADEDBD,false);
        super.render(d,mouseX,mouseY,delta);JiahaoSubtitleRenderer.render(d);
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext d,int mouseX,int mouseY,float delta){}
    @Override public void removed(){terminal.clear();java.util.Arrays.fill(lines,null);if(input!=null)input.setText("");super.removed();}
    public JiahaoCodeModel model(){return terminal;}
}
