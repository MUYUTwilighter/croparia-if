package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.util.BlockMaterial;
import cool.muyucloud.croparia.api.crop.util.Color;
import cool.muyucloud.croparia.api.crop.util.CropDependencies;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("unused")
public class Melons {
    /**
     * Add a crop for vanilla material with specified translation key.
     *
     * @param name     crop name
     * @param material material item which the crop grows
     * @param color    int value of color
     * @param tier     tier
     */
    public static @NotNull Melon vanilla(
        @NotNull String name, Block material, int color, int tier
    ) {
        Melon melon = new Melon(
            CropariaIf.of(name), new BlockMaterial(Objects.requireNonNull(material.arch$registryName()).toString()),
            new Color(color), tier, null, new CropDependencies("minecraft", material.getDescriptionId())
        );
        DgRegistries.MELONS.register(melon);
        return melon;
    }

    public static final Melon ACACIA = vanilla("acacia", Blocks.ACACIA_LOG, 0xb86236, 1);
    public static final Melon ANDESITE = vanilla("andesite", Blocks.ANDESITE, 0x7E7E7D, 1);
    public static final Melon BASALT = vanilla("basalt", Blocks.BASALT, 0x2C2C2C, 1);
    public static final Melon BIRCH = vanilla("birch", Blocks.BIRCH_LOG, 0xc6b579, 1);
    public static final Melon BLACKSTONE = vanilla("blackstone", Blocks.BLACKSTONE, 0x1C1C1C, 1);
    public static final Melon CHERRY = vanilla("cherry", Blocks.CHERRY_LOG, 0xe3b1ab, 1);
    public static final Melon COBBLESTONE = vanilla("cobblestone", Blocks.COBBLESTONE, 0x515151, 1);
    public static final Melon COBBLED_DEEPSLATE = vanilla("deepslate_cobblestone", Blocks.COBBLED_DEEPSLATE, 0x3A3A3A, 1);
    public static final Melon CALCITE = vanilla("calcite", Blocks.CALCITE, 0xE3E4E0, 1);
    public static final Melon CRIMSON = vanilla("crimson", Blocks.CRIMSON_STEM, 0x5b2f41, 1);
    public static final Melon DIORITE = vanilla("diorite", Blocks.DIORITE, 0xBABABA, 1);
    public static final Melon DIRT = vanilla("dirt", Blocks.DIRT, 0x8B4513, 1);
    public static final Melon END_STONE = vanilla("end_stone", Blocks.END_STONE, 0xEBF3B2, 1);
    public static final Melon GRANITE = vanilla("granite", Blocks.GRANITE, 0x9C6855, 1);
    public static final Melon GRAVEL = vanilla("gravel", Blocks.GRAVEL, 0x7A7A7A, 1);
    public static final Melon HONEY = vanilla("honey", Blocks.HONEY_BLOCK, 0xB07528, 1);
    public static final Melon JUNGLE = vanilla("jungle", Blocks.JUNGLE_LOG, 0xbd8c6a, 1);
    public static final Melon MANGROVE = vanilla("mangrove", Blocks.MANGROVE_LOG, 0x7d4133, 1);
    public static final Melon MUD = vanilla("mud", Blocks.MUD, 0x464646, 1);
    public static final Melon NETHERRACK = vanilla("netherrack", Blocks.NETHERRACK, 0x7B2F2F, 1);
    public static final Melon OAK = vanilla("oak", Blocks.OAK_LOG, 0x9d824c, 1);
    public static final Melon RED_SAND = vanilla("red_sand", Blocks.RED_SAND, 0xE27B58, 1);
    public static final Melon SAND = vanilla("sand", Blocks.SAND, 0xFAE79D, 1);
    public static final Melon SHROOMLIGHT = vanilla("shroomlight", Blocks.SHROOMLIGHT, 0xFFA060, 1);
    public static final Melon SOUL_SAND = vanilla("soul_sand", Blocks.SOUL_SAND, 0x6A4F3C, 1);
    public static final Melon SOUL_SOIL = vanilla("soul_soil", Blocks.SOUL_SOIL, 0x5C4033, 1);
    public static final Melon SPRUCE = vanilla("spruce", Blocks.SPRUCE_LOG, 0x795933, 1);
    public static final Melon TUFF = vanilla("tuff", Blocks.TUFF, 0x6E6E6E, 1);
    public static final Melon WARPED = vanilla("warped", Blocks.WARPED_STEM, 0x388180, 1);

    public static final Melon CRIMSON_NYLIUM = vanilla("crimson_nylium", Blocks.CRIMSON_NYLIUM, 0x7A3940, 2);
    public static final Melon GRASS_BLOCK = vanilla("grass_block", Blocks.GRASS_BLOCK, 0x88BB55, 2);
    public static final Melon ICE = vanilla("ice", Blocks.ICE, 0x99D9EA, 2);
    public static final Melon MOSS_BLOCK = vanilla("moss_block", Blocks.MOSS_BLOCK, 0x6CA66C, 2);
    public static final Melon MYCELIUM = vanilla("mycelium", Blocks.MYCELIUM, 0x7F6A93, 2);
    public static final Melon OBSIDIAN = vanilla("obsidian", Blocks.OBSIDIAN, 0x1C1C2A, 2);
    public static final Melon PALE_MOSS_BLOCK = vanilla("pale_moss_block", Blocks.PALE_MOSS_BLOCK, 0xA2C5A2, 2);
    public static final Melon PODZOL = vanilla("podzol", Blocks.PODZOL, 0x8B5A2B, 2);
    public static final Melon SCULK = vanilla("sculk", Blocks.SCULK, 0x0B3B2E, 2);
    public static final Melon WARPED_NYLIUM = vanilla("warped_nylium", Blocks.WARPED_NYLIUM, 0x2E7C7A, 2);

    public static final Melon OCHRE_FROGLIGHT = vanilla("ochre_froglight", Blocks.OCHRE_FROGLIGHT, 0xFFA500, 3);
    public static final Melon PEARLESCENT_FROGLIGHT = vanilla("pearlescent_froglight", Blocks.PEARLESCENT_FROGLIGHT, 0xD8BFD8, 3);
    public static final Melon SCULK_CATALYST = vanilla("sculk_catalyst", Blocks.SCULK_CATALYST, 0x0F4D3A, 3);
    public static final Melon SCULK_SHRIEKER = vanilla("sculk_shrieker", Blocks.SCULK_SHRIEKER, 0x0A362B, 3);
    public static final Melon SCULK_SENSOR = vanilla("sculk_sensor", Blocks.SCULK_SENSOR, 0x0C3E2D, 3);
    public static final Melon SPONGE = vanilla("sponge", Blocks.SPONGE, 0xC2B46C, 3);
    public static final Melon VERDANT_FROGLIGHT = vanilla("verdant_froglight", Blocks.VERDANT_FROGLIGHT, 0x799075, 3);

    public static final Melon DRAGON_HEAD = vanilla("dragon_head", Blocks.DRAGON_HEAD, 0x6F3C7C, 4);
    public static final Melon WITHER_SKELETON_SKULL = vanilla("wither_skeleton_skull", Blocks.WITHER_SKELETON_SKULL, 0x121212, 4);

    public static final Melon CREEPER_HEAD = vanilla("creeper_head", Blocks.CREEPER_HEAD, 0x00FF00, 5);
    public static final Melon SKELETON_SKULL = vanilla("skeleton_skull", Blocks.SKELETON_SKULL, 0xFFFFE0, 5);
    public static final Melon ZOMBIE_HEAD = vanilla("zombie_head", Blocks.ZOMBIE_HEAD, 0x00AA00, 5);

    public static final Melon PIGLIN_HEAD = vanilla("piglin_head", Blocks.PIGLIN_HEAD, 0xFFD700, 6);

    public static final Melon PLAYER_HEAD = vanilla("player_head", Blocks.PLAYER_HEAD, 0xFFDAB9, 7);


    public static void register() {
        CropariaIf.LOGGER.info("Loading custom crops from file definitions");
        DgRegistries.MELONS.readCrops();
    }
}
