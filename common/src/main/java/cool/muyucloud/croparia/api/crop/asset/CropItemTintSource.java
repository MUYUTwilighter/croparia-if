package cool.muyucloud.croparia.api.crop.asset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.util.CropRegistryAccess;
import cool.muyucloud.croparia.registry.CropariaComponents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class CropItemTintSource implements ItemTintSource {
    public static final Codec<ColorProcess> PROCESS_CODEC = Codec.STRING.xmap(s -> ColorProcess.valueOf(s.toUpperCase(Locale.ROOT)), ColorProcess::getName);
    public static final MapCodec<CropItemTintSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        PROCESS_CODEC.optionalFieldOf("process", ColorProcess.KEEP).forGetter(CropItemTintSource::getProcess)
    ).apply(instance, CropItemTintSource::new));

    private final @NotNull ColorProcess process;

    public CropItemTintSource(@NotNull ColorProcess color) {
        this.process = color;
    }

    @Override
    public int calculate(@NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (!(stack.getItem() instanceof CropRegistryAccess<?> access)) return -1;
        Identifier mayId = stack.get(CropariaComponents.CROP_ID.get());
        if (mayId == null) return -1;
        Optional<? extends AbstractCrop<?>> mayCrop = access.getRegistry().getById(mayId);
        if (mayCrop.isEmpty()) return -1;
        AbstractCrop<?> crop = mayCrop.get();
        if (!(crop instanceof ColorAccess colorAccess)) return -1;
        int val = colorAccess.getColor().getValue();
        return this.getProcess().process(val);
    }

    public @NotNull ColorProcess getProcess() {
        return process;
    }

    @Override
    public @NotNull MapCodec<CropItemTintSource> type() {
        return CODEC;
    }

    public enum ColorProcess {
        KEEP("keep", in -> in),
        ZERO_ALPHA("zero_alpha", in -> in & 0x00FFFFFF),
        FULL_ALPHA("full_alpha", in -> in | 0xFF000000);

        private final @NotNull String name;
        private final @NotNull Function<Integer, Integer> process;

        ColorProcess(String name, Function<Integer, Integer> process) {
            this.name = name;
            this.process = process;
        }

        public @NotNull String getName() {
            return name;
        }

        public int process(int color) {
            return process.apply(color);
        }
    }
}
