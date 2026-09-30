// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.shouyun.jiahaomode.sound.ModSounds;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;

/** One relative, resource-backed music instance. Never reads a system audio file. */
public final class HaoMarchSound extends MovingSoundInstance {
    HaoMarchSound() {
        // MASTER bypasses vanilla category sliders; the listener still applies global volume.
        super(ModSounds.JIAHAO_MARCH,SoundCategory.MASTER,SoundInstance.createRandom());
        relative=true;attenuationType=AttenuationType.NONE;repeat=false;volume=.001f;
    }
    void volume(float next) { volume = next; }
    void finish() { setDone(); }
    @Override public float getVolume() { return super.getVolume()*(float)JiahaoClientConfig.musicVolume(); }
    @Override public boolean shouldAlwaysPlay() { return true; }
    @Override public void tick() { }
}
