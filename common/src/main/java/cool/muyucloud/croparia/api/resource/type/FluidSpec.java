package cool.muyucloud.croparia.api.resource.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import cool.muyucloud.croparia.util.TagUtil;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class FluidSpec implements TypedResource<Fluid> {
    public static final MapCodec<FluidSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(fluid -> fluid.getResource().arch$registryName()),
        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(FluidSpec::getNbt)
    ).apply(instance, (id, nbt) -> new FluidSpec(BuiltInRegistries.FLUID.get(id), nbt.orElse(null))));
    public static final FluidSpec EMPTY = new FluidSpec(Fluids.EMPTY, null);
    public static final TypeToken<FluidSpec> TYPE = TypeToken.register(CropariaIf.of("fluid_spec"), EMPTY, CODEC).orElseThrow();

    @NotNull
    private final Fluid resource;
    @Nullable
    private final CompoundTag tag;

    @NotNull
    public static FluidSpec of(@NotNull Fluid fluid) {
        return new FluidSpec(fluid, null);
    }

    @NotNull
    public static FluidSpec of(@NotNull Fluid fluid, @Nullable CompoundTag nbt) {
        return new FluidSpec(fluid, nbt);
    }

    public FluidSpec(@NotNull Fluid fluid, @Nullable CompoundTag nbt) {
        this.resource = fluid;
        this.tag = nbt == null ? null : nbt.copy();
    }

    @NotNull
    public FluidSpec copy() {
        return new FluidSpec(this.getResource(), this.tag);
    }

    @NotNull
    public FluidSpec with(@NotNull Fluid fluid) {
        return new FluidSpec(fluid, this.tag);
    }

    @NotNull
    public FluidSpec replaceNbt(@Nullable CompoundTag nbt) {
        return new FluidSpec(this.getResource(), nbt);
    }

    public boolean is(@NotNull FluidSpec spec) {
        return this.getResource() == spec.getResource() && Objects.equals(this.tag, spec.tag);
    }

    public boolean is(@NotNull ResourceLocation tag) {
        return TagUtil.isIn(Registries.FLUID, tag, this.getResource());
    }

    public FluidStack toStack(long amount) {
        return FluidStack.create(this.getResource(), amount, this.tag == null ? null : this.tag.copy());
    }

    @Override
    public TypeToken<FluidSpec> getType() {
        return TYPE;
    }

    @Override
    public MapCodec<FluidSpec> getCodec() {
        return CODEC;
    }

    @Override
    @NotNull
    public Fluid getResource() {
        return this.resource;
    }

    public Optional<CompoundTag> getNbt() {
        return Optional.ofNullable(this.tag == null ? null : this.tag.copy());
    }

    @NotNull
    public CompoundTag getTagOrEmpty() {
        return this.tag == null ? new CompoundTag() : this.tag.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FluidSpec fluidSpec)) return false;
        if (this.isEmpty()) return fluidSpec.isEmpty();
        return Objects.equals(resource, fluidSpec.resource) && Objects.equals(tag, fluidSpec.tag);
    }

    @Override
    public int hashCode() {
        if (this.isEmpty()) return 0;
        return Objects.hash(resource, tag);
    }
}
