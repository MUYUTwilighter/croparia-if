package cool.muyucloud.croparia.api.repo;

import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.util.CifUtil;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ContainerRepo<C extends Container> implements Repo<ItemSpec> {
    private final C container;
    
    public ContainerRepo(C container) {
        this.container = container;
    }

    public C getContainer() {
        return container;
    }

    @Override
    public int size() {
        return this.getContainer().getContainerSize();
    }

    @Override
    public TypeToken<ItemSpec> getType() {
        return ItemSpec.TYPE;
    }

    @Override
    public boolean isEmpty(int i) {
        return this.getContainer().getItem(i).isEmpty();
    }

    @Override
    public ItemSpec resourceFor(int i) {
        return ItemSpec.of(this.getContainer().getItem(i));
    }

    @Override
    public long simConsume(int i, ItemSpec resource, long amount) {
        ItemStack stack = this.getContainer().getItem(i);
        if (!resource.is(stack)) {
            return 0;
        }
        long stored = stack.getCount();
        return Math.min(amount, stored);
    }

    @Override
    public long consume(int i, ItemSpec resource, long amount) {
        ItemStack stack = this.getContainer().getItem(i);
        if (!resource.is(stack)) {
            return 0;
        }
        int stored = stack.getCount();
        int consumed = CifUtil.toIntSafe(Math.min(amount, stored));
        stack.shrink(consumed);
        this.getContainer().setItem(i, stack);
        return consumed;
    }

    @Override
    public long simAccept(int i, ItemSpec resource, long amount) {
        // Ensure the amount does not exceed the slot's capacity
        long capacity = this.capacityFor(i, resource);
        long room = capacity - this.amountFor(i);
        amount = Math.min(amount, room);
        if (!this.getContainer().canPlaceItem(i, resource.createStack(amount))) {
            return 0;
        }
        ItemStack stored = this.getContainer().getItem(i);
        if (resource.is(stored) || stored.isEmpty()) {
            long accepted = Math.min(this.capacityFor(i, resource) - stored.getCount(), amount);
            return Math.max(accepted, 0);
        } else {
            return 0;
        }
    }

    @Override
    public long accept(int i, ItemSpec resource, long amount) {
        // Ensure the amount does not exceed the slot's capacity
        long capacity = this.capacityFor(i, resource);
        long room = capacity - this.amountFor(i);
        amount = Math.min(amount, room);
        if (!this.getContainer().canPlaceItem(i, resource.createStack(amount))) {
            return 0;
        }
        ItemStack stored = this.getContainer().getItem(i);
        if (resource.is(stored) || stored.isEmpty()) {
            long accepted = Math.min(this.capacityFor(i, resource) - stored.getCount(), amount);
            this.getContainer().setItem(i, resource.createStack(Math.max(accepted, 0) + stored.getCount()));
            return accepted;
        } else {
            return 0;
        }
    }

    @Override
    public long capacityFor(int i, ItemSpec resource) {
        ItemStack stored = this.getContainer().getItem(i);
        ItemStack toPlace = resource.createStack();
        int containerSize = this.getContainer().getMaxStackSize(toPlace);
        if (stored.isEmpty() || resource.is(stored)) {
            return containerSize;
        } else {
            return 0;
        }
    }

    @Override
    public long amountFor(int i, ItemSpec resource) {
        ItemStack stored = this.getContainer().getItem(i);
        if (resource.is(stored)) {
            return stored.getCount();
        } else {
            return 0;
        }
    }
}
