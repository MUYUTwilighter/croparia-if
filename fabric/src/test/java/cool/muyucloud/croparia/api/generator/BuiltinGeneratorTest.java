package cool.muyucloud.croparia.api.generator;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.crop.Crop;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.asset.Color;
import cool.muyucloud.croparia.api.crop.util.BlockMaterial;
import cool.muyucloud.croparia.api.crop.util.CropDependencies;
import cool.muyucloud.croparia.api.crop.util.ItemMaterial;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.*;
import cool.muyucloud.croparia.api.json.JsonTransformer;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.api.placeholder.Template;
import cool.muyucloud.croparia.api.recipe.entry.BlockOutput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinGeneratorTest {
    private static final Path ROOT = Path.of("../common/src/main/resources/data-generators/croparia");
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final Set<String> UNSUPPORTED_THERMAL_COMPAT = Set.of(
        "thermal.insolator.crop.toml", "thermal.insolator.melon.toml"
    );

    private static ClassLoader previousContextLoader;
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrap() {
        // Fabric's test classes use Knot, while ServiceLoader otherwise sees Gradle's parent loader.
        previousContextLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(BuiltinGeneratorTest.class.getClassLoader());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Vanilla item prototypes are normally bound during the world-data bootstrap.
        BuiltInRegistries.ITEM.listElements().forEach(holder -> holder.bindComponents(DataComponents.COMMON_ITEM_COMPONENTS));
        registryOps = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
    }

    @AfterAll
    static void restoreContextLoader() {
        Thread.currentThread().setContextClassLoader(previousContextLoader);
    }

    @Test
    void builtinTemplatesUseCurrentPlaceholdersAndNativeSchemas() throws Exception {
        Map<String, DgEntry> entries = Map.of(
            "croparia:crops", crop(),
            "croparia:melons", melon(),
            "croparia:elements", Element.AIR,
            "croparia:items", new ItemDgEntry(Identifier.parse("croparia:infusor"))
        );
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".toml")).toList()) {
                JsonObject spec = JsonTransformer.parseToml(Files.readString(path)).getAsJsonObject();
                if (!spec.has("registry")) continue; // Recipe-wizard templates require a world context.
                assertTrue(spec.get("startup").getAsBoolean(), path.toString());
                DgEntry entry = Objects.requireNonNull(entries.get(spec.get("registry").getAsString()), path.toString());
                String filename = path.getFileName().toString();
                if (UNSUPPORTED_THERMAL_COMPAT.contains(filename)) {
                    assertFalse(spec.get("enabled").getAsBoolean(), "Upstream Thermal cannot read component outputs yet: " + filename);
                }
                if (spec.has("type") && spec.get("type").getAsString().equals("croparia:lang")) continue;
                String outputPath = new Template(spec.get("path").getAsString()).parse(entry);
                assertFalse(outputPath.contains("${"), outputPath);
                String template = spec.get("template").getAsString();
                if (spec.has("content")) {
                    template = template.replace("${content}", new Template(spec.get("content").getAsString()).parse(entry));
                }
                String rendered = new Template(template).parse(entry);
                assertFalse(rendered.contains("${"), filename + ": " + rendered);
                JsonObject json = JSON.fromJson(rendered, JsonObject.class);
                assertNotNull(json, filename);

                if (filename.equals("croparia.crop_seed.toml") || filename.equals("croparia.melon_seed.toml")) {
                    JsonObject result = json.getAsJsonObject("result");
                    String item = filename.contains("crop_seed") ? "croparia:crop_seed" : "croparia:melon_seed";
                    assertEquals(item, result.get("id").getAsString());
                    assertEquals(entry.getKey().toString(), result.getAsJsonObject("components").get("croparia:crop").getAsString());
                }
                if (outputPath.contains("/loot_table/")) {
                    String blockId = switch (filename) {
                        case "croparia.crop_block.toml" -> "crop";
                        case "croparia.melon_block.toml" -> "melon";
                        case "croparia.melon_stem.toml" -> "melon_stem";
                        case "croparia.melon_attach.toml" -> "melon_attach";
                        default -> throw new AssertionError(filename);
                    };
                    assertEquals("croparia/loot_table/" + blockId + "/" + entry.getKey().getPath() + ".json", outputPath);
                    assertEquals("minecraft:block", json.get("type").getAsString());
                    for (JsonElement pool : json.getAsJsonArray("pools")) {
                        assertEquals(1, pool.getAsJsonObject().get("rolls").getAsDouble());
                        for (JsonElement drop : pool.getAsJsonObject().getAsJsonArray("entries")) {
                            JsonObject components = drop.getAsJsonObject().getAsJsonArray("functions")
                                .get(0).getAsJsonObject().getAsJsonObject("components");
                            assertEquals(entry.getKey().toString(), components.get("croparia:crop").getAsString());
                        }
                    }
                    // The plain-JVM test has vanilla registries, not loader-registered Croparia objects.
                    // Substitute equivalent registered carriers; keep the generated schema and component values intact.
                    var decoded = LootTable.DIRECT_CODEC.parse(registryOps, vanillaCarriers(json));
                    assertTrue(decoded.isSuccess(), filename + ": " + decoded);
                } else if (json.has("type") && json.get("type").getAsString().startsWith("minecraft:crafting_")) {
                    var decoded = Recipe.CODEC.parse(registryOps, vanillaCarriers(json));
                    assertTrue(decoded.isSuccess(), filename + ": " + decoded);
                }
            }
        }
    }

    @Test
    void botanyPotsTemplatesMatchOnlyTheRequestedCropAndKeepOutputComponents() throws Exception {
        if (CustomIngredientSerializer.get(Identifier.parse("fabric:components")) == null) {
            Ingredient ingredient = DefaultCustomIngredients.components(Ingredient.of(Items.WHEAT_SEEDS),
                builder -> builder.remove(DataComponents.CUSTOM_NAME));
            CustomIngredientSerializer.register(Objects.requireNonNull(ingredient.getCustomIngredient()).getSerializer());
        }
        assertEquals("123456", new Template("${rgb}").parse(Color.of("#123456"), Color.PLACEHOLDER));
        assertEquals("ABCDEF", new Template("${rgb}").parse(Color.of(0x12abcdef), Color.PLACEHOLDER));

        for (DgEntry entry : List.of(crop(), melon())) {
            boolean isCrop = entry instanceof Crop;
            String name = isCrop ? "crop" : "melon";
            String seed = "croparia:" + name + "_seed";
            String fruit = isCrop ? "croparia:crop_fruit" : "croparia:melon";
            JsonObject spec = JsonTransformer.parseToml(Files.readString(ROOT.resolve("datapack/botanypots." + name + ".toml"))).getAsJsonObject();
            assertTrue(spec.get("enabled").getAsBoolean());
            assertEquals("botanypots", spec.get("dependencies").getAsString());
            String rendered = new Template(spec.get("template").getAsString()).parse(entry);
            assertFalse(rendered.contains("${"), rendered);
            JsonObject json = JSON.fromJson(rendered, JsonObject.class);
            assertEquals("botanypots:crop", json.get("type").getAsString());
            assertEquals(isCrop ? 1200 : 600, json.get("grow_time").getAsInt());
            assertFalse(json.has("growthTicks"));
            assertFalse(json.has("categories")); // Current BP defaults to its dirt-soil ingredient.

            JsonObject input = json.getAsJsonObject("input");
            assertEquals("neoforge:components", input.get("type").getAsString());
            assertEquals("fabric:components", input.get("fabric:type").getAsString());
            assertEquals(seed, input.get("base").getAsString());
            assertEquals(seed, input.getAsJsonArray("items").get(0).getAsString());
            assertFalse(input.get("strict").getAsBoolean());
            assertEquals(entry.getKey().toString(), input.getAsJsonObject("components").get("croparia:crop").getAsString());

            // Use the real 26.1.2 Fabric Ingredient dispatcher, including the extra NeoForge fields.
            JsonObject nativeInput = vanillaCarriers(input).getAsJsonObject();
            Ingredient ingredient = Ingredient.CODEC.parse(registryOps, nativeInput).getOrThrow();
            ItemStack stack = new ItemStack(isCrop ? Items.WHEAT_SEEDS : Items.MELON_SEEDS);
            assertFalse(ingredient.test(stack), "An unidentified seed must not match");
            stack.set(DataComponents.ITEM_MODEL, entry.getKey());
            assertTrue(ingredient.test(stack));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("renamed seed"));
            assertTrue(ingredient.test(stack), "Unrelated components must remain allowed");
            stack.set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:other"));
            assertFalse(ingredient.test(stack), "Another crop sharing the item ID must not match");
            String display = SlotDisplay.CODEC.encodeStart(registryOps, ingredient.display()).getOrThrow().toString();
            assertTrue(display.contains("minecraft:item_model"), display);
            assertTrue(display.contains(entry.getKey().toString()), display);

            // NeoForge's codec reads this holder set and patch; its implementation is audited upstream.
            var items = HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false)
                .parse(registryOps, nativeInput.get("items")).getOrThrow();
            assertEquals(1, items.size());
            assertEquals(stack.getItem(), items.get(0).value());
            DataComponentPatch patch = DataComponentPatch.CODEC.parse(registryOps, nativeInput.get("components")).getOrThrow();
            stack.applyComponents(patch);
            assertEquals(entry.getKey(), stack.get(DataComponents.ITEM_MODEL));
            assertTrue(ingredient.test(stack));

            JsonArray phases = json.getAsJsonObject("display").getAsJsonArray("phases");
            assertEquals("botanypots:transitional", json.getAsJsonObject("display").get("type").getAsString());
            assertEquals(8, phases.size());
            for (int age = 0; age < phases.size(); age++) {
                JsonObject phase = phases.get(age).getAsJsonObject();
                assertEquals("botanypots:simple", phase.get("type").getAsString());
                JsonObject state = phase.getAsJsonObject("block_state");
                assertEquals(isCrop ? "croparia:crop" : age < 7 ? "croparia:melon_stem" : "croparia:melon", state.get("block").getAsString());
                if (isCrop || age < 7) assertEquals(Integer.toString(age), state.getAsJsonObject("properties").get("age").getAsString());
                if (isCrop) assertEquals(Crop.NATURE, state.getAsJsonObject("properties").get("type").getAsString());
                if (age >= (isCrop ? 5 : 7)) assertEquals("#ABCDEF", phase.getAsJsonObject("options").get("color").getAsString());
            }

            JsonArray drops = json.getAsJsonArray("drops").get(0).getAsJsonObject().getAsJsonArray("items");
            assertEquals(2, drops.size());
            for (int i = 0; i < drops.size(); i++) {
                JsonObject drop = drops.get(i).getAsJsonObject();
                JsonObject result = drop.getAsJsonObject("result");
                assertEquals(i == 0 ? seed : fruit, result.get("id").getAsString());
                assertEquals(i == 0 ? 0.01 : 1.0, drop.get("chance").getAsDouble());
                ItemStack output = ItemStack.CODEC.parse(registryOps, vanillaCarriers(result)).getOrThrow();
                assertFalse(output.isEmpty());
                assertEquals(entry.getKey(), output.get(DataComponents.ITEM_MODEL));
            }
        }
    }

    @Test
    void recipeWizardTemplatesPreserveBlockAndItemComponents() throws Exception {
        ItemStack stack = new ItemStack(Items.APPLE, 3);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("croparia:test"));
        JsonElement item = ItemOutput.CODEC.encodeStart(registryOps, ItemOutput.of(stack)).getOrThrow();
        BlockOutput block = BlockOutput.CODEC.parse(registryOps, JsonParser.parseString("""
            {"id":"minecraft:chest","properties":{"facing":"north"},
             "components":{"minecraft:item_model":"croparia:test"}}
            """)).getOrThrow();
        JsonElement blockJson = BlockOutput.CODEC.encodeStart(registryOps, block).getOrThrow();
        JsonObject values = new JsonObject();
        values.addProperty("datetime", "20260101_120000");
        values.addProperty("infusor_element", "air");
        values.addProperty("soak_element", "air");
        for (String key : List.of("block", "ritual_input", "neighbor", "below")) values.add(key, blockJson);
        for (String key : List.of("item", "off_hand")) values.add(key, item);
        Placeholder<JsonObject> placeholders = Placeholder.build(builder -> {
            for (String key : values.keySet()) {
                builder.then(PatternKey.literal(key), TypeMapper.of(json -> json.get(key)), Placeholder.JSON);
            }
            return builder;
        });

        var codecs = Map.of(
            "infusor", InfusorRecipe.TYPED_SERIALIZER.codec().codec(),
            "ritual", RitualRecipe.TYPED_SERIALIZER.codec().codec(),
            "soak", SoakRecipe.TYPED_SERIALIZER.codec().codec()
        );
        for (String name : codecs.keySet()) {
            JsonObject spec = JsonTransformer.parseToml(Files.readString(ROOT.resolve("recipe_wizard/croparia." + name + ".toml"))).getAsJsonObject();
            String path = new Template(spec.get("path").getAsString()).parse(values, placeholders);
            assertEquals("croparia/" + name + "_20260101_120000.json", path);
            String rendered = new Template(spec.get("template").getAsString()).parse(values, placeholders);
            assertFalse(rendered.contains("${"), rendered);
            JsonObject json = JSON.fromJson(rendered, JsonObject.class);
            var decoded = codecs.get(name).parse(registryOps, json);
            assertTrue(decoded.isSuccess(), name + ": " + decoded);
            if (name.equals("soak")) {
                assertEquals(blockJson, json.get("input"));
                assertEquals(blockJson, json.get("output"));
            } else {
                assertEquals(item, json.get("ingredient"));
                assertEquals(item, json.get("result"));
                if (name.equals("ritual")) {
                    assertEquals(blockJson, json.get("ritual"));
                    assertEquals(blockJson, json.get("block"));
                }
            }
        }
    }

    @Test
    void cropAndMelonLanguagesMergeAndEscapeNames(@TempDir Path temp) throws Exception {
        PackHandler pack = new PackHandler(Identifier.parse("croparia_test:languages"), temp, new JsonObject(), () -> false) {};
        List<LangGenerator> generators = new ArrayList<>();
        for (TranslatableEntry entry : List.of(crop(), melon())) {
            String name = entry instanceof Crop ? "crop" : "melon";
            JsonObject spec = JsonTransformer.parseToml(Files.readString(ROOT.resolve("resourcepack/croparia." + name + "_lang.toml"))).getAsJsonObject();
            LangGenerator generator = new LangGenerator(true, spec.get("startup").getAsBoolean(), List.of(),
                new Template(spec.get("path").getAsString()), DgRegistry.ofMap(Map.of(entry.getKey(), entry)),
                new Template(spec.get("template").getAsString()));
            generator.generate(pack);
            generators.add(generator);
        }
        generators.forEach(generator -> generator.onGenerated(pack));
        for (String lang : List.of("en_us", "zh_cn")) {
            String source = pack.<String>occupy(generators.getFirst(), "croparia/lang/" + lang + ".json").orElseThrow();
            assertFalse(source.contains("${"), source);
            JsonObject translations = JSON.fromJson(source, JsonObject.class);
            assertEquals(2, translations.size());
            assertEquals(crop().translate(lang), translations.get("crop.croparia.test").getAsString());
            assertEquals(melon().translate(lang), translations.get("melon.croparia.test").getAsString());
        }
    }

    private static Crop crop() {
        return new Crop(Identifier.parse("croparia:test"), new ItemMaterial("minecraft:diamond"), Color.of(0xabcdef),
            1, Crop.NATURE, Map.of("en_us", "Crop \"quoted\"\nsecond line", "zh_cn", "作物\\名称"), dependencies("crop.croparia.test")) {
            @Override public boolean shouldLoad() { return true; }
        };
    }

    private static Melon melon() {
        return new Melon(Identifier.parse("croparia:test"), new BlockMaterial("minecraft:stone"), Color.of(0xabcdef),
            1, Map.of("en_us", "Melon \"quoted\"\nsecond line", "zh_cn", "巨果\\名称"), dependencies("melon.croparia.test")) {
            @Override public boolean shouldLoad() { return true; }
        };
    }

    private static CropDependencies dependencies(String key) {
        return new CropDependencies("croparia", key) {
            @Override protected boolean shouldLoad(String modId) { return true; }
        };
    }

    private static JsonElement vanillaCarriers(JsonElement value) {
        Map<String, String> ids = Map.ofEntries(
            Map.entry("croparia:crop_seed", "minecraft:wheat_seeds"), Map.entry("croparia:crop_fruit", "minecraft:apple"),
            Map.entry("croparia:melon_seed", "minecraft:melon_seeds"), Map.entry("croparia:melon", "minecraft:melon"),
            Map.entry("croparia:crop", "minecraft:wheat"), Map.entry("croparia:melon_stem", "minecraft:melon_stem"),
            Map.entry("croparia:melon_attach", "minecraft:attached_melon_stem"), Map.entry("croparia:croparia", "minecraft:wheat"),
            Map.entry("#croparia:seed_ingredient", "minecraft:wheat_seeds"), Map.entry("croparia:gem_air", "minecraft:diamond"),
            Map.entry("croparia:bucket_air", "minecraft:water_bucket"), Map.entry("croparia:potion_air", "minecraft:potion")
        );
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            value.getAsJsonObject().entrySet().forEach(e -> result.add(
                e.getKey().equals("croparia:crop") ? "minecraft:item_model" : e.getKey(), vanillaCarriers(e.getValue())));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(e -> result.add(vanillaCarriers(e)));
            return result;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return new JsonPrimitive(ids.getOrDefault(value.getAsString(), value.getAsString()));
        }
        return value;
    }
}
