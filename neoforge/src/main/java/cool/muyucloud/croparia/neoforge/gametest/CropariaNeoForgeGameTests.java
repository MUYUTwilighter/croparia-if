package cool.muyucloud.croparia.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("unused")
public final class CropariaNeoForgeGameTests {
    private CropariaNeoForgeGameTests() {
    }

    /**
     * Minimal NeoForge GameTest sample.
     * <p>
     * Keep `minecraft:empty` for bootstrap safety, then migrate to your own template
     * once you add structure files.
     * </p>
     */
    @GameTest(template = "minecraft:empty")
    public static void sanity(GameTestHelper helper) {
        helper.succeed();
    }
}
