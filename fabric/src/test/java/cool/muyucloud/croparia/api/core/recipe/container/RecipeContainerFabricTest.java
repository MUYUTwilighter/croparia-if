package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.element.Element;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeContainerFabricTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void infusorContainerHandlesBoundsAndEmptyState() {
        InfusorContainer container = new InfusorContainer(Element.AIR, List.of(ItemStack.EMPTY, new ItemStack(Items.APPLE)));

        assertEquals(2, container.size());
        assertFalse(container.isEmpty());
        assertSame(ItemStack.EMPTY, container.getItem(10));
        int count = 0;
        for (ItemStack ignored : container) {
            count++;
        }
        assertEquals(2, count);

        InfusorContainer allEmpty = new InfusorContainer(Element.AIR, List.of(ItemStack.EMPTY));
        assertTrue(allEmpty.isEmpty());
    }

    @Test
    void soakContainerEmptyDependsOnStateElementAndRandom() {
        SoakContainer air = new SoakContainer(Blocks.AIR.defaultBlockState(), Element.AIR, 1.0f);
        assertTrue(air.isEmpty());

        SoakContainer emptyElement = new SoakContainer(Blocks.STONE.defaultBlockState(), Element.EMPTY, 1.0f);
        assertTrue(emptyElement.isEmpty());

        SoakContainer zeroRandom = new SoakContainer(Blocks.STONE.defaultBlockState(), Element.AIR, 0.0f);
        assertTrue(zeroRandom.isEmpty());

        SoakContainer valid = new SoakContainer(Blocks.STONE.defaultBlockState(), Element.AIR, 0.5f);
        assertFalse(valid.isEmpty());
        assertEquals(0, valid.size());
        assertSame(ItemStack.EMPTY, valid.getItem(0));
    }

    @Test
    void ritualContainerUsesMatchedResultAndStacks() {
        RitualStructure.Result success = RitualStructure.result(List.of(Blocks.STONE.defaultBlockState()), () -> {
        });
        RitualContainer successContainer = RitualContainer.of(
            Blocks.STONE.defaultBlockState(),
            List.of(new ItemStack(Items.APPLE)),
            success
        );
        assertFalse(successContainer.isEmpty());
        assertEquals(1, successContainer.size());
        assertSame(ItemStack.EMPTY, successContainer.getItem(5));

        RitualContainer failed = RitualContainer.of(
            Blocks.STONE.defaultBlockState(),
            List.of(new ItemStack(Items.APPLE)),
            RitualStructure.Result.FAIL
        );
        assertTrue(failed.isEmpty());

        RitualContainer noItems = RitualContainer.of(
            Blocks.STONE.defaultBlockState(),
            List.of(ItemStack.EMPTY),
            success
        );
        assertTrue(noItems.isEmpty());
    }
}
