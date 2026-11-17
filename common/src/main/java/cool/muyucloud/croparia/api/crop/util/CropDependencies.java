package cool.muyucloud.croparia.api.crop.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.platform.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CropDependencies implements Iterable<Map.Entry<String, String>> {
    public static final Codec<CropDependencies> CODEC_VANILLA = Codec.STRING.xmap(s -> new CropDependencies(CropariaIf.MOD_ID, s), keys -> keys.getKey(CropariaIf.MOD_ID));
    public static final Codec<CropDependencies> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(CropDependencies::new, CropDependencies::getCandidates);
    public static final MultiCodec<CropDependencies> CODEC_ANY = CodecUtil.of(CODEC, CODEC_VANILLA);
    public static final CropDependencies EMPTY = new CropDependencies(Map.of());

    private final Map<String, String> candidates;
    private final LazySupplier<String> chosen = LazySupplier.of(() -> {
        for (Map.Entry<String, String> entry : this) {
            String modId = entry.getKey();
            if (this.shouldLoad(modId)) return entry.getValue();
        }
        return null;
    });

    public CropDependencies(Map<String, String> candidates) {
        this.candidates = ImmutableMap.copyOf(candidates);
    }

    public CropDependencies(String... entries) {
        if (entries.length % 2 == 0) {
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < entries.length; i += 2) {
                map.put(entries[i], entries[i + 1]);
            }
            this.candidates = ImmutableMap.copyOf(map);
        } else {
            throw new IllegalArgumentException("Entries are not in pairs");
        }
    }

    public boolean shouldLoad() {
        for (String modId : this.candidates.keySet()) {
            if (shouldLoad(modId)) {
                return true;
            }
        }
        return this.isEmpty();
    }

    protected boolean shouldLoad(String modId) {
        return Platform.isModLoaded(modId) && CropariaIf.CONFIG.isModValid(modId);
    }

    protected Map<String, String> getCandidates() {
        return this.candidates;
    }

    /**
     * Get a candidate translation key by mod id.
     *
     * @param modId The mod id to get the key for.
     * @return The translation key, or null if the mod id is not a candidate.
     *
     */
    public @Nullable String getKey(String modId) {
        return this.candidates.get(modId);
    }

    /**
     * Get a chosen translation key.
     *
     * @return the chosen translation key, or null if no candidate is available.
     * @see #chosen
     *
     */
    public String getChosen() {
        return chosen.get();
    }

    public boolean isEmpty() {
        return this.candidates.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return this.candidates.entrySet().iterator();
    }

    public int size() {
        return this.candidates.size();
    }
}
