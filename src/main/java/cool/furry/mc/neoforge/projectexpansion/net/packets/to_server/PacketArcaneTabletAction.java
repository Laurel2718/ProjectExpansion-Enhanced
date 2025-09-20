package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

public record PacketArcaneTabletAction(String action, boolean shiftHeld) implements IPacket {
    public static final CustomPacketPayload.Type<PacketArcaneTabletAction> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_tablet_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTabletAction> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketArcaneTabletAction::action,
            ByteBufCodecs.BOOL, PacketArcaneTabletAction::shiftHeld,
            PacketArcaneTabletAction::new
    );

    // Valid action whitelist
    private static final Set<String> VALID_ACTIONS = Set.of(
        "clear", "rotate", "balance", "learn", "unlearn", "burn", "burn_one", "batch_craft"
    );
    
    @Override
    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.containerMenu instanceof ContainerArcaneTablet arcaneTabletContainer) {
                // Input validation: check if action is in whitelist
                if (action.startsWith("extract:")) {
                    // Extract operation requires additional resource ID format validation
                    String itemId = action.substring(8);
                    if (itemId.length() > 256 || !isValidResourceLocation(itemId)) {
                        return; // Reject invalid resource ID
                    }
                } else if (!VALID_ACTIONS.contains(action)) {
                    return; // Reject unknown operation
                }
                
                arcaneTabletContainer.performAction(action, shiftHeld);
            }
        }
    }
    
    private boolean isValidResourceLocation(String resourceId) {
        try {
            ResourceLocation.parse(resourceId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}




