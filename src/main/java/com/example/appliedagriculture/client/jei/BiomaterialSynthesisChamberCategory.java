package com.example.appliedagriculture.client.jei;

import com.example.appliedagriculture.compat.IndustrialForegoingCompat;
import com.example.appliedagriculture.registration.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class BiomaterialSynthesisChamberCategory implements IRecipeCategory<BiomaterialSynthesisChamberRecipe> {

    public static final RecipeType<BiomaterialSynthesisChamberRecipe> TYPE =
            RecipeType.create("appliedagriculture", "biomaterial_synthesis_chamber", BiomaterialSynthesisChamberRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public BiomaterialSynthesisChamberCategory(IJeiHelpers helpers) {
        var guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 92);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIOMATERIAL_SYNTHESIS_CHAMBER.get()));
    }

    @Override
    public RecipeType<BiomaterialSynthesisChamberRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.appliedagriculture.biomaterial_synthesis_chamber");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BiomaterialSynthesisChamberRecipe recipe, IFocusGroup focuses) {
        // Catalyst = the machine's template item is not consumed
        builder.addSlot(RecipeIngredientRole.CATALYST, 24, 38).addItemStack(recipe.input());

        int x = 110;
        for (var output : recipe.outputs()) {
            builder.addOutputSlot(x, 38).addItemStack(output);
            x += 22;
        }
        int fx = 110;
        for (var fluidStack : recipe.fluids()) {
            builder.addOutputSlot(fx, 60).addFluidStack(fluidStack.getFluid(), fluidStack.getAmount());
            fx += 22;
        }
    }

    @Override
    public void draw(BiomaterialSynthesisChamberRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics g,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("jei.appliedagriculture.bio_costs"), 8, 6, 0xFF3F3F3F, false);
        g.drawString(font, Component.translatable("jei.appliedagriculture.input_not_consumed"), 8, 64, 0xFF3F3F3F, false);
        if (IndustrialForegoingCompat.isLoaded()) {
            g.drawString(font, Component.translatable("jei.appliedagriculture.if_dynamic_note"), 8, 78, 0xFF7F7F7F, false);
        }
    }
}
