// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.gadget;

import java.util.Random;

/** Bounded text simulator. Input is never interpreted or passed to another subsystem. */
public final class JiahaoCodeModel {
    public static final int INPUT_LIMIT=128,HISTORY_LIMIT=20,OUTPUT_LIMIT=80;
    public record Row(String content,boolean translation){}
    private final Row[] output=new Row[OUTPUT_LIMIT];
    private final String[] history=new String[HISTORY_LIMIT];
    private final Random random;
    private int outputStart,outputSize,historyStart,historySize,elapsed,revision;
    private boolean running;
    public JiahaoCodeModel(Random random){this.random=random;for(int i=1;i<=4;i++)key("boot."+i);}
    private void append(Row row){if(outputSize<OUTPUT_LIMIT)output[(outputStart+outputSize++)%OUTPUT_LIMIT]=row;else{output[outputStart]=row;outputStart=(outputStart+1)%OUTPUT_LIMIT;}revision++;}
    private void key(String suffix){append(new Row("screen.jiahao-mode.code."+suffix,true));}
    public boolean submit(String input){
        if(input==null||input.isBlank()||running)return false;
        String text=input.substring(0,Math.min(INPUT_LIMIT,input.length()));
        if(historySize<HISTORY_LIMIT)history[(historyStart+historySize++)%HISTORY_LIMIT]=text;
        else{history[historyStart]=text;historyStart=(historyStart+1)%HISTORY_LIMIT;}
        append(new Row("> "+text,false));key("executing");elapsed=0;running=true;return true;
    }
    public void tick(){if(!running)return;elapsed++;if(elapsed%4==0&&elapsed<24)key("output."+(1+random.nextInt(9)));if(elapsed>=24){key("success");running=false;}}
    public int progress(){return running?elapsed*100/24:100;}
    public void appendMessage(String text){append(new Row(text,false));}
    public boolean running(){return running;}
    public int revision(){return revision;}
    public int outputSize(){return outputSize;}
    public int historySize(){return historySize;}
    public Row row(int i){if(i<0||i>=outputSize)throw new IndexOutOfBoundsException(i);return output[(outputStart+i)%OUTPUT_LIMIT];}
    public String history(int i){if(i<0||i>=historySize)throw new IndexOutOfBoundsException(i);return history[(historyStart+i)%HISTORY_LIMIT];}
    public void clear(){java.util.Arrays.fill(output,null);java.util.Arrays.fill(history,null);outputStart=outputSize=historyStart=historySize=0;running=false;revision++;}
}
