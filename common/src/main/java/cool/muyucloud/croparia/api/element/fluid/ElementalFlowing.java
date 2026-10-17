package cool.muyucloud.croparia.api.element.fluid;

import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class ElementalFlowing extends ArchitecturyFlowingFluid.Flowing implements ElementAccess {
    private static final Map<Element, ElementalFlowing> FLOWING_MAP = new HashMap<>();
    private final Element element;

    public ElementalFlowing(@NonNull Element element, @NonNull ArchitecturyFluidAttributes attributes) {
        super(attributes);
        this.element = this.assertEmpty(element);
        FLOWING_MAP.put(element, this);
    }

    @Override
    public @NonNull Element getElement() {
        return this.element;
    }

    @Nullable
    public static ElementalFlowing fromElement(@NonNull Element element) {
        return FLOWING_MAP.get(element);
    }
}
