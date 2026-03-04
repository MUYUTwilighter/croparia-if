package cool.muyucloud.croparia.api.repo.fabric;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.resource.FabricItemSpec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

public class ItemRepoProxy extends AbstractFabricRepoProxy<ItemSpec, ItemVariant> {
    public ItemRepoProxy(Repo<ItemSpec> repo) {
        super(repo);
    }

    @Override
    protected ItemSpec fromVariant(ItemVariant variant) {
        return FabricItemSpec.from(variant);
    }

    @Override
    protected ItemVariant toVariant(ItemSpec resource) {
        return FabricItemSpec.of(resource);
    }
}
