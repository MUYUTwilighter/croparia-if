package cool.muyucloud.croparia.api.repo;

import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Unified storage interface for {@link BlockEntity}.<br>
 * It is used to connect your customized {@link Repo} to the fluid API from fabric / forge.<br>
 * <p>
 * Use {@link #item(Repo)} or {@link #fluid(Repo)} to create a {@link RepoProxy}. <br>
 * <b>DO NOT INSTANTIATE THIS CLASS UNLESS YOU KNOW WHAT YOU ARE DOING</b>
 * </p>
 */
public abstract class RepoProxy<T extends TypedResource<?>> extends DelegateRepo<T> {
    /**
     * Create a fluid interface from your customized {@link Repo}. <br>
     * You should only create {@link RepoProxy} from this method,
     * and the implemented {@link RepoProxy} from fabric / forge module is returned.<br>
     *
     * @param repo the fluid repo
     * @return the fluid agent
     */
    @ExpectPlatform
    public static RepoProxy<FluidSpec> fluid(Repo<FluidSpec> repo) {
        throw new AssertionError("Not implemented");
    }

    @ExpectPlatform
    public static RepoProxy<ItemSpec> item(Repo<ItemSpec> repo) {
        throw new AssertionError("Not implemented");
    }

    protected RepoProxy(Repo<T> repo) {
        super(repo);
    }
}
