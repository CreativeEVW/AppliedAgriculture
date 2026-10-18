package com.example.appliedagriculture.client.button;

import appeng.api.config.Setting;
import com.example.appliedagriculture.network.AAConfigButtonPacket;
import net.neoforged.neoforge.network.PacketDistributor;

/** Serverbound variant of the setting toggle button (copied from AppliedGenerators' AGServerSettingToggleButton). */
public class AAServerSettingToggleButton<T extends Enum<T>> extends AASettingToggleButton<T> {

    public AAServerSettingToggleButton(Setting<T> setting, T val) {
        super(setting, val, AAServerSettingToggleButton::sendToServer);
    }

    private static <T extends Enum<T>> void sendToServer(AASettingToggleButton<T> button, boolean backwards) {
        PacketDistributor.sendToServer(new AAConfigButtonPacket(button.getSetting(), backwards));
    }
}
