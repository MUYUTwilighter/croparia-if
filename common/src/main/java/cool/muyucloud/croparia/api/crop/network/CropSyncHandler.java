package cool.muyucloud.croparia.api.crop.network;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.command.CommonCommandRoot;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import cool.muyucloud.croparia.mixin.CreativeModeTabsAccessor;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.SplitPacketTransformer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Crop sync handler, which syncs the crop definitions to the client.
 * <p>
 * The whole definition set is sent as one payload. {@link SplitPacketTransformer} takes care of slicing the encoded
 * bytes whenever they exceed the packet size limit, and reassembles them on the receiving side, so this class does not
 * need to page the crops itself.
 */
@SuppressWarnings("UnstableApiUsage")
public class CropSyncHandler implements NetworkHandler, Iterable<CropSyncHandler.Batch<?>> {
    public static final StreamCodec<FriendlyByteBuf, CropSyncHandler> CODEC = StreamCodec.of((buf, handler) -> {
        buf.writeBoolean(handler.shouldReload());
        buf.writeInt(handler.size());
        for (CropSyncHandler.Batch<?> batch : handler) {
            Batch.STREAM_CODEC.encode(buf, batch);
        }
    }, buf -> {
        boolean reload = buf.readBoolean();
        int size = buf.readInt();
        Batch<?>[] batches = new Batch[size];
        for (int i = 0; i < size; i++) {
            batches[i] = Batch.STREAM_CODEC.decode(buf);
        }
        return new CropSyncHandler(reload, batches);
    });
    public static final NetworkHandlerType<CropSyncHandler> TYPE = NetworkHandlerType.ofS2C(
        CropariaIf.of("crop_sync"), CODEC, List.of(new SplitPacketTransformer())
    );

    private final List<Batch<?>> batches;
    private final boolean reload;

    public CropSyncHandler(boolean reload, CropRegistry<?>... registries) {
        this(reload, Arrays.stream(registries).map(Batch::new).toArray(Batch[]::new));
    }

    public CropSyncHandler(boolean reload, Collection<CropRegistry<?>> registries) {
        this(reload, registries.toArray(CropRegistry[]::new));
    }

    protected CropSyncHandler(boolean reload, Batch<?>... batches) {
        this.reload = reload;
        this.batches = List.of(batches);
    }

    public int size() {
        return batches.size();
    }

    public boolean shouldReload() {
        return reload;
    }

    @Override
    public @NotNull Iterator<Batch<?>> iterator() {
        return this.batches.iterator();
    }

    @Override
    public @NotNull NetworkHandlerType<?> handlerType() {
        return TYPE;
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(this::apply);
    }

    /** Applies decoded definitions on the client thread, in either configuration or play phase. */
    public void apply() {
        for (Batch<?> batch : batches) batch.handle();
        CreativeModeTabsAccessor.cif$setCachedParams(null);
        if (this.shouldReload()) CropariaIf.ifClient(ref -> {
            var client = ref.get();
            if (client.isLocalServer() || CropariaIf.CONFIG.getAutoReload()) {
                client.reloadResourcePacks();
            } else {
                String root = CommonCommandRoot.commandRoot(true);
                client.gui.getChat().addClientSystemMessage(Texts.translatable("chat.croparia.sync.reload",
                    Texts.forStyles(Texts.translatable("chat.croparia.sync.reload.now"),
                        Texts.runCommand(root, "sync", "all"), Texts.inlineMouseBehavior()),
                    Texts.forStyles(Texts.translatable("chat.croparia.sync.reload.autoReload"),
                        Texts.runCommand(root, "autoReload", "true"), Texts.inlineMouseBehavior(),
                        Texts.hoverText(Texts.translatable("chat.croparia.sync.reload.autoReload.tooltip")))
                ));
            }
        });
    }

    public static class Batch<C extends AbstractCrop<?>> {
        public static final MapCodec<Batch<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CropRegistry.CODEC_BY_NAME.fieldOf("type").forGetter(Batch::getRegistry),
            CompoundTag.CODEC.listOf().fieldOf("crops").forGetter(Batch::getRawCrops)
        ).apply(instance, Batch::deserialize));
        public static final StreamCodec<FriendlyByteBuf, Batch<?>> STREAM_CODEC = StreamCodec.of((buf, batch) -> {
            buf.writeIdentifier(batch.getRegistry().getId());
            buf.writeInt(batch.getRawCrops().size());
            for (CompoundTag tag : batch.getRawCrops()) {
                buf.writeNbt(tag);
            }
        }, buf -> {
            Identifier id = buf.readIdentifier();
            int count = buf.readInt();
            List<CompoundTag> tags = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                tags.add(buf.readNbt());
            }
            return deserialize((CropRegistry<?>) DgRegistry.REGISTRY.get(id), tags);
        });

        protected static <C extends AbstractCrop<?>> Batch<C> deserialize(@NotNull CropRegistry<C> registry, @NotNull List<CompoundTag> rawCrops) {
            List<C> crops = rawCrops.stream().map(crop -> registry.getCropCodec().decode(NbtOps.INSTANCE, crop).getOrThrow().getFirst()).toList();
            return new Batch<>(registry, crops);
        }

        private final @NotNull CropRegistry<C> registry;
        private final @NotNull List<C> crops;

        public Batch(@NotNull CropRegistry<C> registry) {
            ArrayList<C> crops = new ArrayList<>();
            registry.forEach(crops::add);
            crops.trimToSize();
            this(registry, crops);
        }

        protected Batch(@NotNull CropRegistry<C> registry, @NotNull List<C> crops) {
            this.registry = registry;
            this.crops = crops;
        }

        public @NotNull CropRegistry<C> getRegistry() {
            return registry;
        }

        public @NotNull List<C> getCrops() {
            return crops;
        }

        public @NotNull List<CompoundTag> getRawCrops() {
            return registry.getNbt();
        }

        public void handle() {
            registry.syncCrops(crops);
        }
    }

    public static class Task implements ConfigurationTask {
        public static final Type TYPE = new Type(CropariaIf.of("crop_sync").toString());

        private final CropSyncHandler handler;

        public Task(boolean reload, CropRegistry<?>... registries) {
            this.handler = new CropSyncHandler(reload, registries);
        }

        @Override
        public void start(Consumer<Packet<?>> connection) {
            connection.accept(new ClientboundCustomPayloadPacket(handler));
        }

        @Override
        public @NotNull Type type() {
            return TYPE;
        }
    }

    public static class TaskAck implements CustomPacketPayload {
        public static final TaskAck INSTANCE = new TaskAck();
        public static final Type<TaskAck> TYPE =
            new Type<>(CropariaIf.of("crop_sync_ack"));
        public static final StreamCodec<FriendlyByteBuf, TaskAck> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @NotNull Type<TaskAck> type() {
            return TYPE;
        }
    }
}
