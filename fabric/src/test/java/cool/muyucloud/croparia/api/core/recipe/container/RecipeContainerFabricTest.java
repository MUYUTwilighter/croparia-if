package cool.muyucloud.croparia.api.core.recipe.container;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.entry.BlockOutput;
import cool.muyucloud.croparia.api.recipe.entry.ItemInput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeContainerFabricTest {
    private static ItemOutput decodedOutput;
    private static ItemInput decodedInput;
    private static ItemSpec decodedSpec;
    private static RitualRecipe decodedRitual;
    private static InfusorRecipe decodedInfusor;
    private static SoakRecipe decodedSoak;
    private static RitualStructure decodedStructure;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Recipe codecs are loaded by JEI/REI before the first resource reload binds item components.
        assertDoesNotThrow(() -> {
            assertNotNull(RitualRecipe.TYPED_SERIALIZER);
            assertNotNull(RitualStructure.TYPED_SERIALIZER);
            assertNotNull(SoakRecipe.TYPED_SERIALIZER);
            assertNotNull(InfusorRecipe.TYPED_SERIALIZER);
        });
        // Creating a world decodes actual recipes before the reload binds item components.
        assertThrows(NullPointerException.class, Items.APPLE::components);
        String output = """
            {"id":"minecraft:apple","amount":3,"components":{
              "minecraft:item_model":"croparia:diamond","!minecraft:custom_name":{}
            }}
            """;
        decodedOutput = decode(ItemOutput.CODEC, output);
        decodedSpec = decode(ItemSpec.CODEC, output);
        decodedInput = decode(ItemInput.CODEC, "\"minecraft:apple\"");
        assertEquals(3, decodedOutput.getAmount());
        assertEquals(decodedOutput.getComponentsPatch(), decodedSpec.getComponentsPatch());
        assertEquals(decodedSpec, decodedSpec.copy());
        assertEquals(decodedSpec.hashCode(), decodedSpec.copy().hashCode());
        assertTrue(decode(ItemOutput.CODEC, "\"minecraft:apple\"").getComponentsPatch().isEmpty());
        decodedRitual = decode(RitualRecipe.TYPED_SERIALIZER.codec().codec(), """
            {"ritual":"minecraft:stone","block":"minecraft:dirt","ingredient":"minecraft:apple","result":%s}
            """.formatted(output));
        decodedInfusor = decode(InfusorRecipe.TYPED_SERIALIZER.codec().codec(), """
            {"element":"air","ingredient":"minecraft:apple","result":%s}
            """.formatted(output));
        decodedSoak = decode(SoakRecipe.TYPED_SERIALIZER.codec().codec(), """
            {"element":"air","probability":1.0,"input":"minecraft:stone","output":{
              "id":"minecraft:chest","properties":{"facing":"east"},
              "components":{"minecraft:item_model":"croparia:diamond"}
            }}
            """);
        decodedStructure = decode(RitualStructure.TYPED_SERIALIZER.codec().codec(), """
            {"ritual":"minecraft:stone","keys":{},"pattern":[["   "," *$","   "]]}
            """);
        assertEquals(decodedOutput, ItemOutput.CODEC.parse(JsonOps.INSTANCE,
            ItemOutput.CODEC.encodeStart(JsonOps.INSTANCE, decodedOutput).getOrThrow()).getOrThrow());
        assertEquals(decodedSpec, ItemSpec.CODEC.parse(JsonOps.INSTANCE,
            ItemSpec.CODEC.encodeStart(JsonOps.INSTANCE, decodedSpec).getOrThrow()).getOrThrow());
        assertThrows(NullPointerException.class, Items.APPLE::components);

        // Unit tests have no resource reload to bind 26.1 item components.
        BuiltInRegistries.ITEM.listElements().forEach(holder -> holder.bindComponents(DataComponents.COMMON_ITEM_COMPONENTS));
        Items.APPLE.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
            .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
            .set(DataComponents.MAX_STACK_SIZE, 16)
            .set(DataComponents.CUSTOM_NAME, Component.literal("Default apple"))
            .build());
    }

    @Test
    void decodedRecipesUseBoundDefaultsAndPreserveOverrides() {
        Identifier model = Identifier.parse("croparia:diamond");
        ItemStack result = decodedOutput.createStack();
        assertTrue(result.is(Items.APPLE));
        assertEquals(3, result.getCount());
        assertEquals(16, result.getMaxStackSize());
        assertEquals(model, result.get(DataComponents.ITEM_MODEL));
        assertFalse(result.has(DataComponents.CUSTOM_NAME));
        assertTrue(ItemStack.isSameItemSameComponents(result, decodedOutput.getDisplayStacks().getFirst()));
        assertTrue(decodedSpec.is(result));
        assertFalse(decodedSpec.createStack().has(DataComponents.CUSTOM_NAME));
        assertTrue(ItemSpec.EMPTY.createStack().isEmpty());
        assertTrue(ItemOutput.EMPTY.getDisplayStacks().getFirst().isEmpty());

        ItemSpec edited = decodedSpec.with(DataComponents.MAX_STACK_SIZE, 8)
            .with(DataComponentMap.builder().set(DataComponents.DAMAGE, 2).build())
            .with(DataComponentPatch.builder().remove(DataComponents.DAMAGE).build());
        assertEquals(8, edited.createStack().getMaxStackSize());
        assertFalse(edited.createStack().has(DataComponents.CUSTOM_NAME));
        assertFalse(edited.createStack().has(DataComponents.DAMAGE));
        assertTrue(edited.with(Items.BREAD).createStack().is(Items.BREAD));
        assertEquals(16, decodedSpec.createStack().getMaxStackSize());

        assertEquals(16, decodedInput.getDisplayStacks().getFirst().getMaxStackSize());
        assertTrue(ItemStack.isSameItemSameComponents(result, decodedRitual.getOutputs().getFirst().getFirst()));
        assertTrue(ItemStack.isSameItemSameComponents(result, decodedInfusor.getOutputs().getFirst().getFirst()));
        assertEquals(Blocks.CHEST, decodedSoak.getOutput().getBlock());
        assertTrue(decodedSoak.getOutput().getComponents().test(
            DataComponentMap.builder().set(DataComponents.ITEM_MODEL, model).build()));
        assertEquals(Identifier.parse("minecraft:stone"), decodedStructure.getRitual().getId().orElseThrow());
    }

    private static <T> T decode(Codec<T> codec, String json) {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void recipeDisplayIconsWorkAfterComponentBinding() {
        ItemStack any = BlockInput.ANY.getDisplayStacks().getFirst();
        assertTrue(any.is(Items.LIGHT_GRAY_STAINED_GLASS_PANE));
        assertTrue(any.has(DataComponents.CUSTOM_NAME));
        assertTrue(BlockInput.STACK_UNKNOWN.get().is(Items.BEDROCK));
        assertTrue(BlockOutput.STACK_UNKNOWN.get().is(Items.BEDROCK));
        ItemStack air = BlockOutput.STACK_AIR.get();
        assertTrue(air.is(Items.BARRIER));
        assertTrue(air.has(DataComponents.CUSTOM_NAME));
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
        SoakContainer air = new SoakContainer(block(Blocks.AIR), Element.AIR, 1.0f);
        assertTrue(air.isEmpty());

        SoakContainer emptyElement = new SoakContainer(block(Blocks.STONE), Element.EMPTY, 1.0f);
        assertTrue(emptyElement.isEmpty());

        SoakContainer zeroRandom = new SoakContainer(block(Blocks.STONE), Element.AIR, 0.0f);
        assertTrue(zeroRandom.isEmpty());

        SoakContainer valid = new SoakContainer(block(Blocks.STONE), Element.AIR, 0.5f);
        assertFalse(valid.isEmpty());
        assertEquals(0, valid.size());
        assertSame(ItemStack.EMPTY, valid.getItem(0));
    }

    @Test
    void ritualContainerUsesMatchedResultAndStacks() {
        BlockInWorld ritual = block(Blocks.STONE);
        RitualStructure.Result success = RitualStructure.result(List.of(ritual), () -> {
        });
        RitualContainer successContainer = RitualContainer.of(
            ritual,
            List.of(new ItemStack(Items.APPLE)),
            success
        );
        assertFalse(successContainer.isEmpty());
        assertEquals(1, successContainer.size());
        assertSame(ItemStack.EMPTY, successContainer.getItem(5));

        RitualContainer failed = RitualContainer.of(
            ritual,
            List.of(new ItemStack(Items.APPLE)),
            RitualStructure.Result.FAIL
        );
        assertTrue(failed.isEmpty());

        RitualContainer noItems = RitualContainer.of(
            ritual,
            List.of(ItemStack.EMPTY),
            success
        );
        assertTrue(noItems.isEmpty());
    }

    private static BlockInWorld block(Block block) {
        return new BlockInWorld(null, BlockPos.ZERO, false) {
            @Override
            public BlockState getState() {
                return block.defaultBlockState();
            }
        };
    }
}
