package cool.muyucloud.croparia.util;

import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("unused")
public class Constants {
    public static final ResourceLocation ITEM_DROP = ResourceLocation.parse("croparia:textures/gui/item_drop.png");
    public static final ResourceLocation ELEM_INFUSE = ResourceLocation.parse("croparia:textures/gui/elem_infuse.png");
    public static final ResourceLocation BLOCK_PLACE = ResourceLocation.parse("croparia:textures/gui/block_place.png");
    public static final ResourceLocation BLOCK_PLACE_UPON = ResourceLocation.parse("croparia:textures/gui/block_place_upon.png");
    public static final ResourceLocation LEFT_DARK = ResourceLocation.parse("croparia:textures/gui/left_dark.png");
    public static final ResourceLocation LEFT_WHITE = ResourceLocation.parse("croparia:textures/gui/left_white.png");
    public static final ResourceLocation RIGHT_DARK = ResourceLocation.parse("croparia:textures/gui/right_dark.png");
    public static final ResourceLocation RIGHT_WHITE = ResourceLocation.parse("croparia:textures/gui/right_white.png");
    public static final ResourceLocation UP_DARK = ResourceLocation.parse("croparia:textures/gui/up_dark.png");
    public static final ResourceLocation UP_WHITE = ResourceLocation.parse("croparia:textures/gui/up_white.png");
    public static final ResourceLocation DOWN_DARK = ResourceLocation.parse("croparia:textures/gui/down_dark.png");
    public static final ResourceLocation DOWN_WHITE = ResourceLocation.parse("croparia:textures/gui/down_white.png");
    public static final Style USAGE = Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY);
    public static final MutableComponent INPUT_BLOCK = Texts.translatable("tooltip.croparia.input");
    public static final MutableComponent AIR_BLOCK = Texts.translatable("tooltip.croparia.air");
    public static final MutableComponent ANY_BLOCK = Texts.translatable("tooltip.croparia.any");
    public static final MutableComponent TOOLTIP_RITUAL = Texts.translatable("tooltip.croparia.ritual").setStyle(USAGE);
    public static final MutableComponent ITEM_DROP_TOOLTIP = Texts.translatable("tooltip.croparia.item_drop").setStyle(USAGE);
    public static final MutableComponent ELEM_INFUSE_TOOLTIP = Texts.translatable("tooltip.croparia.elem_infuse").setStyle(USAGE);
    public static final MutableComponent BLOCK_PLACE_TOOLTIP = Texts.translatable("tooltip.croparia.block_place").setStyle(USAGE);
    public static final MutableComponent SOAK_BLOCK_INPUT = Texts.translatable("tooltip.croparia.soak.input").setStyle(USAGE);
    public static final MutableComponent SOAK_INFUSOR = Texts.translatable("tooltip.croparia.soak.infusor").setStyle(USAGE);
    public static final MutableComponent RITUAL_STRUCTURE_LOWER = Texts.translatable("gui.croparia.ritual_structure.lower");
    public static final MutableComponent RITUAL_STRUCTURE_UPPER = Texts.translatable("gui.croparia.ritual_structure.upper");
    public static final MutableComponent INSUFFICIENT_XP = Texts.translatable("overlay.croparia.xp");
}
