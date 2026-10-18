package com.example.appliedagriculture.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Optional integration with MESpawner's monster disks.
 *
 * MESpawner is NOT a required dependency: all access goes through reflection, and this
 * class is only ever touched from code guarded by {@link #isLoaded()}. Never call any
 * method here (not even isLoaded() from a static initializer elsewhere) without the
 * guard — loading MonsterDiskItem when MESpawner is absent throws NoClassDefFoundError.
 */
public final class MESpawnerCompat {

    private static final String MONSTER_DISK_CLASS = "com.example.mespawner.item.MonsterDiskItem";

    private MESpawnerCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("mespawner");
    }

    /** Whether the stack's item is a MESpawner monster disk. */
    public static boolean isMonsterDisk(ItemStack stack) {
        return isMonsterDisk(stack.getItem());
    }

    public static boolean isMonsterDisk(Item item) {
        try {
            return Class.forName(MONSTER_DISK_CLASS).isInstance(item);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * The entity types "formed" on the disk (kill count above the threshold).
     * Returns an empty list on any failure — the disk is then treated as empty.
     */
    @SuppressWarnings("unchecked")
    public static List<EntityType<?>> getFormedEntityTypes(ItemStack stack, Level level) {
        try {
            var method = stack.getItem().getClass().getMethod(
                    "getFormedEntityTypes", ItemStack.class, Level.class);
            return (List<EntityType<?>>) method.invoke(stack.getItem(), stack, level);
        } catch (ReflectiveOperationException e) {
            return List.of();
        }
    }
}
