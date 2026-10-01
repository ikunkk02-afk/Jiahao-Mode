// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.test;

import com.shouyun.jiahaomode.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Blocks;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public final class JiahaoMusicDiscTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="jiahao_music_discs")
    public void allDiscsInsertPlayStopAndEject(TestContext c) {
        var player=c.createMockPlayer(GameMode.SURVIVAL);
        Item[] discs={ModItems.MUSIC_DISC_JIAHAO_MARCH,ModItems.MUSIC_DISC_NEVADA,ModItems.MUSIC_DISC_SPECTRE};
        float[] seconds={162,208.562f,230.635f};
        var relative=new BlockPos(1,1,1);var pos=c.getAbsolutePos(relative);var world=c.getWorld();
        for(int i=0;i<discs.length;i++) {
            c.setBlockState(relative,Blocks.JUKEBOX);
            var box=(JukeboxBlockEntity)world.getBlockEntity(pos);var stack=new ItemStack(discs[i]);
            c.assertTrue(stack.getMaxCount()==1&&stack.isIn(ConventionalItemTags.MUSIC_DISCS),"Disc is unstackable and tagged");
            var song=JukeboxSong.getSongEntryFromStack(world.getRegistryManager(),stack).orElseThrow();
            c.assertTrue(Math.abs(song.value().lengthInSeconds()-seconds[i])<.01,"Song duration matches imported audio");
            c.assertTrue(box.isValid(0,stack),"Vanilla inventory accepts custom disc");
            c.assertTrue(JukeboxPlayableComponent.tryPlayStack(world,pos,stack,player).isAccepted(),"Vanilla insertion succeeds");
            c.assertTrue(stack.isEmpty()&&world.getBlockState(pos).get(JukeboxBlock.HAS_RECORD),"Survival insertion moves one disc into box");
            c.assertTrue(box.getManager().isPlaying()&&box.getComparatorOutput()==13+i,"Song plays with comparator output");
            c.assertTrue(!box.isValid(0,new ItemStack(discs[i])),"Occupied box refuses another disc");
            // Advance directly to the native stop boundary rather than waiting several minutes.
            box.getManager().setValues(song,song.value().getLengthInTicks()+19);
            box.getManager().tick(world,box.getCachedState());
            c.assertTrue(box.getManager().isPlaying(),"Song remains active before native end boundary");
            box.getManager().tick(world,box.getCachedState());
            c.assertTrue(!box.getManager().isPlaying()&&box.getStack().isOf(discs[i]),"Song ends without consuming the disc");
            box.dropRecord();
            c.assertTrue(box.getStack().isEmpty()&&!world.getBlockState(pos).get(JukeboxBlock.HAS_RECORD),"Ejection clears box");
        }
        c.complete();
    }
}
