// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.mixin;

import com.shouyun.jiahaomode.client.HaoMarchSound;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMusicMixin {
    // Vanilla stops every zero-volume source when any category slider changes.
    // Keep our muted voice running so raising its own slider resumes the same position.
    @Redirect(method="updateSoundVolume",at=@At(value="INVOKE",target="Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private void jiahao$keepMutedMusic(Map<SoundInstance,Channel.SourceManager> sources,
                                      BiConsumer<SoundInstance,Channel.SourceManager> vanilla) {
        sources.forEach((sound,manager)->{
            if(sound instanceof HaoMarchSound) {
                float volume=MathHelper.clamp(sound.getVolume(),0,1);
                manager.run(source->source.setVolume(volume));
            } else vanilla.accept(sound,manager);
        });
    }
}
