package com.example.appliedagriculture.client;

import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import com.example.appliedagriculture.api.AASettings;
import com.example.appliedagriculture.client.button.AAIcon;
import com.example.appliedagriculture.client.button.AAServerSettingToggleButton;
import com.example.appliedagriculture.client.button.ActionEPPButton;
import com.example.appliedagriculture.client.gui.subgui.OutputSideConfig;
import com.example.appliedagriculture.menu.BiomaterialSynthesisChamberMenu;
import com.example.appliedagriculture.registration.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BiomaterialSynthesisChamberScreen extends UpgradeableScreen<BiomaterialSynthesisChamberMenu> {

    private final AAServerSettingToggleButton<YesNo> exportBtn;
    private final ActionEPPButton outputSideBtn;

    public BiomaterialSynthesisChamberScreen(BiomaterialSynthesisChamberMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.exportBtn = new AAServerSettingToggleButton<>(AASettings.ME_EXPORT, YesNo.NO);
        this.addToLeftToolbar(exportBtn);

        this.outputSideBtn = new ActionEPPButton(b -> this.openOutputConfig(), AAIcon.OUTPUT_SIDES.getBlitter());
        this.outputSideBtn.setMessage(Component.translatable("gui.appliedagriculture.set_output_sides.open"));
        this.addToLeftToolbar(this.outputSideBtn);
    }

    private void openOutputConfig() {
        if (this.getMenu().getHost() != null) {
            switchToScreen(new OutputSideConfig<>(
                    this,
                    new ItemStack(ModItems.BIOMATERIAL_SYNTHESIS_CHAMBER.get()),
                    this.getMenu().getHost(),
                    this.getMenu().getOutputSides(),
                    OutputSideConfig::sendToggle));
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        setTextContent("status", Component.translatable(switch (menu.status) {
            case 1 -> "gui.appliedagriculture.status_working";
            case 2 -> "gui.appliedagriculture.status_missing_power";
            default -> "gui.appliedagriculture.status_idle";
        }));

        this.exportBtn.set(menu.meExport);
        // Output direction button only shows when products go to adjacent containers
        this.outputSideBtn.setVisibility(this.exportBtn.getCurrentValue() == YesNo.NO);
    }
}
