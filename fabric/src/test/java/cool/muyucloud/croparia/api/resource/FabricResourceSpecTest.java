package cool.muyucloud.croparia.api.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FabricResourceSpecTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.ITEM.listElements().forEach(holder -> holder.bindComponents(DataComponents.COMMON_ITEM_COMPONENTS));
    }

    @Test
    void itemMatchingAndConversionsPreserveExplicitPatch() {
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:diamond"))
            .remove(DataComponents.CUSTOM_NAME)
            .build();
        ItemSpec spec = new ItemSpec(Items.APPLE, patch);
        ItemVariant variant = ItemVariant.of(Items.APPLE, patch);

        assertTrue(FabricItemSpec.matches(spec, variant));
        assertTrue(FabricItemSpec.matches(variant, spec));
        assertFalse(FabricItemSpec.matches(spec, ItemVariant.of(Items.BREAD, patch)));
        assertFalse(FabricItemSpec.matches(ItemVariant.of(Items.APPLE), spec));
        ItemVariant different = ItemVariant.of(Items.APPLE, DataComponentPatch.builder()
            .set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:emerald"))
            .remove(DataComponents.CUSTOM_NAME)
            .build());
        assertFalse(FabricItemSpec.matches(spec, different));
        assertFalse(FabricItemSpec.matches(different, spec));
        assertEquals(patch, FabricItemSpec.of(spec).getComponentsPatch());
        assertEquals(patch, FabricItemSpec.from(variant).getComponentsPatch());
        assertEquals(variant, FabricItemSpec.of(FabricItemSpec.from(variant)));
        assertTrue(FabricItemSpec.matches(ItemSpec.of(Items.APPLE), ItemVariant.of(Items.APPLE)));
    }

    @Test
    void itemSpecCodecPreservesBothFormatsAndRejectsUnknownIds() {
        JsonPrimitive plain = new JsonPrimitive("minecraft:apple");
        ItemSpec simple = ItemSpec.CODEC.parse(JsonOps.INSTANCE, plain).getOrThrow();
        assertSame(Items.APPLE, simple.getResource());
        assertEquals(plain, ItemSpec.CODEC.encodeStart(JsonOps.INSTANCE, simple).getOrThrow());

        assertTrue(Items.APPLE.components().has(DataComponents.RARITY));
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:diamond"))
            .remove(DataComponents.RARITY)
            .build();
        ItemSpec configured = new ItemSpec(Items.APPLE, patch);
        JsonObject object = ItemSpec.CODEC.encodeStart(JsonOps.INSTANCE, configured).getOrThrow().getAsJsonObject();
        assertEquals("minecraft:apple", object.get("id").getAsString());
        assertTrue(object.has("components"));
        assertEquals(configured, ItemSpec.CODEC.parse(JsonOps.INSTANCE, object).getOrThrow());
        var stack = configured.createStack(7);
        assertSame(Items.APPLE, stack.getItem());
        assertEquals(7, stack.getCount());
        assertEquals(patch, stack.getComponentsPatch());

        JsonPrimitive unknown = new JsonPrimitive("croparia_test:missing_item");
        assertTrue(ItemSpec.CODEC.parse(JsonOps.INSTANCE, unknown).isError());
        JsonObject missing = object.deepCopy();
        missing.add("id", unknown);
        assertTrue(ItemSpec.CODEC.parse(JsonOps.INSTANCE, missing).isError());
    }

    @Test
    void fluidMatchingAndConversionsPreserveComponentPatch() {
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:diamond"))
            .build();
        FluidSpec spec = new FluidSpec(Fluids.WATER, patch);
        FluidVariant variant = FluidVariant.of(Fluids.WATER, patch);

        assertTrue(FabricFluidSpec.matches(spec, variant));
        assertTrue(FabricFluidSpec.matches(variant, spec));
        assertFalse(FabricFluidSpec.matches(spec, FluidVariant.of(Fluids.LAVA, patch)));
        assertFalse(FabricFluidSpec.matches(FluidVariant.of(Fluids.WATER), spec));
        FluidVariant different = FluidVariant.of(Fluids.WATER, DataComponentPatch.builder()
            .set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:emerald"))
            .build());
        assertFalse(FabricFluidSpec.matches(spec, different));
        assertFalse(FabricFluidSpec.matches(different, spec));
        assertEquals(patch, FabricFluidSpec.toVariant(spec).getComponentsPatch());
        assertEquals(patch, FabricFluidSpec.from(variant).getComponentsPatch());
        assertEquals(variant, FabricFluidSpec.toVariant(FabricFluidSpec.from(variant)));
        assertTrue(FabricFluidSpec.matches(FluidSpec.of(Fluids.WATER), FluidVariant.of(Fluids.WATER)));
    }
}
