package com.example.appliedagriculture.registration;

import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.item.MilkProteinItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AppliedAgriculture.MOD_ID);

    public static final DeferredItem<BlockItem> CROP_ASSEMBLY_CHAMBER = ITEMS.register("crop_assembly_chamber",
            () -> new BlockItem(ModBlocks.CROP_ASSEMBLY_CHAMBER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> BIOMATERIAL_SYNTHESIS_CHAMBER = ITEMS.register("biomaterial_synthesis_chamber",
            () -> new BlockItem(ModBlocks.BIOMATERIAL_SYNTHESIS_CHAMBER.get(), new Item.Properties()));

    // Biomaterial synthesis products
    public static final DeferredItem<Item> MILK_PROTEIN = ITEMS.register("milk_protein",
            // Edible: 3 hunger + 3 saturation (0.5 modifier), clears all effects like milk
            () -> new MilkProteinItem(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build())));
    public static final DeferredItem<Item> FUNGUS_MIXTURE = ITEMS.register("fungus_mixture",
            // Edible directly, same stats as mushroom stew
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build())));

    // Upgrade cards — must be AE2 UpgradeCardItem for native upgrade slots
    public static final DeferredItem<Item> AMPLIFICATION_CARD = ITEMS.register("amplification_card",
            () -> appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> YIELD_SYNTHESIS_CARD = ITEMS.register("yield_synthesis_card",
            () -> appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(64)));
}
