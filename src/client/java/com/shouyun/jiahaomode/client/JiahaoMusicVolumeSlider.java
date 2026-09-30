// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/** Independent music preference, shown in the vanilla sound settings list. */
public final class JiahaoMusicVolumeSlider extends SliderWidget {
    public JiahaoMusicVolumeSlider() {
        super(0,0,310,20,Text.empty(),JiahaoClientConfig.musicVolume());
        updateMessage();
    }
    @Override protected void updateMessage() {
        setMessage(Text.translatable("options.jiahao-mode.music_volume",Math.round(value*100)));
    }
    @Override protected void applyValue() { JiahaoClientConfig.setMusicVolume(value); }
}
