package com.example.appliedagriculture.menu;

import appeng.api.config.YesNo;
import appeng.api.util.IConfigManager;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.api.AASettings;
import com.example.appliedagriculture.blockentity.BiomaterialSynthesisChamberBlockEntity;
import com.example.appliedagriculture.menu.helper.DirectionSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class BiomaterialSynthesisChamberMenu extends UpgradeableMenu<BiomaterialSynthesisChamberBlockEntity> {

    public static final MenuType<BiomaterialSynthesisChamberMenu> TYPE = MenuTypeBuilder
            .create(BiomaterialSynthesisChamberMenu::new, BiomaterialSynthesisChamberBlockEntity.class)
            .withMenuTitle(host -> Component.translatable("block.appliedagriculture.biomaterial_synthesis_chamber"))
            .buildUnregistered(ResourceLocation.fromNamespaceAndPath(AppliedAgriculture.MOD_ID, "biomaterial_synthesis_chamber"));

    @GuiSync(20)
    public int status;

    @GuiSync(22)
    public YesNo meExport = YesNo.YES;

    @GuiSync(23)
    public DirectionSet outputSides = new DirectionSet(new java.util.ArrayList<>());

    public BiomaterialSynthesisChamberMenu(int id, Inventory playerInventory, BiomaterialSynthesisChamberBlockEntity host) {
        super(TYPE, id, playerInventory, host);
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.meExport = this.getHost().getConfigManager().getSetting(AASettings.ME_EXPORT);
        this.outputSides.clear();
        this.outputSides.addAll(this.getHost().getOutputSides());
    }

    public java.util.List<net.minecraft.core.Direction> getOutputSides() {
        return outputSides.sides();
    }

    @Override
    protected void setupInventorySlots() {
        // Spawn egg / monster disk / sapling / mushroom slot (left of center)
        addSlot(new AppEngSlot(getHost().eggSlot, 0) {
            @Override public boolean mayPlace(ItemStack s) {
                return BiomaterialSynthesisChamberBlockEntity.isValidInput(s);
            }
            @Override public int getMaxStackSize() { return 1; }
        }, SlotSemantics.STORAGE);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            status = getHost().getStatus();
        }
        super.broadcastChanges();
    }
}
