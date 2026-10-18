package com.example.appliedagriculture.client.jei;

import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.compat.IndustrialForegoingCompat;
import com.example.appliedagriculture.registration.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * JEI integration. Recipe lists are generated dynamically from the registries and
 * tags, so modded flowers, seeds, saplings and spawn eggs show up automatically.
 */
@OnlyIn(Dist.CLIENT)
@JeiPlugin
public class AppliedAgricultureJeiPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(AppliedAgriculture.MOD_ID, "jei_plugin");

    private static final Map<Item, Item> SAPLING_LOGS = Map.ofEntries(
            Map.entry(Items.OAK_SAPLING, Items.OAK_LOG),
            Map.entry(Items.SPRUCE_SAPLING, Items.SPRUCE_LOG),
            Map.entry(Items.BIRCH_SAPLING, Items.BIRCH_LOG),
            Map.entry(Items.JUNGLE_SAPLING, Items.JUNGLE_LOG),
            Map.entry(Items.ACACIA_SAPLING, Items.ACACIA_LOG),
            Map.entry(Items.DARK_OAK_SAPLING, Items.DARK_OAK_LOG),
            Map.entry(Items.MANGROVE_PROPAGULE, Items.MANGROVE_LOG),
            Map.entry(Items.CHERRY_SAPLING, Items.CHERRY_LOG)
    );

    private static final List<Item> VANILLA_MUSHROOMS = List.of(
            Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS);

    /** Representative break drops for vanilla crops (full lists, average counts). */
    private static final Map<Item, List<ItemStack>> SEED_OUTPUTS = Map.ofEntries(
            Map.entry(Items.WHEAT_SEEDS, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS, 2))),
            Map.entry(Items.BEETROOT_SEEDS, List.of(new ItemStack(Items.BEETROOT), new ItemStack(Items.BEETROOT_SEEDS, 2))),
            Map.entry(Items.CARROT, List.of(new ItemStack(Items.CARROT, 2))),
            Map.entry(Items.POTATO, List.of(new ItemStack(Items.POTATO, 2))),
            Map.entry(Items.NETHER_WART, List.of(new ItemStack(Items.NETHER_WART, 2))),
            Map.entry(Items.COCOA_BEANS, List.of(new ItemStack(Items.COCOA_BEANS, 2))),
            Map.entry(Items.TORCHFLOWER_SEEDS, List.of(new ItemStack(Items.TORCHFLOWER), new ItemStack(Items.TORCHFLOWER_SEEDS))),
            Map.entry(Items.PITCHER_POD, List.of(new ItemStack(Items.PITCHER_PLANT))),
            Map.entry(Items.MELON_SEEDS, List.of(new ItemStack(Items.MELON_SEEDS, 2))),
            Map.entry(Items.PUMPKIN_SEEDS, List.of(new ItemStack(Items.PUMPKIN_SEEDS, 2))),
            Map.entry(Items.SWEET_BERRIES, List.of(new ItemStack(Items.SWEET_BERRIES, 2))),
            Map.entry(Items.BAMBOO, List.of(new ItemStack(Items.BAMBOO))),
            Map.entry(Items.SUGAR_CANE, List.of(new ItemStack(Items.SUGAR_CANE))),
            Map.entry(Items.CACTUS, List.of(new ItemStack(Items.CACTUS)))
    );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helpers = registration.getJeiHelpers();
        registration.addRecipeCategories(
                new CropAssemblyChamberCategory(helpers),
                new BiomaterialSynthesisChamberCategory(helpers));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CropAssemblyChamberCategory.TYPE, cropChamberRecipes());
        registration.addRecipes(BiomaterialSynthesisChamberCategory.TYPE, bioChamberRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Makes U (view recipes) on the machine items show our categories
        registration.addRecipeCatalyst(ModItems.CROP_ASSEMBLY_CHAMBER.get(), CropAssemblyChamberCategory.TYPE);
        registration.addRecipeCatalyst(ModItems.BIOMATERIAL_SYNTHESIS_CHAMBER.get(), BiomaterialSynthesisChamberCategory.TYPE);
    }

    // ===== Crop assembly chamber =====

    private static List<CropAssemblyChamberRecipe> cropChamberRecipes() {
        var recipes = new ArrayList<CropAssemblyChamberRecipe>();

        // Chorus flower — 1x flower + 1x chorus fruit
        recipes.add(new CropAssemblyChamberRecipe(new ItemStack(Items.CHORUS_FLOWER),
                List.of(new ItemStack(Items.CHORUS_FLOWER), new ItemStack(Items.CHORUS_FRUIT))));

        // All flowers from the vanilla tags (modded flowers usually add themselves) — 3x the same flower
        var seen = new HashSet<Item>();
        for (var tag : List.of(ItemTags.FLOWERS, ItemTags.TALL_FLOWERS)) {
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                var item = holder.value();
                if (item == Items.CHORUS_FLOWER || !seen.add(item)) continue;
                recipes.add(new CropAssemblyChamberRecipe(
                        new ItemStack(item), List.of(new ItemStack(item, 3))));
            }
        }

        // All seed-like items (age-based crops) — outputs are representative examples
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)) continue;
            var block = blockItem.getBlock();
            boolean hasAge = block instanceof CropBlock
                    || block.defaultBlockState().getProperties().stream()
                            .anyMatch(p -> p.getName().equals("age") && p instanceof IntegerProperty);
            if (!hasAge) continue;
            if (item == Items.CHORUS_FLOWER || !seen.add(item)) continue;
            recipes.add(new CropAssemblyChamberRecipe(new ItemStack(item), representativeSeedOutputs(item)));
        }

        return recipes;
    }

    /**
     * Representative outputs for a seed-like item: curated vanilla table first,
     * then a heuristic for modded seeds: <name>_seeds -> <name> + seeds back,
     * otherwise the item itself (carrots/potatoes/... drop themselves).
     */
    private static List<ItemStack> representativeSeedOutputs(Item item) {
        var curated = SEED_OUTPUTS.get(item);
        if (curated != null) return curated;

        var id = BuiltInRegistries.ITEM.getKey(item);
        if (id.getPath().endsWith("_seeds")) {
            var cropId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                    id.getPath().substring(0, id.getPath().length() - "_seeds".length()));
            var crop = BuiltInRegistries.ITEM.getOptional(cropId).orElse(null);
            if (crop != null) {
                return List.of(new ItemStack(crop), new ItemStack(item));
            }
        }
        return List.of(new ItemStack(item));
    }

    // ===== Biomaterial synthesis chamber =====

    private static List<BiomaterialSynthesisChamberRecipe> bioChamberRecipes() {
        var recipes = new ArrayList<BiomaterialSynthesisChamberRecipe>();
        boolean ifLoaded = IndustrialForegoingCompat.isLoaded();

        // All spawn eggs — special mobs have dedicated products, other mobs give
        // pink slime + liquid meat (Industrial Foregoing, scaled by max health)
        var pinkSlime = ifLoaded ? fluidFromRegistry("industrialforegoing", "pink_slime") : null;
        var liquidMeat = ifLoaded ? fluidFromRegistry("industrialforegoing", "meat") : null;
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var eggItem = SpawnEggItem.byId(type);
            if (eggItem == null) continue;

            var outputs = new ArrayList<ItemStack>();
            var fluids = new ArrayList<FluidStack>();
            if (type == EntityType.CHICKEN) {
                outputs.add(new ItemStack(Items.EGG));
            } else if (type == EntityType.SHEEP) {
                outputs.add(new ItemStack(Items.WHITE_WOOL));
            } else if (type == EntityType.MOOSHROOM) {
                outputs.add(new ItemStack(ModItems.MILK_PROTEIN.get()));
                outputs.add(new ItemStack(ModItems.FUNGUS_MIXTURE.get()));
            } else if (type == EntityType.COW) {
                outputs.add(new ItemStack(ModItems.MILK_PROTEIN.get()));
            } else if (ifLoaded && pinkSlime != null && liquidMeat != null) {
                // Representative amount for 20 HP mobs
                fluids.add(new FluidStack(pinkSlime, 1000));
                fluids.add(new FluidStack(liquidMeat, 1000));
            }
            if (outputs.isEmpty() && fluids.isEmpty()) continue;

            recipes.add(new BiomaterialSynthesisChamberRecipe(new ItemStack(eggItem), outputs, fluids));
        }

        // Wither -> ether gas (Industrial Foregoing's default fluid laser drill recipe)
        if (ifLoaded) {
            var etherGas = fluidFromRegistry("industrialforegoing", "ether_gas");
            if (etherGas != null) {
                recipes.add(new BiomaterialSynthesisChamberRecipe(new ItemStack(Items.WITHER_SPAWN_EGG),
                        List.of(), List.of(new FluidStack(etherGas, 1000))));
            }
        }

        // All saplings — 5x matching log + 1x sapling back (oak: + apple), plus 1b latex with IF
        var latex = ifLoaded ? IndustrialForegoingCompat.getLatex() : null;
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SAPLINGS)) {
            var sapling = holder.value();
            var outputs = new ArrayList<ItemStack>();
            var log = getLogForSapling(sapling);
            if (log != null) {
                outputs.add(new ItemStack(log, 5));
            }
            outputs.add(new ItemStack(sapling));
            if (sapling == Items.OAK_SAPLING) {
                outputs.add(new ItemStack(Items.APPLE));
            }
            var fluids = latex != null ? List.of(new FluidStack(latex, 1000)) : List.<FluidStack>of();
            recipes.add(new BiomaterialSynthesisChamberRecipe(new ItemStack(sapling), outputs, fluids));
        }

        // Vanilla mushrooms — 3x of the same mushroom
        for (var mushroom : VANILLA_MUSHROOMS) {
            recipes.add(new BiomaterialSynthesisChamberRecipe(new ItemStack(mushroom),
                    List.of(new ItemStack(mushroom, 3)), List.of()));
        }

        return recipes;
    }

    /** The log a sapling grows into: vanilla map first, then <ns>:<name>_sapling -> <ns>:<name>_log. */
    @Nullable
    private static Item getLogForSapling(Item sapling) {
        var known = SAPLING_LOGS.get(sapling);
        if (known != null) return known;
        var id = BuiltInRegistries.ITEM.getKey(sapling);
        if (id.getPath().endsWith("_sapling")) {
            var stem = id.getPath().substring(0, id.getPath().length() - "_sapling".length());
            var logId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), stem + "_log");
            return BuiltInRegistries.ITEM.getOptional(logId).orElse(null);
        }
        return null;
    }

    @Nullable
    private static Fluid fluidFromRegistry(String namespace, String path) {
        return BuiltInRegistries.FLUID.getOptional(
                ResourceLocation.fromNamespaceAndPath(namespace, path)).orElse(null);
    }
}
