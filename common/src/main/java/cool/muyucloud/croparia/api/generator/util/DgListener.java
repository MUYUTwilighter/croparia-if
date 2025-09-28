package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.generator.pack.PackHandler;

public interface DgListener {
    default void onGenerated(PackHandler pack) {
    }

    @SuppressWarnings("unused")
    default void onDumped(PackHandler pack) {
    }
}
