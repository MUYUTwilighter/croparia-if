package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.block.entity.ActivatedShriekerBlockEntity;
import cool.muyucloud.croparia.api.core.block.entity.GreenhouseBlockEntity;
import cool.muyucloud.croparia.api.core.block.entity.MaterialExtractorBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(CropariaIf.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<GreenhouseBlockEntity>> GREENHOUSE_BE = register(
        "greenhouse",
        GreenhouseBlockEntity::new,
        Set.of(CropariaBlocks.GREENHOUSE)
    );
    public static final RegistrySupplier<BlockEntityType<MaterialExtractorBlockEntity>> MATERIAL_EXTRACTOR = register(
        "material_extractor",
        MaterialExtractorBlockEntity::new,
        Set.of(CropariaBlocks.MATERIAL_EXTRACTOR)
    );
    public static final RegistrySupplier<BlockEntityType<ActivatedShriekerBlockEntity>> ACTIVATED_SHRIEKER = register(
        "activated_shrieker",
        ActivatedShriekerBlockEntity::new,
        Set.of(CropariaBlocks.ACTIVATED_SHRIEKER)
    );

    @NotNull
    public static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> register(
        @NotNull String name,
        @NotNull BlockEntityType.BlockEntitySupplier<? extends T> factory,
        @NotNull Set<? extends Supplier<? extends Block>> validBlocks
        ) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(
            factory, validBlocks.stream().map(Supplier::get).collect(Collectors.toSet()), Util.fetchChoiceType(References.BLOCK_ENTITY, CropariaIf.of(name).toString())
        ));
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registering block entities");
        BLOCK_ENTITIES.register();
    }
}
