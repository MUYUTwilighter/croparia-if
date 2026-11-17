package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.StateHolderAccess;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(StateHolder.class)
public abstract class StateHolderMixin<O, S> implements StateHolderAccess<S> {
    @Shadow
    public abstract <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V comparable);

    @Shadow
    @Final
    private Property<?>[] propertyKeys;
    @Shadow
    @Final
    private Comparable<?>[] propertyValues;

    @Override
    public Optional<Property<?>> cif$getProperty(String key) {
        for (Property<?> property : propertyKeys) {
            if (property.getName().equals(key)) {
                return Optional.of(property);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> cif$getValue(String key) {
        for (int i = 0; i < this.propertyKeys.length; i++) {
            if (this.propertyKeys[i].getName().equals(key)) {
                return Optional.of(this.propertyValues[i].toString());
            }
        }
        return Optional.empty();
    }

    @Unique
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> S cif$setValue(String key, String valName) {
        for (int i = 0; i < this.propertyKeys.length; i++) {
            Property<T> property = (Property<T>) this.propertyKeys[i];
            if (property.getName().equals(key)) {
                int finalI = i;
                property.getValue(valName).ifPresent(val -> this.propertyValues[finalI] = val);
                break;
            }
        }
        return (S) this;
    }

    @Override
    public <T extends Comparable<T>> Map<String, String> cif$getProperties() {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < this.propertyKeys.length; i++) {
            @SuppressWarnings("unchecked")
            Property<T> key = (Property<T>) this.propertyKeys[i];
            @SuppressWarnings("unchecked")
            T val = (T) this.propertyValues[i];
            String name = key.getName(val);
            map.put(key.getName(), name);
        }
        return map;
    }
}
