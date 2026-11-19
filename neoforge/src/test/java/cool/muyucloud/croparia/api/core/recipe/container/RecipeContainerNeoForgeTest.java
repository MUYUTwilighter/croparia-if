package cool.muyucloud.croparia.api.core.recipe.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeContainerNeoForgeTest {
    @Test
    void infusorContainerHandlesBoundsAndEmptyState() {
        Assumptions.assumeTrue(runtimeReady(), "Requires initialized NeoForge runtime");
        InfusorContainer container = new InfusorContainer(null, List.of(ItemStack.EMPTY, ItemStack.EMPTY));

        assertEquals(2, container.size());
        assertTrue(container.isEmpty());
        assertSame(ItemStack.EMPTY, container.getItem(10));
    }

    @Test
    void soakContainerAirBranchIsEmpty() {
        Assumptions.assumeTrue(runtimeReady(), "Requires initialized NeoForge runtime");
        BlockInWorld block = new BlockInWorld(null, BlockPos.ZERO, false) {
            @Override
            public BlockState getState() {
                return Blocks.AIR.defaultBlockState();
            }
        };
        SoakContainer air = new SoakContainer(block, null, 1.0f);
        assertTrue(air.isEmpty());
        assertEquals(0, air.size());
        assertSame(ItemStack.EMPTY, air.getItem(0));
    }

    private static boolean runtimeReady() {
        try {
            return dev.architectury.platform.Platform.getGameFolder() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
