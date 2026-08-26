package cool.muyucloud.croparia.fabric;

import cool.muyucloud.croparia.CropariaIf;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public class CropariaIfFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CropariaIf.init();
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, CropariaIf.of("elematilius_ore"))
        );
    }
}
