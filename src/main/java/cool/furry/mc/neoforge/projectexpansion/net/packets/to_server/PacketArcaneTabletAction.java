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
        // System.out.println("DEBUG: PacketArcaneTabletAction received: action='" + action + "', shiftHeld=" + shiftHeld);
        if (context.player() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.containerMenu instanceof ContainerArcaneTablet arcaneTabletContainer) {
                // Input validation: check if action is in whitelist
                if (action.startsWith("extract_itemhash:")) {
                    // Extract by hash operation - validate format: itemId@hash:hashCode
                    String hashData = action.substring(17);
                    if (hashData.length() > 512 || !isValidHashFormat(hashData)) {
                        // System.out.println("DEBUG: Rejecting invalid hash format: " + hashData);
                        return; // Reject invalid hash format
                    }
                    // System.out.println("DEBUG: Hash format validation passed");
                } else if (action.startsWith("extract_iteminfo:")) {
                    // Extract by ItemInfo string operation
                    String itemInfoStr = action.substring(17);
                    if (itemInfoStr.length() > 1024) {
                        return; // Reject overly long ItemInfo strings
                    }
                } else if (action.startsWith("extract:")) {
                    // Extract operation requires additional resource ID format validation
                    String itemId = action.substring(8);
                    if (itemId.length() > 256 || !isValidResourceLocation(itemId)) {
                        return; // Reject invalid resource ID
                    }
                } else if (!VALID_ACTIONS.contains(action)) {
                    return; // Reject unknown operation
                }
                
                // System.out.println("DEBUG: Calling arcaneTabletContainer.performAction with: '" + action + "'");
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
    
    private boolean isValidHashFormat(String hashData) {
        try {
            String[] parts = hashData.split("@hash:");
            if (parts.length != 2) {
                return false;
            }
            // Validate resource location
            ResourceLocation.parse(parts[0]);
            // Validate hash code is a number
            Integer.parseInt(parts[1]);
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




