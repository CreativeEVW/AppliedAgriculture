package com.example.appliedagriculture.network;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.menu.AEBaseMenu;
import appeng.util.EnumCycler;
import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.api.AASettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Serverbound packet cycling a custom config-manager setting
 * (copied from AppliedGenerators' AGConfigButtonPacket, without glodium).
 */
public record AAConfigButtonPacket(Setting<?> option, boolean rotationDirection) implements CustomPacketPayload {

    public static final Type<AAConfigButtonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AppliedAgriculture.MOD_ID, "config_button"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AAConfigButtonPacket> STREAM_CODEC =
            StreamCodec.ofMember(AAConfigButtonPacket::write, AAConfigButtonPacket::decode);

    @Override
    public @NotNull Type<AAConfigButtonPacket> type() {
        return TYPE;
    }

    public static AAConfigButtonPacket decode(RegistryFriendlyByteBuf stream) {
        var option = AASettings.getOrThrow(stream.readUtf());
        var rotationDirection = stream.readBoolean();
        return new AAConfigButtonPacket(option, rotationDirection);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeUtf(option.getName());
        data.writeBoolean(rotationDirection);
    }

    public static void handle(AAConfigButtonPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.containerMenu instanceof AEBaseMenu baseMenu) {
                if (baseMenu.getTarget() instanceof IConfigurableObject configurableObject) {
                    var cm = configurableObject.getConfigManager();
                    if (cm.hasSetting(packet.option())) {
                        cycleSetting(cm, packet.option(), packet.rotationDirection());
                    }
                }
            }
        });
    }

    private static <T extends Enum<T>> void cycleSetting(IConfigManager cm, Setting<T> setting, boolean backwards) {
        var currentValue = cm.getSetting(setting);
        var nextValue = EnumCycler.rotateEnum(currentValue, backwards, setting.getValues());
        cm.putSetting(setting, nextValue);
    }
}
