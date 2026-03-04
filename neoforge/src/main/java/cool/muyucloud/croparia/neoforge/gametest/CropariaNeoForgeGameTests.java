package cool.muyucloud.croparia.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("unused")
public final class CropariaNeoForgeGameTests {
    private CropariaNeoForgeGameTests() {
    }

    @GameTest(template = "minecraft:empty")
    public static void sanity(GameTestHelper helper) {
        helper.succeed();
    }
}
