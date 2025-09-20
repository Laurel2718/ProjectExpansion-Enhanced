package cool.furry.mc.neoforge.projectexpansion.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.util.BasicDataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.TagNames;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataComponentTypes {
    public static final DeferredRegister.DataComponents Registry = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Main.MOD_ID);

    public record OwnerData(UUID uuid, String name) {
        public static final Codec<OwnerData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(UUIDUtil.CODEC.fieldOf(TagNames.OWNER).forGetter(OwnerData::uuid), Codec.STRING.fieldOf(TagNames.OWNER_NAME).forGetter(OwnerData::name)).apply(instance, OwnerData::new)
        );
        public static final StreamCodec<ByteBuf, OwnerData> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, OwnerData::uuid, ByteBufCodecs.STRING_UTF8, OwnerData::name, OwnerData::new);
    }
    
    /**
     * Crafting matrix slot data, stores individual item and its slot position
     */
    public record SlotData(int slotIndex, ItemStack item) {
        public static final Codec<SlotData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                    Codec.INT.fieldOf("slot").forGetter(SlotData::slotIndex),
                    ItemStack.CODEC.fieldOf("item").forGetter(SlotData::item)
                ).apply(instance, SlotData::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, SlotData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SlotData::slotIndex,
                ItemStack.STREAM_CODEC, SlotData::item,
                SlotData::new);
    }

    /**
     * Crafting matrix data component, stores only non-empty items and their positions
     * Data component for storing Arcane Tablet crafting matrix items
     */
    public record CraftingMatrixData(List<SlotData> items) {
        // Create crafting matrix data, saving only non-empty items
        public static CraftingMatrixData create(ItemStack[] craftingSlots) {
            List<SlotData> items = new ArrayList<>();
            
            // Only save non-empty items with their slot indices
            for (int i = 0; i < Math.min(craftingSlots.length, 9); i++) {
                ItemStack stack = craftingSlots[i];
                if (stack != null && !stack.isEmpty()) {
                    // Safety check: limit item count to prevent abnormally large stacks
                    ItemStack safeCopy = stack.copy();
                    if (safeCopy.getCount() > safeCopy.getMaxStackSize()) {
                        safeCopy.setCount(safeCopy.getMaxStackSize());
                    }
                    
                    // Clean potentially dangerous NBT data from items
                    safeCopy = sanitizeItemStack(safeCopy);
                    
                    items.add(new SlotData(i, safeCopy));
                }
            }
            
            return new CraftingMatrixData(items);
        }
        
        /**
         * Clean potentially dangerous data from ItemStack
         */
        private static ItemStack sanitizeItemStack(ItemStack stack) {
            // Remove potentially dangerous components, keep only necessary base data
            ItemStack sanitized = new ItemStack(stack.getItem(), stack.getCount());
            
            // Only keep safe components like damage values and other basic attributes
            if (stack.isDamageableItem()) {
                sanitized.setDamageValue(Math.min(stack.getDamageValue(), stack.getMaxDamage()));
            }
            
            return sanitized;
        }
        
        public static final Codec<CraftingMatrixData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(SlotData.CODEC.listOf().fieldOf("items").forGetter(CraftingMatrixData::items)).apply(instance, CraftingMatrixData::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftingMatrixData> STREAM_CODEC = StreamCodec.composite(
                SlotData.STREAM_CODEC.apply(ByteBufCodecs.list()), CraftingMatrixData::items, CraftingMatrixData::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OwnerData>> OWNER = Registry.registerComponentType("uuid", (builder) -> builder.persistent(OwnerData.CODEC).networkSynchronized(OwnerData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapabilityAlchemicalBookLocations.AlchemicalBookLocationData>> ALCHEMICAL_BOOK_LOCATIONS = Registry.registerComponentType("alchemical_book_locations", (builder) -> builder.persistent(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.CODEC).networkSynchronized(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> LAST_USED = Registry.registerComponentType("last_used", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> KNOWLEDGE_GAINED = Registry.registerComponentType("knowledge_gained", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));
    
    // Arcane Tablet crafting matrix persistence
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CraftingMatrixData>> CRAFTING_MATRIX = Registry.registerComponentType("crafting_matrix", (builder) -> builder.persistent(CraftingMatrixData.CODEC).networkSynchronized(CraftingMatrixData.STREAM_CODEC));



}
