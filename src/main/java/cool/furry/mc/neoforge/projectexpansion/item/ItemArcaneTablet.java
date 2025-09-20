package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;

import java.util.List;

public class ItemArcaneTablet extends Item {
    
    public ItemArcaneTablet() {
        super(new Properties()
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .fireResistant()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new ArcaneTabletMenuProvider(hand);
            serverPlayer.openMenu(provider, buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Lang.Items.ARCANE_TABLET_TOOLTIP.translateColored(ChatFormatting.GRAY));
        tooltip.add(Lang.Items.ARCANE_TABLET_TOOLTIP_2.translateColored(ChatFormatting.DARK_GRAY));
    }
    
    /**
     * Save crafting matrix items to the Arcane Tablet using DataComponents
     */
    public static void saveCraftingMatrix(ItemStack tablet, ItemStackHandler craftingMatrix, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack[] slots = new ItemStack[9];
        boolean hasItems = false;
        
        // Convert ItemStackHandler to array (3x3 = 9 slots)
        for (int i = 0; i < Math.min(craftingMatrix.getSlots(), 9); i++) {
            ItemStack stack = craftingMatrix.getStackInSlot(i);
            slots[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            if (!stack.isEmpty()) {
                hasItems = true;
            }
        }
        
        // Save using DataComponent
        if (hasItems) {
            DataComponentTypes.CraftingMatrixData matrixData = DataComponentTypes.CraftingMatrixData.create(slots);
            tablet.set(DataComponentTypes.CRAFTING_MATRIX.get(), matrixData);
        } else {
            // Remove component if no items to save
            tablet.remove(DataComponentTypes.CRAFTING_MATRIX.get());
        }
    }
    
    /**
     * Load crafting matrix items from the Arcane Tablet using DataComponents
     */
    public static void loadCraftingMatrix(ItemStack tablet, ItemStackHandler craftingMatrix, net.minecraft.core.HolderLookup.Provider registries) {
        // First clear all slots
        for (int i = 0; i < craftingMatrix.getSlots(); i++) {
            craftingMatrix.setStackInSlot(i, ItemStack.EMPTY);
        }
        
        DataComponentTypes.CraftingMatrixData matrixData = tablet.get(DataComponentTypes.CRAFTING_MATRIX.get());
        if (matrixData != null && matrixData.items() != null) {
            // Restore items to their specific slots using SlotData
            for (DataComponentTypes.SlotData slotData : matrixData.items()) {
                int slotIndex = slotData.slotIndex();
                ItemStack item = slotData.item();
                
                if (slotIndex >= 0 && slotIndex < craftingMatrix.getSlots() && !item.isEmpty()) {
                    craftingMatrix.setStackInSlot(slotIndex, item.copy());
                }
            }
        }
    }

    private record ArcaneTabletMenuProvider(InteractionHand hand) implements MenuProvider {
        @Override
        public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
            return new ContainerArcaneTablet(windowId, playerInventory, hand);
        }

        @Override
        public Component getDisplayName() {
            return Lang.Items.ARCANE_TABLET.translate();
        }
    }
}
