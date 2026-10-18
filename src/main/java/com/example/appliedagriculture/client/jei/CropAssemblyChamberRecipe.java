package com.example.appliedagriculture.client.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A display recipe for the crop assembly chamber. The input is not consumed. */
public record CropAssemblyChamberRecipe(ItemStack input, List<ItemStack> outputs) {
}
