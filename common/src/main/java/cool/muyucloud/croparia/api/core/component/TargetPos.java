package cool.muyucloud.croparia.api.core.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class TargetPos {
    public static final String TAG_KEY = "CropariaTargetPos";
    public static final MapCodec<TargetPos> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("dim").forGetter(TargetPos::getDim),
        BlockPos.CODEC.fieldOf("pos").forGetter(TargetPos::getPos)
    ).apply(instance, TargetPos::new));

    @NotNull
    private transient final ResourceKey<Level> dimKey;
    @NotNull
    private final BlockPos pos;

    public TargetPos(@NotNull Entity entity) {
        this(entity.level(), entity.blockPosition());
    }

    public TargetPos(@NotNull Level level, @NotNull BlockPos pos) {
        this(level.dimension(), pos);
    }

    public TargetPos(@NotNull ResourceKey<Level> dim, @NotNull BlockPos pos) {
        this.pos = pos;
        this.dimKey = dim;
    }

    public TargetPos(@NotNull ResourceLocation dim, @NotNull BlockPos pos) {
        this(ResourceKey.create(Registries.DIMENSION, dim), pos);
    }

    public static void save(@NotNull ItemStack stack, @NotNull TargetPos targetPos) {
        CompoundTag tag = stack.getOrCreateTag();
        CODEC.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, targetPos).result().ifPresent(encoded -> tag.put(TAG_KEY, encoded));
    }

    public static @Nullable TargetPos load(@NotNull ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_KEY, CompoundTag.TAG_COMPOUND)) {
            return null;
        }
        return CODEC.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.getCompound(TAG_KEY)).result().orElse(null);
    }

    @NotNull
    public ResourceLocation getDim() {
        return dimKey.location();
    }

    @NotNull
    public ResourceKey<Level> getDimKey() {
        return dimKey;
    }

    public Optional<ServerLevel> getLevel(@NotNull MinecraftServer server) {
        return Optional.ofNullable(server.getLevel(this.getDimKey()));
    }

    @NotNull
    public BlockPos getPos() {
        return pos;
    }

    public net.minecraft.network.chat.MutableComponent getTooltip() {
        return Texts.translatable("tooltip.croparia.bounded_position", this.getDim(), this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
    }

    public void teleport(@NotNull Entity entity, @NotNull MinecraftServer server) {
        this.getLevel(server).ifPresent(level -> entity.teleportTo(level, getPos().getX(), getPos().getY(), getPos().getZ(), RelativeMovement.ROTATION, 0, 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetPos targetPos)) return false;
        return Objects.equals(dimKey, targetPos.dimKey) && Objects.equals(pos, targetPos.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimKey, pos);
    }
}
