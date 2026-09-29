// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.quote;
import net.minecraft.util.Identifier;
import java.util.Optional;
public record JiahaoQuote(Identifier id, String translationKey, JiahaoQuoteCategory category,
 int priority, int displayTicks, Optional<Identifier> soundId, Optional<Identifier> animationId) { }
