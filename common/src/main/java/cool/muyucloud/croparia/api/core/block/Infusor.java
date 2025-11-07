package cool.muyucloud.croparia.api.core.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.item.RecipeWizard;
import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.core.recipe.container.InfusorContainer;
import cool.muyucloud.croparia.api.core.util.DropsCache;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.item.ElementalPotion;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.ItemPlaceable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Infusor extends Block implements ItemPlaceable {
    protected final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    public static final EnumProperty<Element> ELEMENT = EnumProperty.create("element", Element.class);

    public Infusor(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(ELEMENT, Element.EMPTY));
    }

    @Override
    protected @NotNull InteractionResult useItemOn(
        ItemStack itemStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult blockHitResult
    ) {
        Item item = itemStack.getItem();
        // Infuse if elemental potion
        if (item instanceof ElementalPotion potion && this.tryInfuse(world, pos, potion, itemStack, player)) {
            return InteractionResult.SUCCESS;
        }
        Element element = state.getValue(ELEMENT);
        // Defuse if using the corresponding empty bottle
        if (element != Element.EMPTY && ItemStack.isSameItemSameComponents(element.getPotion().get().getCraftingRemainder(), itemStack)) {
            return this.tryDefuse(world, pos, itemStack, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        // Place item if main hand and not using recipe wizard
        if (!(item instanceof RecipeWizard) && hand == InteractionHand.MAIN_HAND) {
            this.placeItem(world, pos, itemStack, player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public boolean tryInfuse(Level world, BlockPos pos, ElementalPotion potion, @NotNull ItemStack stack, @Nullable Player player) {
        BlockState state = world.getBlockState(pos);
        if (state.getValue(ELEMENT) == Element.EMPTY) {
            world.setBlockAndUpdate(pos, CropariaBlocks.INFUSOR.get().defaultBlockState().setValue(ELEMENT, potion.getElement()));
            world.playSound(null, pos, SoundEvent.createVariableRangeEvent(CropariaIf.of("block.infusor.infuse")), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            return false;
        }
        if (player != null && player.getAbilities().instabuild) {
            return true;
        }
        stack.shrink(1);
        ItemStack returnStack = potion.getCraftingRemainder();
        CifUtil.exportItem(world, pos, returnStack, player);
        return true;
    }

    public boolean tryDefuse(Level world, BlockPos pos, ItemStack stack, @Nullable Player player) {
        Item item = stack.getItem();
        BlockState state = world.getBlockState(pos);
        Element element = state.getValue(ELEMENT);
        if (element != Element.EMPTY && ElementalPotion.fromElement(element).orElseThrow().getCraftingRemainder().getItem() == item) {
            world.setBlockAndUpdate(pos, CropariaBlocks.INFUSOR.get().defaultBlockState().setValue(ELEMENT, Element.EMPTY));
        } else {
            return false;
        }
        // Don't consume item in creative mode
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ItemStack returnStack = ElementalPotion.fromElement(element).orElseThrow().getDefaultInstance();
        CifUtil.exportItem(world, pos, returnStack, player);
        return true;
    }

    public static Element getElement(BlockState state) {
        return state.getBlock() != CropariaBlocks.INFUSOR.get() ? Element.EMPTY : state.getValue(ELEMENT);
    }

    public void onCrafting(InfusorRecipe recipe, InfusorContainer container, Level world, BlockPos pos, @Nullable Player player) {
        ItemStack stack = recipe.assemble(container);
        CifUtil.exportItem(world, pos, stack, player);
        world.setBlockAndUpdate(pos, this.defaultBlockState());
        world.playSound(null, pos, SoundEvent.createVariableRangeEvent(CropariaIf.of("block.infusor.craft")), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public void tryCraft(ServerLevel world, BlockPos pos, ItemStack input, Element element, Player player) {
        if (!CropariaIf.CONFIG.getInfusor()) {
            return;
        }
        if (DropsCache.isTickQueried(world, pos)) return;
        if (element == Element.EMPTY || input.isEmpty()) {
            return;
        }
        InfusorContainer container = InfusorContainer.of(element, DropsCache.queryStacks(world, pos));
        Recipes.INFUSOR.find(container, world).ifPresent(
            recipe -> onCrafting(recipe, container, world, pos, player)
        );
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        DropsCache.remove(level, pos);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {    // Each entity step on the block will trigger crafting
        System.out.println("Infusor triggered by item entity: " + entity);
        if (entity instanceof ItemEntity itemEntity && world instanceof ServerLevel serverWorld && CropariaIf.CONFIG.getInfusor()) {
            ItemStack input = itemEntity.getItem();
            Element element = state.getValue(ELEMENT);
            if (element == Element.EMPTY || input.isEmpty()) {
                return;
            }
            this.tryCraft(serverWorld, pos, input, element, itemEntity.getOwner() instanceof Player player ? player : null);
        }
    }

    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.SHAPE;
    }

    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.SHAPE;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ELEMENT);
    }

    @Override
    public @NotNull Item asItem() {
        return CropariaItems.INFUSOR.get();
    }
}
