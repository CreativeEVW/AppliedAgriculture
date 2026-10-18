package com.example.appliedagriculture.registration;

import com.example.appliedagriculture.AppliedAgriculture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, AppliedAgriculture.MOD_ID);

    // Register custom recipe serializers here, e.g.:
    // public static final Supplier<RecipeSerializer<SomeRecipe>> EXAMPLE_SERIALIZER =
    //         RECIPE_SERIALIZERS.register("example", () -> new SimpleCraftingRecipeSerializer<>(SomeRecipe::new));
}
