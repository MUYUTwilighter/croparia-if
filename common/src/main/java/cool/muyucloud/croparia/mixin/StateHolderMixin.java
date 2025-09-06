package cool.muyucloud.croparia.mixin;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.StateHolderAccess;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(StateHolder.class)
public abstract class StateHolderMixin<O, S> implements StateHolderAccess< S> {
    @Shadow
    @Final
    private Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;

    @Shadow
    public abstract <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V comparable);

    @Unique
    private Map<String, Property<?>> croparia_if$properties;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(@Nullable O object, Reference2ObjectArrayMap<Property<?>, Comparable<?>> map, @Nullable MapCodec<S> mapCodec, @Nullable CallbackInfo ci) {
        if (map == null) {
            this.croparia_if$properties = ImmutableMap.of();
            return;
        }
        Map<String, Property<?>> properties = new HashMap<>();
        for (Property<?> property : map.keySet()) {
            properties.put(property.getName(), property);
        }
        this.croparia_if$properties = ImmutableMap.copyOf(properties);
    }

    @Override
    public Property<?> cif$getProperty(String key) {
        return this.croparia_if$properties.get(key);
    }

    @Override
    public String cif$getValue(String key) {
        Property<?> property = this.cif$getProperty(key);
        Comparable<?> value = this.values.get(property);
        if (value == null) {
            return null;
        } else if (value instanceof StringRepresentable enumVal) {
            return enumVal.getSerializedName();
        } else {
            return value.toString();
        }
    }

    @Unique
    @Override
    @SuppressWarnings("unchecked")
    public <P extends Comparable<P>> S cif$setValue(String key, String value) {
        Property<P> property = (Property<P>) this.cif$getProperty(key);
        Class<P> cls = property.getValueClass();
        try {
            if (Integer.class.isAssignableFrom(cls)) {
                Property<Integer> intProp = (Property<Integer>) property;
                return setValue(intProp, Integer.parseInt(value));
            } else if (Boolean.class.isAssignableFrom(cls)) {
                Property<Boolean> boolProp = (Property<Boolean>) property;
                return setValue(boolProp, Boolean.parseBoolean(value));
            } else if (String.class.isAssignableFrom(cls)) {
                Property<String> stringProp = (Property<String>) property;
                return setValue(stringProp, value);
            } else if (StringRepresentable.class.isAssignableFrom(cls)) {
                for (P p : property.getPossibleValues()) {
                    StringRepresentable enumVal = (StringRepresentable) p;
                    if (enumVal.getSerializedName().equals(value)) {
                        return setValue(property, p);
                    }
                }
                return (S) this;
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (Throwable t) {
            CropariaIf.LOGGER.error("Failed to set property %s to %s".formatted(key, value), t);
            return (S) this;
        }
    }

    @Override
    public Map<String, String> cif$getProperties() {
        Map<String, String> map = new HashMap<>();
        for (String key : this.croparia_if$properties.keySet()) {
            map.put(key, this.cif$getValue(key));
        }
        return map;
    }
}
