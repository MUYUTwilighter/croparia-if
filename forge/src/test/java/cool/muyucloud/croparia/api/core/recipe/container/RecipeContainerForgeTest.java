package cool.muyucloud.croparia.api.core.recipe.container;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeContainerForgeTest {
    @Test
    void infusorContainerHandlesBoundsAndEmptyState() {
        Assumptions.assumeTrue(runtimeReady(), "Requires initialized Forge runtime");
        InfusorContainer container = new InfusorContainer(null, List.of(ItemStack.EMPTY, ItemStack.EMPTY));

        assertEquals(2, container.getContainerSize());
        assertTrue(container.isEmpty());
        assertSame(ItemStack.EMPTY, container.getItem(10));
    }

    @Test
    void soakContainerAirBranchIsEmpty() {
        Assumptions.assumeTrue(runtimeReady(), "Requires initialized Forge runtime");
        SoakContainer air = new SoakContainer(Blocks.AIR.defaultBlockState(), null, 1.0f);
        assertTrue(air.isEmpty());
        assertEquals(0, air.getContainerSize());
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
