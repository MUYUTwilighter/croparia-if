package cool.muyucloud.croparia.client;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.asset.CropBlockTintSource;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.client.command.ClientCommandRoot;
import cool.muyucloud.croparia.client.screen.CropTransmuterScreen;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.MenuTypes;
import cool.muyucloud.croparia.util.Ref;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class CropariaIfClient {
    public static final int STEM_YOUNG = 0x4CAF50;
    public static final int STEM_MATURE = 0xB0852A;

    public static void init() {
        CropariaIf.LOGGER.info("Initializing client setup");
        ClientCommandRoot.register();
        MenuScreenRegistry.registerScreenFactory(MenuTypes.CROP_TRANSMUTER.get(), CropTransmuterScreen::new);
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> PackHandler.forEach(
            pack -> pack.onClientStopping(Ref.of(client))
        ));
    }

    public static void registerBlockTintSources(BiConsumer<List<BlockTintSource>, Block[]> registrar) {
        BlockTintSource crop = new CropBlockTintSource();
        BlockTintSource stem = new BlockTintSource() {
            @Override
            public int color(@NotNull BlockState state) {
                float age = (float) state.getValue(StemBlock.AGE) / StemBlock.MAX_AGE;
                return ARGB.srgbLerp(age, ARGB.opaque(STEM_YOUNG), ARGB.opaque(STEM_MATURE));
            }

            @Override
            public @NotNull Set<Property<?>> relevantProperties() {
                return Set.of(StemBlock.AGE);
            }
        };
        registrar.accept(List.of(crop), new Block[]{CropariaBlocks.CROP_BLOCK.get(), CropariaBlocks.MELON_BLOCK.get()});
        // stem_growth*: 0 = aging stem, 1 = crop-colored overlay.
        registrar.accept(List.of(stem, crop), new Block[]{CropariaBlocks.MELON_STEM.get()});
        // stem_fruit: 0 = crop-colored overlay, 1 = mature stem.
        registrar.accept(List.of(crop, BlockTintSources.constant(ARGB.opaque(STEM_MATURE))), new Block[]{CropariaBlocks.MELON_ATTACH.get()});
    }
}
