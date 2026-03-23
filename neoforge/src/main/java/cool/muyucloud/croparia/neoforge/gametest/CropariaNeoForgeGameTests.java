package cool.muyucloud.croparia.neoforge.gametest;

import cool.muyucloud.croparia.CropariaIf;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestHolder;

@SuppressWarnings("unused")
@GameTestHolder(CropariaIf.MOD_ID)
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
    public static void sanity_bootstrap(GameTestHelper helper) {
        helper.succeed();
    }
}
