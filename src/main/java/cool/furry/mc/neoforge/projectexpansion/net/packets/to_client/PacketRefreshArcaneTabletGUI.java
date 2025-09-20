package cool.furry.mc.neoforge.projectexpansion.net.packets.to_client;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to refresh the Arcane Tablet GUI after knowledge changes
 */
public record PacketRefreshArcaneTabletGUI() implements IPacket {
    public static final CustomPacketPayload.Type<PacketRefreshArcaneTabletGUI> TYPE = new CustomPacketPayload.Type<>(Main.rl("refresh_arcane_tablet_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRefreshArcaneTabletGUI> STREAM_CODEC = StreamCodec.unit(new PacketRefreshArcaneTabletGUI());

    @Override
    public void handle(IPayloadContext context) {
        // Ensure this runs on the client side
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GUIArcaneTablet arcaneTabletGUI) {
                arcaneTabletGUI.refreshItemList();
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
