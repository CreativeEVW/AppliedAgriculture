package com.example.appliedagriculture.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Edible milk protein: restores hunger and clears all effects, like milk.
 * Also substitutes for milk buckets in recipes (e.g. milk protein + dirt -> milk mud).
 */
public class MilkProteinItem extends Item {

    public MilkProteinItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            // Same as drinking a milk bucket
            entity.removeAllEffects();
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
