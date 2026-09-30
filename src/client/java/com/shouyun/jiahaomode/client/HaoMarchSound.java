// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.sound.ModSounds;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;

/** One relative, resource-backed music instance. Never reads a system audio file. */
public final class HaoMarchSound extends MovingSoundInstance {
    HaoMarchSound() {
        super(ModSounds.JIAHAO_MARCH,SoundCategory.MUSIC,SoundInstance.createRandom());
        relative=true;attenuationType=AttenuationType.NONE;repeat=false;volume=.001f;
    }
    void volume(float next) { volume = next; }
    void finish() { setDone(); }
    @Override public void tick() { }
}
