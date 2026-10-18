package com.example.appliedagriculture.network;

import appeng.menu.AEBaseMenu;
import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.blockentity.IOutputSideConfigurable;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Serverbound packet toggling an output side on the machine. */
public record AAOutputSidePacket(String side, boolean value) implements CustomPacketPayload {

    public static final Type<AAOutputSidePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AppliedAgriculture.MOD_ID, "output_side"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AAOutputSidePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AAOutputSidePacket::side,
            ByteBufCodecs.BOOL, AAOutputSidePacket::value,
            AAOutputSidePacket::new);

    @Override
    public @NotNull Type<AAOutputSidePacket> type() {
        return TYPE;
    }

    public static void handle(AAOutputSidePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp
                    && sp.containerMenu instanceof AEBaseMenu menu
                    && menu.getTarget() instanceof IOutputSideConfigurable host) {
                host.setOutputSide(Direction.byName(packet.side()), packet.value());
            }
        });
    }
}
