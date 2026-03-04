package cool.muyucloud.croparia.api.repo.fabric;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.platform.PlatformItemProxy;
import cool.muyucloud.croparia.api.resource.FabricItemSpec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;

public class PlatformItemProxyImpl extends AbstractFabricStorageProxy<ItemSpec, ItemVariant> implements PlatformItemProxy {
    @NotNull
    public static PlatformItemProxyImpl of(@NotNull Storage<ItemVariant> storage) {
        return new PlatformItemProxyImpl(storage);
    }

    public PlatformItemProxyImpl(Storage<ItemVariant> storage) {
        super(storage);
    }

    @Override
    protected ItemSpec fromVariant(ItemVariant variant) {
        return FabricItemSpec.from(variant);
    }

    @Override
    protected ItemVariant toVariant(ItemSpec resource) {
        return FabricItemSpec.of(resource);
    }

    @Override
    protected boolean matches(ItemVariant variant, ItemSpec resource) {
        return FabricItemSpec.matches(variant, resource);
    }

    @Override
    public Optional<Repo<ItemSpec>> peel() {
        return super.peel();
    }
}
