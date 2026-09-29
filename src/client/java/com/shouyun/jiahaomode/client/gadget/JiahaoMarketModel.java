// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.gadget;

import java.util.Random;

/** Entirely fictional, local random walk. No network, assets or persistent data. */
public final class JiahaoMarketModel {
    public static final int CAPACITY = 128;
    private final double[] prices = new double[CAPACITY];
    private final Random random;
    private final int name;
    private final double baseline, drift;
    private int start, size;
    public JiahaoMarketModel(Random random) {
        this.random=random; name=1+random.nextInt(6);
        baseline=50+random.nextDouble()*200;
        drift=(random.nextDouble()-.45)*.003;
        prices[size++]=baseline;
        for(int i=1;i<80;i++)append();
    }
    public void append() {
        double change=Math.max(-.025,Math.min(.025,drift+random.nextGaussian()*.008));
        double price=Math.max(.01,Math.min(999999,current()*(1+change)));
        if(size<CAPACITY)prices[(start+size++)%CAPACITY]=price;
        else {prices[start]=price;start=(start+1)%CAPACITY;}
    }
    public int size(){return size;}
    public int name(){return name;}
    public double get(int i){if(i<0||i>=size)throw new IndexOutOfBoundsException(i);return prices[(start+i)%CAPACITY];}
    public double current(){return get(size-1);}
    public double percent(){return (current()/baseline-1)*100;}
    public String buy(){return random.nextBoolean()?"order_executed":"position_acquired";}
    public String sell(){return "position_closed";}
}
