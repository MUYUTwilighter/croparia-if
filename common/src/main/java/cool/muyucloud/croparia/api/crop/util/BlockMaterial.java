package cool.muyucloud.croparia.api.crop.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BlockMaterial extends Material<Block> {
    public static final Codec<BlockMaterial> CODEC_STR = Codec.STRING.xmap(BlockMaterial::new, BlockMaterial::getName);
    public static final MapCodec<BlockMaterial> CODEC_COMP = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(BlockMaterial::getName),
            Codec.INT.optionalFieldOf("count", 2).forGetter(BlockMaterial::getCount)
        ).apply(instance, BlockMaterial::new)
    );
    public static final MultiCodec<BlockMaterial> CODEC = CodecUtil.of(
        CodecUtil.of(CODEC_COMP.codec(), toDecode -> toDecode.getCount() == 2
            ? TestedCodec.fail(() -> "Default amount, proceed to string codec") : TestedCodec.success()), CODEC_STR
    );
    public static final Placeholder<BlockMaterial> PLACEHOLDER = Placeholder.build(node -> node
        .self(TypeMapper.identity(), BlockMaterial.CODEC)
        .then(PatternKey.literal("result"), TypeMapper.of(material -> ItemOutput.of(material.asItem())), Placeholder.ITEM_OUTPUT)
        .concat(Material.PLACEHOLDER, TypeMapper.of(material -> material)));

    private transient final OnLoadSupplier<List<Block>> blocks = OnLoadSupplier.of(
        () -> {
            List<Block> result = this.candidates(BuiltInRegistries.BLOCK.key().location()).stream().filter(block -> {
                boolean blacklist = CropariaIf.CONFIG.isModValid(Objects.requireNonNull(block.arch$registryName()).getNamespace());
                boolean hasItem = block.asItem() instanceof BlockItem;
                return blacklist & hasItem;
            }).toList();
            return result.isEmpty() ? List.of(Blocks.AIR) : result;
        }
    );

    public static ResourceLocation parse(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock().arch$registryName();
        } else {
            throw new IllegalArgumentException("Not a block item: " + item.arch$registryName());
        }
    }

    public BlockMaterial(@NotNull ItemStack stack) {
        super(parse(stack).toString(), stack.getCount());
    }

    public BlockMaterial(@NotNull String name) {
        super(name, 2);
    }

    public BlockMaterial(@NotNull String name, int count) {
        super(name, count);
    }

    @Override
    public List<Block> candidates() {
        return blocks.get();
    }

    @Override
    public ItemStack asItem() {
        ItemStack stack =  this.candidates().getFirst().asItem().getDefaultInstance();
        stack.setCount(this.getCount());
        return stack;
    }

    @Override
    public boolean isEmpty() {
        return this.asItem().isEmpty();
    }
}
