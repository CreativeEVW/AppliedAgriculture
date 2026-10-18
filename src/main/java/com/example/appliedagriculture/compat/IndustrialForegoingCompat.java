package com.example.appliedagriculture.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Optional;

/**
 * Optional integration with Industrial Foregoing.
 *
 * Industrial Foregoing is NOT a required dependency: every call here is guarded by
 * {@link #isLoaded()}, and IF classes are only touched via reflection (its recipe
 * classes are never referenced in this code). The entity -> fluid mapping follows
 * IF's fluid laser drill recipes, so changes to IF's recipe data are picked up
 * automatically.
 */
public final class IndustrialForegoingCompat {

    private static final ResourceLocation LASER_DRILL_FLUID_TYPE =
            ResourceLocation.fromNamespaceAndPath("industrialforegoing", "laser_drill_fluid");
    private static final ResourceLocation LATEX =
            ResourceLocation.fromNamespaceAndPath("industrialforegoing", "latex");
    private static final ResourceLocation PINK_SLIME =
            ResourceLocation.fromNamespaceAndPath("industrialforegoing", "pink_slime");
    private static final ResourceLocation LIQUID_MEAT =
            ResourceLocation.fromNamespaceAndPath("industrialforegoing", "meat");

    private IndustrialForegoingCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("industrialforegoing");
    }

    /** The latex fluid, or null when IF is absent. */
    public static Fluid getLatex() {
        return BuiltInRegistries.FLUID.getOptional(LATEX).orElse(null);
    }

    /** Pink slime (mob slaughter byproduct), or null when IF is absent. */
    public static Fluid getPinkSlime() {
        return BuiltInRegistries.FLUID.getOptional(PINK_SLIME).orElse(null);
    }

    /** Liquid meat (肉汤, mob slaughter byproduct), or null when IF is absent. */
    public static Fluid getLiquidMeat() {
        return BuiltInRegistries.FLUID.getOptional(LIQUID_MEAT).orElse(null);
    }

    /**
     * Looks up IF's fluid laser drill recipes for the given entity type and returns
     * the fluid it produces, or null when IF has no recipe for this entity.
     * (e.g. IF's default recipe: wither -> ether gas)
     */
    public static Fluid getLaserDrillFluid(ServerLevel level, EntityType<?> type) {
        var recipeType = BuiltInRegistries.RECIPE_TYPE.getOptional(LASER_DRILL_FLUID_TYPE).orElse(null);
        if (recipeType == null) return null;

        // The recipe type is looked up dynamically, so use a raw type to satisfy the generic signature
        @SuppressWarnings({"unchecked", "rawtypes"})
        var recipes = level.getRecipeManager().getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) recipeType);
        for (var rawHolder : recipes) {
            var recipe = ((net.minecraft.world.item.crafting.RecipeHolder<?>) rawHolder).value();
            try {
                // LaserDrillFluidRecipe extends DrillRecipe, whose fields are public
                var entityDataField = recipe.getClass().getSuperclass().getField("entityData");
                var optional = (Optional<?>) entityDataField.get(recipe);
                if (optional.isEmpty()) continue;

                var entityData = optional.get();
                var entityIngredient = entityData.getClass().getMethod("getEntity").invoke(entityData);
                var recipeEntity = (EntityType<?>) entityIngredient.getClass().getMethod("getType").invoke(entityIngredient);
                if (recipeEntity != type) continue;

                var outputField = recipe.getClass().getSuperclass().getField("output");
                var sized = (SizedFluidIngredient) outputField.get(recipe);
                var stacks = sized.ingredient().getStacks();
                if (stacks.length > 0) {
                    return stacks[0].getFluid();
                }
            } catch (ReflectiveOperationException ignored) {
                // Skip unreadable recipes — nothing else we can do without a hard dependency
            }
        }
        return null;
    }
}
