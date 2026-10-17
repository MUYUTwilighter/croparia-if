package cool.muyucloud.croparia.api.resource.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import cool.muyucloud.croparia.util.TagUtil;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("unused")
public class FluidSpec implements TypedResource<Fluid>, DataComponentHolder {
    public static final MapCodec<FluidSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(fluidSpec -> fluidSpec.getResource().arch$registryName()),
        DataComponentMap.CODEC.fieldOf("components").forGetter(FluidSpec::getComponents)
    ).apply(instance, (id, nbt) -> new FluidSpec(BuiltInRegistries.FLUID.getValue(id), nbt)));
    public static final FluidSpec EMPTY = FluidSpec.of(Fluids.EMPTY);
    public static final TypeToken<FluidSpec> TYPE = TypeToken.register(CropariaIf.of("fluid_spec"), EMPTY, CODEC).orElseThrow();

    @NonNull
    public static FluidSpec of(@NonNull Fluid fluid) {
        return new FluidSpec(fluid, DataComponentMap.EMPTY);
    }

    @NonNull
    public static FluidSpec of(@NonNull Fluid fluid, @Nullable DataComponentMap nbt) {
        return new FluidSpec(fluid, nbt == null ? DataComponentMap.EMPTY : nbt);
    }

    @NonNull
    private final Fluid resource;
    @NonNull
    private final PatchedDataComponentMap components;

    public FluidSpec(@NonNull Fluid fluid) {
        this(fluid, DataComponentPatch.EMPTY);
    }

    public FluidSpec(@NonNull Fluid fluid, @NonNull DataComponentPatch components) {
        this.resource = fluid;
        this.components = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, components);
    }

    public FluidSpec(@NonNull Fluid fluid, @NonNull DataComponentMap components) {
        this.resource = fluid;
        this.components = new PatchedDataComponentMap(components);
    }

    @NonNull
    public FluidSpec copy() {
        return new FluidSpec(this.getResource(), this.getComponentsPatch());
    }

    @NonNull
    public FluidSpec with(@NonNull Fluid fluid) {
        return new FluidSpec(fluid, this.getComponents());
    }

    @NonNull
    public FluidSpec with(@NonNull DataComponentPatch components) {
        PatchedDataComponentMap patched = new PatchedDataComponentMap(this.getComponents());
        patched.applyPatch(components);
        return new FluidSpec(this.getResource(), patched);
    }

    @NonNull
    public FluidSpec with(@NonNull DataComponentMap components) {
        PatchedDataComponentMap patched = new PatchedDataComponentMap(this.getComponents());
        patched.setAll(components);
        return new FluidSpec(this.getResource(), patched);
    }

    @NonNull
    public FluidSpec with(@NonNull TypedDataComponent<?> component) {
        PatchedDataComponentMap patched = new PatchedDataComponentMap(this.getComponents());
        component.applyTo(patched);
        return new FluidSpec(this.getResource(), patched);
    }

    @NonNull
    public <T> FluidSpec with(@NonNull DataComponentType<T> type, @NonNull T value) {
        return this.with(new TypedDataComponent<>(type, value));
    }

    @NonNull
    public FluidSpec replaceComponents(@NonNull DataComponentMap components) {
        return new FluidSpec(this.getResource(), components);
    }

    public boolean is(@NonNull FluidSpec spec) {
        return this.getResource() == spec.getResource() && this.getComponents().equals(spec.getComponents());
    }

    public boolean is(@NonNull Identifier tag) {
        return TagUtil.isIn(Registries.FLUID, tag, this.getResource());
    }

    public FluidStack toStack(long amount) {
        return FluidStack.create(this.getResource(), amount, this.getComponentsPatch());
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
    @NonNull
    public Fluid getResource() {
        return this.resource;
    }

    @Override
    @NonNull
    public DataComponentMap getComponents() {
        return this.components;
    }

    @NonNull
    public DataComponentPatch getComponentsPatch() {
        return this.components.asPatch();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FluidSpec fluidSpec)) return false;
        if (this.isEmpty()) return fluidSpec.isEmpty();
        return Objects.equals(resource, fluidSpec.resource) && Objects.equals(components, fluidSpec.components);
    }

    @Override
    public int hashCode() {
        if (this.isEmpty()) return 0;
        return Objects.hash(resource, components);
    }
}
