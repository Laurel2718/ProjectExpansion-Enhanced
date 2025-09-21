package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.util.EXUtils;
import cool.furry.mc.neoforge.projectexpansion.util.IHasCapability;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.items.ItemPE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;

public class ItemFinalStar extends ItemPE implements IItemEmcHolder, IPedestalItem, IHasCapability {
    
    public ItemFinalStar() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, context, list, flag);
        list.add(Lang.Items.FINAL_STAR_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Items.FINAL_STAR_PEDESTAL_TOOLTIP.translateColored(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // ===== EMC Holder Implementation =====
    
    @Override
    public long insertEmc(@NotNull ItemStack itemStack, long emc, @NotNull IEmcStorage.EmcAction action) {
        // Final Star doesn't accept EMC input - it's already "full"
        return 0L;
    }

    @Override
    public long extractEmc(@NotNull ItemStack itemStack, long emc, @NotNull IEmcStorage.EmcAction action) {
        // Can extract any amount of EMC - infinite source
        return emc;
    }

    @Override
    public @Range(from = 0L, to = 9223372036854775807L) long getStoredEmc(@NotNull ItemStack itemStack) {
        // 1 quadrillion EMC - virtually infinite
        return 1_000_000_000_000_000L;
    }

    @Override
    public @Range(from = 1L, to = 9223372036854775807L) long getMaximumEmc(@NotNull ItemStack itemStack) {
        // Maximum possible EMC storage
        return Long.MAX_VALUE;
    }

    // ===== Pedestal Item Implementation =====
    
    @Override
    public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull PEDESTAL pedestal) {
        // Get update interval from config (default: 20 ticks = 1 second)
        int interval = Config.server.finalStarUpdateInterval.get();
        if (interval <= 0) {
            return false; // Disabled
        }

        // Only update on server side and at specified intervals
        if (!level.isClientSide && level.getGameTime() % (long) interval == EXUtils.mod(blockPos.hashCode(), interval)) {
            // Look for items above the pedestal (dropped items)
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, 
                new AABB(blockPos).inflate(0D, 1D, 0D));
            
            if (!items.isEmpty()) {
                // Check adjacent blocks for item handlers (chests, etc.)
                for (Direction facing : EXUtils.DIRECTIONS) {
                    if (facing != Direction.UP) {
                        BlockEntity blockEntity = level.getBlockEntity(blockPos.relative(facing));
                        if (blockEntity != null) {
                            var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos.relative(facing), facing.getOpposite());
                            if (itemHandler != null) {
                                // Get a random item from the dropped items
                                ItemStack sourceStack = items.get(level.random.nextInt(items.size())).getItem();
                                
                                // Check if we can copy this item
                                boolean canCopy = Config.server.finalStarCopiesAnyItem.get() || 
                                                IEMCProxy.INSTANCE.hasValue(sourceStack);
                                
                                if (canCopy) {
                                    // Create a copy with max stack size
                                    ItemStack toInsert = sourceStack.copy();
                                    toInsert.setCount(sourceStack.getMaxStackSize());
                                    
                                    // Handle NBT based on config (simplified for now)
                                    if (!Config.server.finalStarCopiesNBT.get() && !toInsert.getComponents().isEmpty()) {
                                        toInsert = new ItemStack(toInsert.getItem(), toInsert.getCount());
                                    }
                                    
                                    // Try to insert the item
                                    ItemStack remainder = ItemHandlerHelper.insertItem(itemHandler, toInsert, false);
                                    if (remainder.getCount() < toInsert.getCount()) {
                                        return true; // Successfully inserted at least some items
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @NotNull List<Component> getPedestalDescription(float stackSize) {
        return List.of(Lang.Items.FINAL_STAR_PEDESTAL_DESCRIPTION.translate());
    }
    
    // ===== Capability Registration =====
    
    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register EMC Holder capability
        event.registerItem(
            moze_intel.projecte.api.capabilities.PECapabilities.EMC_HOLDER_ITEM_CAPABILITY,
            (stack, context) -> this,
            this
        );
        
        // Register Pedestal Item capability  
        event.registerItem(
            moze_intel.projecte.api.capabilities.PECapabilities.PEDESTAL_ITEM_CAPABILITY,
            (stack, context) -> this,
            this
        );
    }
}