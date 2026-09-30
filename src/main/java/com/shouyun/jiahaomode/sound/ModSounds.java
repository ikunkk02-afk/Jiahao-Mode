// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.sound;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
public final class ModSounds {
    public static final SoundEvent JIAHAO_MARCH=Registry.register(Registries.SOUND_EVENT,JiahaoMode.id("music.jiahao_march"),SoundEvent.of(JiahaoMode.id("music.jiahao_march")));
    private ModSounds() {}
    public static void initialize() {}
}
