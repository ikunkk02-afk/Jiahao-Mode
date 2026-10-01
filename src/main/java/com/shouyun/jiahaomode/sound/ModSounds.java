// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.sound;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
public final class ModSounds {
    public static final SoundEvent JIAHAO_MARCH=Registry.register(Registries.SOUND_EVENT,JiahaoMode.id("music.jiahao_march"),SoundEvent.of(JiahaoMode.id("music.jiahao_march")));
    public static final SoundEvent DISC_JIAHAO_MARCH=disc("jiahao_march");
    public static final SoundEvent DISC_NEVADA=disc("nevada");
    public static final SoundEvent DISC_SPECTRE=disc("spectre");
    private static SoundEvent disc(String song) {
        var id=JiahaoMode.id("music_disc."+song);
        return Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id));
    }
    private ModSounds() {}
    public static void initialize() {}
}
