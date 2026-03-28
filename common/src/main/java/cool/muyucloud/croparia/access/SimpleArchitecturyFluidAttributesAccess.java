package cool.muyucloud.croparia.access;

import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;

public interface SimpleArchitecturyFluidAttributesAccess {
    Component cif$getName();

    SimpleArchitecturyFluidAttributesAccess cif$setName(Component name);

    static SimpleArchitecturyFluidAttributes setName(SimpleArchitecturyFluidAttributes instance, Component name) {
        SimpleArchitecturyFluidAttributesAccess access = (SimpleArchitecturyFluidAttributesAccess) instance;
        return (SimpleArchitecturyFluidAttributes) access.cif$setName(name);
    }

    static Component getName(SimpleArchitecturyFluidAttributes instance) {
        SimpleArchitecturyFluidAttributesAccess access = (SimpleArchitecturyFluidAttributesAccess) instance;
        return access.cif$getName();
    }
}
