package com.example.appliedagriculture.client.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** A display recipe for the biomaterial synthesis chamber. The input is not consumed. */
public record BiomaterialSynthesisChamberRecipe(ItemStack input, List<ItemStack> outputs,
                                                 List<FluidStack> fluids) {
}
