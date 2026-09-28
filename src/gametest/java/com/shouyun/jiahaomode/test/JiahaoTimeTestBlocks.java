// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Deterministic random-tick probe, registered only in the isolated test mod. */
public final class JiahaoTimeTestBlocks implements ModInitializer {
	static long randomTicks;
	static final Block PROBE = new Block(AbstractBlock.Settings.create().ticksRandomly()) {
		@Override protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) { JiahaoTimeTestBlocks.randomTicks++; }
	};
	@Override public void onInitialize() { Registry.register(Registries.BLOCK, Identifier.of("jiahao-mode-test", "random_probe"), PROBE); }
}
