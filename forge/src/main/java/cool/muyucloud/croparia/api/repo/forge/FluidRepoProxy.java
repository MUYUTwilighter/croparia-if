package cool.muyucloud.croparia.api.repo.forge;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.forge.ForgeFluidSpec;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import cool.muyucloud.croparia.util.CifUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class FluidRepoProxy extends RepoProxy<FluidSpec> implements IFluidHandler {
    public FluidRepoProxy(Repo<FluidSpec> repo) {
        super(repo);
    }

    @Override
    public int getTanks() {
        return this.get().size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int i) {
        FluidSpec fluidSpec = this.resourceFor(i);
        return ForgeFluidSpec.of(fluidSpec, this.amountFor(i, fluidSpec));
    }

    @Override
    public int getTankCapacity(int i) {
        return CifUtil.toIntSafe(this.capacityFor(i) / ForgeFluidSpec.FORGE_TO_INTERNAL_RATIO);
    }

    @Override
    public boolean isFluidValid(int i, @NotNull FluidStack input) {
        FluidSpec fluid = ForgeFluidSpec.from(input);
        long amount = ForgeFluidSpec.toInternalAmount(input.getAmount());
        return this.simAccept(i, fluid, amount) >= amount;
    }

    @Override
    public int fill(@NotNull FluidStack input, FluidAction fluidAction) {
        FluidSpec fluid = ForgeFluidSpec.from(input);
        if (fluidAction.simulate()) {
            return CifUtil.toIntSafe(this.simAccept(fluid, ForgeFluidSpec.toInternalAmount(input.getAmount())) / ForgeFluidSpec.FORGE_TO_INTERNAL_RATIO);
        } else if (fluidAction.execute()) {
            return CifUtil.toIntSafe(this.accept(fluid, ForgeFluidSpec.toInternalAmount(input.getAmount())) / ForgeFluidSpec.FORGE_TO_INTERNAL_RATIO);
        } else {
            return 0;
        }
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack input, FluidAction fluidAction) {
        FluidSpec fluid = ForgeFluidSpec.from(input);
        if (fluidAction.simulate()) {
            long consumed = this.simConsume(ForgeFluidSpec.from(input), ForgeFluidSpec.toInternalAmount(input.getAmount()));
            return ForgeFluidSpec.of(fluid, consumed);
        } else if (fluidAction.execute()) {
            long consumed = this.consume(ForgeFluidSpec.from(input), ForgeFluidSpec.toInternalAmount(input.getAmount()));
            return ForgeFluidSpec.of(fluid, consumed);
        } else {
            return FluidStack.EMPTY;
        }
    }

    @Override
    public @NotNull FluidStack drain(int amount, @NotNull FluidAction fluidAction) {
        if (this.size() < 1) return FluidStack.EMPTY;
        FluidSpec fluid = this.resourceFor(0);
        if (fluidAction.simulate()) {
            long consumed = this.simConsume(fluid, ForgeFluidSpec.toInternalAmount(amount));
            return ForgeFluidSpec.of(fluid, consumed);
        } else if (fluidAction.execute()) {
            long consumed = this.consume(fluid, ForgeFluidSpec.toInternalAmount(amount));
            return ForgeFluidSpec.of(fluid, consumed);
        } else {
            return FluidStack.EMPTY;
        }
    }
}
