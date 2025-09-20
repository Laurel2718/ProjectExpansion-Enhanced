package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet for transferring JEI recipes to Arcane Tablet
 */
public record PacketArcaneTabletRecipeTransfer(Int2ObjectMap<List<ItemStack>> stacksMap, boolean maxTransfer) implements IPacket {
    public static final CustomPacketPayload.Type<PacketArcaneTabletRecipeTransfer> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_tablet_recipe_transfer"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTabletRecipeTransfer> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, PacketArcaneTabletRecipeTransfer packet) {
            buf.writeBoolean(packet.maxTransfer);
            buf.writeInt(packet.stacksMap.size());
            for (Int2ObjectMap.Entry<List<ItemStack>> entry : packet.stacksMap.int2ObjectEntrySet()) {
                buf.writeInt(entry.getIntKey());
                buf.writeInt(entry.getValue().size());
                for (ItemStack stack : entry.getValue()) {
                    ItemStack.STREAM_CODEC.encode(buf, stack);
                }
            }
        }
        
        @Override
        public PacketArcaneTabletRecipeTransfer decode(RegistryFriendlyByteBuf buf) {
            boolean maxTransfer = buf.readBoolean();
            int mapSize = buf.readInt();
            Int2ObjectMap<List<ItemStack>> stacksMap = new Int2ObjectOpenHashMap<>();
            
            for (int i = 0; i < mapSize; i++) {
                int key = buf.readInt();
                int listSize = buf.readInt();
                List<ItemStack> stacks = new ArrayList<>();
                for (int j = 0; j < listSize; j++) {
                    stacks.add(ItemStack.STREAM_CODEC.decode(buf));
                }
                stacksMap.put(key, stacks);
            }
            
            return new PacketArcaneTabletRecipeTransfer(stacksMap, maxTransfer);
        }
    };

    @Override
    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.containerMenu instanceof ContainerArcaneTablet arcaneTabletContainer) {
                arcaneTabletContainer.transferItems(stacksMap);
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
