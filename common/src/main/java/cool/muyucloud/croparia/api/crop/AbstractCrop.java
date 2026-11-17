package cool.muyucloud.croparia.api.crop;

import cool.muyucloud.croparia.api.crop.block.entity.CropEntity;
import cool.muyucloud.croparia.api.crop.util.CropRegistryAccess;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import cool.muyucloud.croparia.registry.CropariaComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class AbstractCrop<T> implements TranslatableEntry {
    public static String defaultTranslation(Identifier id) {
        String name = id.getPath();
        name = name.replace("_", " ").trim();
        StringBuilder builder = new StringBuilder();
        for (String token : name.split(" ")) {
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1)).append(" ");
        }
        return builder.toString().trim();
    }

    public abstract @NotNull Material<T> getMaterial();

    public abstract ItemStack getFruit();

    public static <C extends AbstractCrop<?>> Optional<C> readCrop(ItemStack stack) {
        Identifier cropId = stack.get(CropariaComponents.CROP_ID.get());
        if (cropId == null) return Optional.empty();
        Item item = stack.getItem();
        if (!(item instanceof CropRegistryAccess<?> access)) return Optional.empty();
        @SuppressWarnings("unchecked")
        C crop = (C) access.getRegistry().getById(cropId).orElse(null);
        return Optional.ofNullable(crop);
    }

    @SuppressWarnings("unchecked")
    public static <C extends AbstractCrop<?>> Optional<C> readCrop(BlockEntity be) {
        if (be instanceof CropEntity<?> cropBe) return (Optional<C>) cropBe.getCrop();
        else return Optional.empty();
    }

    public static <C extends AbstractCrop<?>> Optional<C> readCrop(Level level, BlockPos pos) {
        return readCrop(level.getBlockEntity(pos));
    }
}
