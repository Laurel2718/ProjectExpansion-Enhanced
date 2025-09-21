package cool.furry.mc.neoforge.projectexpansion.gui.container;

import cool.furry.mc.neoforge.projectexpansion.net.PacketHandler;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTabletAction;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketRefreshArcaneTabletGUI;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import cool.furry.mc.neoforge.projectexpansion.util.EXUtils;
import cool.furry.mc.neoforge.projectexpansion.item.ItemArcaneTablet;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Comparator;

public class ContainerArcaneTablet extends ContainerBase {
    private static final int[] ROTATION_SLOTS = {0, 1, 2, 5, 8, 7, 6, 3};
    private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    
    private final Player player;
    private final InteractionHand hand;
    private final IKnowledgeProvider knowledgeProvider;
    private final CraftingContainer craftMatrix;
    private final ResultContainer craftResult;
    private final ItemStackHandler itemHandler;
    private final int playerInventoryStart;

    public ContainerArcaneTablet(int windowId, Inventory playerInv, InteractionHand hand) {
        super(MenuTypes.ARCANE_TABLET.get(), windowId, playerInv);
        this.player = playerInv.player;
        this.hand = hand;
        this.knowledgeProvider = Util.getKnowledgeProvider(player);
        this.itemHandler = new ItemStackHandler(9);
        this.craftMatrix = new ArcaneTabletCraftingContainer(this, 3, 3, itemHandler);
        this.craftResult = new ResultContainer();

        // Add crafting result slot - positioned in the left side area
        addSlot(new ArcaneTabletResultSlot(player, craftMatrix, craftResult, 0, -23, 75));
        
        // Add crafting grid slots (3x3) - positioned in the left side area  
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new Slot(craftMatrix, x + y * 3, -59 + x * 18, 17 + y * 18));
            }
        }
        
        // Add player inventory - record the starting index
        this.playerInventoryStart = slots.size(); // Current number of slots before adding player inventory
        addPlayerInventory(8, 135);
        
        // Load saved crafting matrix from the Arcane Tablet NBT
        ItemStack tabletStack = player.getItemInHand(hand);
        if (tabletStack.getItem() instanceof ItemArcaneTablet) {
            ItemArcaneTablet.loadCraftingMatrix(tabletStack, itemHandler, player.level().registryAccess());
        }

        // Initialize crafting result
        slotChangedCraftingGrid(this, player.level(), player, craftMatrix, craftResult);
    }

    public ContainerArcaneTablet(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, buf.readEnum(InteractionHand.class));
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack heldItem = player.getItemInHand(hand);
        return heldItem.getItem() instanceof cool.furry.mc.neoforge.projectexpansion.item.ItemArcaneTablet;
    }

    public void handleButtonPress(String action, boolean shiftHeld) {
        // This method should only be called on the server side
        // The EXButton already sends the network packet from client to server
        performAction(action, shiftHeld);
    }

    public void performAction(String action, boolean shiftHeld) {
        switch (action) {
            case "clear" -> clearCraftingMatrix();
            case "rotate" -> rotateCraftingMatrix(!shiftHeld);
            case "balance" -> {
                if (shiftHeld) {
                    spreadCraftingMatrix();
                } else {
                    balanceCraftingMatrix();
                }
            }
            case "learn" -> learnItem();
            case "unlearn" -> unlearnItem();
            case "burn" -> burnItem(true); // Default to burning all when using button
            case "burn_one" -> burnItem(false); // Burn one item
            case "batch_craft" -> performBatchCraftingAction(); // Ctrl+Left click batch crafting
        }
        
        // Handle item extraction
        if (action.startsWith("extract:")) {
            tryExtractItem(action.substring(8), shiftHeld);
        }
    }

    // EMC extraction rate limiting (max extractions per second) - Performance optimized
    private static final int MAX_EXTRACTIONS_PER_SECOND = 20;
    private static final int RATE_LIMIT_WINDOW_MS = 1000;
    private static final int CLEANUP_INTERVAL_MS = 30000; // Clean up every 30 seconds
    private static final Map<UUID, ExtractionTracker> playerExtractionTimes = new ConcurrentHashMap<>();
    private static volatile long lastCleanupTime = System.currentTimeMillis();
    
    // Performance optimized extraction tracker
    private static class ExtractionTracker {
        private final long[] timestamps = new long[MAX_EXTRACTIONS_PER_SECOND];
        private int index = 0;
        private int count = 0;
        
        synchronized boolean canExtract(long currentTime) {
            // Remove old entries efficiently
            while (count > 0 && (currentTime - timestamps[index]) > RATE_LIMIT_WINDOW_MS) {
                count--;
                index = (index + 1) % MAX_EXTRACTIONS_PER_SECOND;
            }
            
            if (count >= MAX_EXTRACTIONS_PER_SECOND) {
                return false;
            }
            
            // Add new timestamp
            int writeIndex = (index + count) % MAX_EXTRACTIONS_PER_SECOND;
            timestamps[writeIndex] = currentTime;
            count++;
            return true;
        }
    }
    
    private void tryExtractItem(String itemId, boolean pullStack) {
        // Rate limiting check - performance optimized
        if (!checkExtractionRateLimit(player.getUUID())) {
            return;
        }
        
        try {
            ResourceLocation id = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != Items.AIR && knowledgeProvider != null) {
                BigInteger availableEMC = knowledgeProvider.getEmc();
                long itemValue = IEMCProxy.INSTANCE.getValue(item);
                BigInteger emc = BigInteger.valueOf(itemValue);
                
                // Prevent negative and zero values
                if (emc.equals(BigInteger.ZERO) || itemValue <= 0) {
                    return;
                }
                
                // Safe division calculation to prevent overflow
                BigInteger bigAvail;
                try {
                    bigAvail = availableEMC.divide(emc);
                } catch (ArithmeticException e) {
                    return; // Division by zero or other arithmetic exception
                }
                
                // Limit maximum available quantity to prevent integer overflow
                int available = Math.min(
                    bigAvail.compareTo(MAX_INT) >= 0 ? Integer.MAX_VALUE : bigAvail.intValue(),
                    64 // Limit single extraction to max stack size
                );
            
            if (available > 0) {
                ItemStack stack = new ItemStack(item);
                BigInteger cost = BigInteger.ZERO;
                
                if (pullStack) {
                    // Give full stack to player
                    int stackSize = Math.min(stack.getMaxStackSize(), available);
                    stack.setCount(stackSize);
                    player.getInventory().placeItemBackInInventory(stack, true);
                    cost = emc.multiply(BigInteger.valueOf(stackSize));
                } else {
                    // Add single item to cursor
                    if (getCarried().isEmpty()) {
                        setCarried(stack);
                        cost = emc;
                    } else if (getCarried().getCount() < getCarried().getMaxStackSize() && 
                               getCarried().getItem() == stack.getItem()) {
                        getCarried().grow(1);
                        cost = emc;
                    }
                }
                
                if (!cost.equals(BigInteger.ZERO)) {
                    knowledgeProvider.setEmc(availableEMC.subtract(cost));
                    if (player instanceof ServerPlayer serverPlayer) {
                        knowledgeProvider.sync(serverPlayer);
                    }
                }
            }
        }
        } catch (Exception e) {
            // Handle ResourceLocation parsing exceptions and other errors
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.disconnect(net.minecraft.network.chat.Component.literal("Invalid item extraction request"));
            }
        }
    }
    
    /**
     * Check player's EMC extraction rate limiting - Performance optimized
     * Uses circular buffer to avoid frequent memory allocations
     */
    private boolean checkExtractionRateLimit(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        
        // Periodic cleanup to prevent memory leaks
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanupInactiveTrackers(currentTime);
            lastCleanupTime = currentTime;
        }
        
        ExtractionTracker tracker = playerExtractionTimes.computeIfAbsent(playerId, k -> new ExtractionTracker());
        return tracker.canExtract(currentTime);
    }
    
    /**
     * Clean up inactive player trackers to prevent memory leaks
     */
    private static void cleanupInactiveTrackers(long currentTime) {
        playerExtractionTimes.entrySet().removeIf(entry -> {
            ExtractionTracker tracker = entry.getValue();
            synchronized (tracker) {
                // Remove tracker if no recent activity
                return tracker.count == 0 || 
                       (currentTime - tracker.timestamps[tracker.index]) > CLEANUP_INTERVAL_MS;
            }
        });
    }

    private void tryAddKnowledge(ItemStack stack) {
        if (knowledgeProvider != null) {
            EXUtils.KnowledgeAddResult result = EXUtils.addKnowledge(player, knowledgeProvider, stack);
            if (result == EXUtils.KnowledgeAddResult.ADDED && player instanceof ServerPlayer serverPlayer) {
                ItemInfo itemInfo = ItemInfo.fromStack(stack);
                ItemInfo cleaned = IEMCProxy.INSTANCE.getPersistentInfo(itemInfo);
                knowledgeProvider.syncKnowledgeChange(serverPlayer, cleaned, true);
                knowledgeProvider.sync(serverPlayer);
                // Send refresh packet to update GUI
                PacketHandler.sendToClient(serverPlayer, new PacketRefreshArcaneTabletGUI());
            }
        }
    }

    private void learnItem() {
        ItemStack cursorStack = getCarried();
        if (!cursorStack.isEmpty() && knowledgeProvider != null) {
            ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(cursorStack)).createStack();
            tryAddKnowledge(fixed);
        }
    }

    private void unlearnItem() {
        ItemStack cursorStack = getCarried();
        if (!cursorStack.isEmpty() && knowledgeProvider != null) {
            ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(cursorStack)).createStack();
            if (knowledgeProvider.removeKnowledge(fixed) && player instanceof ServerPlayer serverPlayer) {
                ItemInfo itemInfo = ItemInfo.fromStack(fixed);
                ItemInfo cleaned = IEMCProxy.INSTANCE.getPersistentInfo(itemInfo);
                knowledgeProvider.syncKnowledgeChange(serverPlayer, cleaned, false); // false = removing knowledge
                knowledgeProvider.sync(serverPlayer);
                // Send refresh packet to update GUI
                PacketHandler.sendToClient(serverPlayer, new PacketRefreshArcaneTabletGUI());
            }
        }
    }

    private void burnItem(boolean burnAll) {
        ItemStack cursorStack = getCarried();
        if (!cursorStack.isEmpty() && knowledgeProvider != null) {
            // PROTECTION: Prevent burning the Arcane Tablet that is currently being used
            if (cursorStack.getItem() instanceof ItemArcaneTablet) {
                ItemStack heldTablet = player.getItemInHand(hand);
                if (cursorStack == heldTablet) {
                    // This is the exact same ItemStack instance that opened this GUI - don't burn it
                    return;
                }
            }
            
            if (IEMCProxy.INSTANCE.hasValue(cursorStack)) {
                ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(cursorStack)).createStack();
                
                // Always try to add knowledge when burning
                tryAddKnowledge(fixed);
                
                long value = IEMCProxy.INSTANCE.getValue(fixed);
                long totalToAdd;
                
                if (burnAll) {
                    // Left click or Shift+left click from inventory: burn entire stack
                    totalToAdd = (long) (value * cursorStack.getCount() * ProjectEConfig.server.difficulty.covalenceLoss.get());
                    setCarried(ItemStack.EMPTY);
                } else {
                    // Right click: burn one item
                    totalToAdd = (long) (value * ProjectEConfig.server.difficulty.covalenceLoss.get());
                    cursorStack.shrink(1);
                    if (cursorStack.isEmpty()) {
                        setCarried(ItemStack.EMPTY);
                    }
                }
                
                knowledgeProvider.setEmc(knowledgeProvider.getEmc().add(BigInteger.valueOf(totalToAdd)));
                
                if (player instanceof ServerPlayer serverPlayer) {
                    knowledgeProvider.sync(serverPlayer);
                }
            }
        }
    }
    
    private void burnItemFromInventory(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            Slot slot = slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            
            // PROTECTION: Prevent burning the Arcane Tablet that is currently being used
            if (stack.getItem() instanceof ItemArcaneTablet) {
                ItemStack heldTablet = player.getItemInHand(hand);
                if (stack == heldTablet) {
                    // This is the exact same ItemStack instance that opened this GUI - don't burn it
                    return;
                }
            }
            
            if (!stack.isEmpty() && IEMCProxy.INSTANCE.hasValue(stack)) {
                ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)).createStack();
                
                // Try to add knowledge when burning
                tryAddKnowledge(fixed);
                
                long value = IEMCProxy.INSTANCE.getValue(fixed);
                long totalToAdd = (long) (value * stack.getCount() * ProjectEConfig.server.difficulty.covalenceLoss.get());
                
                knowledgeProvider.setEmc(knowledgeProvider.getEmc().add(BigInteger.valueOf(totalToAdd)));
                
                if (player instanceof ServerPlayer serverPlayer) {
                    knowledgeProvider.sync(serverPlayer);
                }
                
                // Remove the item from the slot
                slot.set(ItemStack.EMPTY);
            }
        }
    }


    public void clearCraftingMatrix() {
        boolean syncEMC = false;
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack stack = craftMatrix.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                long value = IEMCProxy.INSTANCE.getValue(stack);
                if (ProjectEConfig.server.difficulty.covalenceLoss.get() >= 1D && value > 0L && knowledgeProvider != null) {
                    ItemInfo itemInfo = ItemInfo.fromStack(stack);
                    ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(itemInfo).createStack();
                    tryAddKnowledge(fixed);
                    knowledgeProvider.setEmc(knowledgeProvider.getEmc().add(BigInteger.valueOf(value * stack.getCount())));
                    syncEMC = true;
                } else {
                    player.getInventory().placeItemBackInInventory(stack, true);
                }
            }
        }
        if (syncEMC && knowledgeProvider != null && player instanceof ServerPlayer serverPlayer) {
            knowledgeProvider.sync(serverPlayer);
        }
        slotChangedCraftingGrid(this, player.level(), player, craftMatrix, craftResult);
    }

    public void rotateCraftingMatrix(boolean clockwise) {
        ItemStack[] stacks = new ItemStack[ROTATION_SLOTS.length];
        
        if (clockwise) {
            for (int i = 0; i < ROTATION_SLOTS.length; i++) {
                int j = i - 1;
                if (j < 0) {
                    j = ROTATION_SLOTS.length - 1;
                }
                stacks[i] = craftMatrix.getItem(ROTATION_SLOTS[j % ROTATION_SLOTS.length]);
            }
        } else {
            for (int i = 0; i < ROTATION_SLOTS.length; i++) {
                stacks[i] = craftMatrix.getItem(ROTATION_SLOTS[(i + 1) % ROTATION_SLOTS.length]);
            }
        }
        
        for (int i = 0; i < ROTATION_SLOTS.length; i++) {
            craftMatrix.setItem(ROTATION_SLOTS[i], stacks[i]);
        }
        
        slotChangedCraftingGrid(this, player.level(), player, craftMatrix, craftResult);
        broadcastChanges();
    }

    public void balanceCraftingMatrix() {
        Map<ItemInfo, List<ItemStack>> itemGroups = new HashMap<>();
        Map<ItemInfo, Integer> totalCounts = new HashMap<>();
        
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack stack = craftMatrix.getItem(i);
            if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                ItemInfo info = ItemInfo.fromStack(stack);
                itemGroups.computeIfAbsent(info, k -> new ArrayList<>()).add(stack);
                totalCounts.put(info, totalCounts.getOrDefault(info, 0) + stack.getCount());
            }
        }
        
        for (Map.Entry<ItemInfo, List<ItemStack>> entry : itemGroups.entrySet()) {
            List<ItemStack> stacks = entry.getValue();
            int totalCount = totalCounts.get(entry.getKey());
            int countPerStack = totalCount / stacks.size();
            int remainder = totalCount % stacks.size();
            
            for (ItemStack stack : stacks) {
                stack.setCount(countPerStack);
            }
            
            for (int i = 0; i < remainder && i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                }
            }
        }
        
        slotChangedCraftingGrid(this, player.level(), player, craftMatrix, craftResult);
        broadcastChanges();
    }

    public void spreadCraftingMatrix() {
        while (true) {
            ItemStack biggestStack = null;
            int biggestSize = 1;
            
            for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                ItemStack stack = craftMatrix.getItem(i);
                if (!stack.isEmpty() && stack.getCount() > biggestSize) {
                    biggestStack = stack;
                    biggestSize = stack.getCount();
                }
            }
            
            if (biggestStack == null) {
                break;
            }
            
            boolean spread = false;
            for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                ItemStack stack = craftMatrix.getItem(i);
                if (stack.isEmpty() && biggestStack.getCount() > 1) {
                    craftMatrix.setItem(i, biggestStack.split(1));
                    spread = true;
                    break;
                }
            }
            
            if (!spread) {
                break;
            }
        }
        
        balanceCraftingMatrix();
    }

    public static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer container, ResultContainer result) {
        if (level.isClientSide) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack resultStack = ItemStack.EMPTY;
        
        CraftingInput craftingInput = container.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipeHolder = level.getServer().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, craftingInput, level);
            
        if (recipeHolder.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = recipeHolder.get();
            if (result.setRecipeUsed(level, serverPlayer, holder)) {
                resultStack = holder.value().assemble(craftingInput, level.registryAccess());
            }
        }
        
        result.setItem(0, resultStack);
        menu.setRemoteSlot(0, resultStack);
        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
            menu.containerId, menu.incrementStateId(), 0, resultStack));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Handle Shift+Left click on inventory items to burn them directly
        if (clickType == ClickType.QUICK_MOVE && button == 0 && slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            ItemStack stack = slot.getItem();
            
            // PROTECTION: Prevent moving/burning the Arcane Tablet that is currently being used
            if (stack.getItem() instanceof ItemArcaneTablet) {
                ItemStack heldTablet = player.getItemInHand(hand);
                if (stack == heldTablet) {
                    // This is the exact same ItemStack instance that opened this GUI - don't allow moving/burning
                    return;
                }
            }
            
            // Check if this is a player inventory slot (not crafting or result slots)
            if (slotId >= playerInventoryStart && !stack.isEmpty() && IEMCProxy.INSTANCE.hasValue(stack)) {
                // Shift+Left click on inventory item with EMC value - burn the entire stack
                burnItemFromInventory(slotId);
                return; // Don't call super, we handled this click
            }
        }
        
        // PROTECTION: Also protect against regular clicks/drags on the Arcane Tablet
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            ItemStack stack = slot.getItem();
            
            if (stack.getItem() instanceof ItemArcaneTablet) {
                ItemStack heldTablet = player.getItemInHand(hand);
                if (stack == heldTablet) {
                    // This is the exact same ItemStack instance that opened this GUI - don't allow any clicks
                    return;
                }
            }
        }
        
        // Default behavior for all other clicks
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        slotChangedCraftingGrid(this, player.level(), player, craftMatrix, craftResult);
        // Save crafting matrix whenever it changes to ensure persistence
        if (container == craftMatrix) {
            saveCraftingMatrix();
        }
    }

    public IKnowledgeProvider getKnowledgeProvider() {
        return knowledgeProvider;
    }
    
    /**
     * Save the current crafting matrix to the Arcane Tablet using DataComponents
     */
    public void saveCraftingMatrix() {
        ItemStack tabletStack = player.getItemInHand(hand);
        if (tabletStack.getItem() instanceof ItemArcaneTablet) {
            ItemArcaneTablet.saveCraftingMatrix(tabletStack, itemHandler, player.level().registryAccess());
        }
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        // Save crafting matrix when container is closed
        saveCraftingMatrix();
    }
    
    /**
     * Transfer items for JEI recipe integration
     * This method handles both transferring from player inventory and from EMC
     */
    public void transferItems(Int2ObjectMap<List<ItemStack>> stacksMap) {
        // First try to transfer from player inventory
        stacksMap.forEach(this::transferFromInventory);
        
        // Then try to transfer remaining items from EMC
        boolean syncEMC = false;
        for (Map.Entry<Integer, List<ItemStack>> entry : stacksMap.int2ObjectEntrySet()) {
            if (transferFromTablet(entry.getKey(), entry.getValue())) {
                syncEMC = true;
            }
        }
        
        if (syncEMC && knowledgeProvider != null && player instanceof ServerPlayer serverPlayer) {
            knowledgeProvider.sync(serverPlayer);
        }
        
        broadcastChanges();
    }
    
    /**
     * Try to transfer items from EMC to crafting grid
     */
    private boolean transferFromTablet(int destSlot, List<ItemStack> candidateStacks) {
        if (candidateStacks.size() > 1) {
            // Sort by EMC value to prefer cheaper alternatives
            candidateStacks = candidateStacks.stream()
                .sorted(Comparator.comparingLong(o -> IEMCProxy.INSTANCE.getValue(o)))
                .toList();
        }
        
        for (ItemStack stack : candidateStacks) {
            ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)).createStack();
            
            if (knowledgeProvider != null && knowledgeProvider.hasKnowledge(fixed)) {
                long value = IEMCProxy.INSTANCE.getValue(fixed);
                BigInteger bigValue = BigInteger.valueOf(value);
                
                if (value > 0L && knowledgeProvider.getEmc().compareTo(bigValue) >= 0) {
                    ItemStack slotItem = craftMatrix.getItem(destSlot);
                    
                    if (slotItem.isEmpty()) {
                        // Slot is empty, place the item
                        craftMatrix.setItem(destSlot, fixed.copy());
                        knowledgeProvider.setEmc(knowledgeProvider.getEmc().subtract(bigValue));
                        return true;
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() 
                               && canItemsStack(slotItem, fixed)) {
                        // Slot has compatible item, increase count
                        slotItem.grow(1);
                        knowledgeProvider.setEmc(knowledgeProvider.getEmc().subtract(bigValue));
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Try to transfer items from player inventory to crafting grid
     */
    private void transferFromInventory(int destSlot, List<ItemStack> candidateStacks) {
        for (ItemStack candidate : candidateStacks) {
            // Search player inventory for matching items
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack inventoryStack = player.getInventory().getItem(i);
                if (!inventoryStack.isEmpty() && canItemsStack(inventoryStack, candidate)) {
                    ItemStack slotItem = craftMatrix.getItem(destSlot);
                    
                    if (slotItem.isEmpty()) {
                        // Move one item to the crafting slot
                        ItemStack toMove = inventoryStack.split(1);
                        craftMatrix.setItem(destSlot, toMove);
                        return;
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() 
                               && canItemsStack(slotItem, inventoryStack)) {
                        // Increase existing stack
                        slotItem.grow(1);
                        inventoryStack.shrink(1);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Check if two ItemStacks can stack together
     */
    private boolean canItemsStack(ItemStack stack1, ItemStack stack2) {
        return ItemStack.isSameItemSameComponents(stack1, stack2);
    }
    
    /**
     * Performs batch crafting when shift-clicking the result slot
     * Crafts as many items as possible up to one stack (64 total items)
     */
    private ItemStack performBatchCrafting(ArcaneTabletResultSlot resultSlot, Player player) {
        ItemStack currentResult = resultSlot.getItem();
        if (currentResult.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        ItemStack firstCraft = currentResult.copy();
        int maxStackSize = currentResult.getMaxStackSize();
        int perCraft = currentResult.getCount();
        
        // Calculate how many times we can craft (limited by stack size)
        int maxCrafts = maxStackSize / perCraft;
        if (maxCrafts <= 0) maxCrafts = 1;
        
        // Store what we've crafted to move to inventory
        ItemStack totalCrafted = ItemStack.EMPTY;
        int successfulCrafts = 0;
        boolean needsEMCSync = false;
        
        for (int i = 0; i < maxCrafts; i++) {
            // Check if we can still craft (result slot has items)
            if (resultSlot.getItem().isEmpty()) {
                break;
            }
            
            // Store the current materials before crafting
            ItemStack[] materialsBefore = new ItemStack[craftMatrix.getContainerSize()];
            for (int j = 0; j < materialsBefore.length; j++) {
                materialsBefore[j] = craftMatrix.getItem(j).copy();
            }
            
            ItemStack craftedItem = resultSlot.getItem().copy();
            
            // Take the item (this consumes materials)
            resultSlot.onTakeNoRefill(player, craftedItem);
            
            // Check if we need to refill materials from EMC
            for (int j = 0; j < materialsBefore.length; j++) {
                if (!materialsBefore[j].isEmpty() && craftMatrix.getItem(j).isEmpty()) {
                    // Try to refill this slot from EMC
                    if (knowledgeProvider != null && knowledgeProvider.hasKnowledge(materialsBefore[j])) {
                        long value = IEMCProxy.INSTANCE.getValue(materialsBefore[j]);
                        BigInteger bigValue = BigInteger.valueOf(value);
                        
                        if (value > 0L && knowledgeProvider.getEmc().compareTo(bigValue) >= 0) {
                            craftMatrix.setItem(j, materialsBefore[j].copy());
                            knowledgeProvider.setEmc(knowledgeProvider.getEmc().subtract(bigValue));
                            needsEMCSync = true;
                        } else {
                            // Can't refill, stop batch crafting
                            break;
                        }
                    } else {
                        // No knowledge for this item, stop batch crafting
                        break;
                    }
                }
            }
            
            // Add to our total
            if (totalCrafted.isEmpty()) {
                totalCrafted = craftedItem.copy();
            } else {
                totalCrafted.grow(craftedItem.getCount());
            }
            
            successfulCrafts++;
            
            // Update the crafting result for next iteration
            slotChangedCraftingGrid(this, player.level(), player, craftMatrix, craftResult);
        }
        
        if (!totalCrafted.isEmpty()) {
            // Try to move all crafted items to player inventory
            if (!moveItemStackTo(totalCrafted, playerInventoryStart, playerInventoryStart + 36, true)) {
                // If we can't fit all items, drop the excess
                player.drop(totalCrafted, false);
            }
            
            // Sync EMC if materials were consumed from EMC
            if (needsEMCSync && knowledgeProvider != null && player instanceof ServerPlayer serverPlayer) {
                knowledgeProvider.sync(serverPlayer);
            }
            
            return firstCraft;
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Performs batch crafting action when triggered by Ctrl+Left click
     */
    private void performBatchCraftingAction() {
        ArcaneTabletResultSlot resultSlot = (ArcaneTabletResultSlot) slots.get(0);
        if (resultSlot.hasItem()) {
            ItemStack result = performBatchCrafting(resultSlot, player);
            // The actual crafting is done in performBatchCrafting method
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot.getSlotIndex() == player.getInventory().selected) {
            return ItemStack.EMPTY;
        }
        
        // Handle result slot shift-clicking - this is now handled by GUI layer for batch crafting
        // quickMoveStack on result slot should not happen anymore as GUI intercepts Shift+click
        if (slot instanceof ArcaneTabletResultSlot resultSlot) {
            // This should normally not be called anymore since GUI handles Shift+click on result slot
            return ItemStack.EMPTY;
        }
        
        // Handle player inventory shift-clicking (burning items with EMC)
        if (index >= playerInventoryStart && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            
            // PROTECTION: Prevent moving/burning the Arcane Tablet that is currently being used
            if (stack.getItem() instanceof ItemArcaneTablet) {
                ItemStack heldTablet = player.getItemInHand(hand);
                if (stack == heldTablet) {
                    // This is the exact same ItemStack instance that opened this GUI - don't allow moving/burning
                    return ItemStack.EMPTY;
                }
            }
            
            if (player instanceof ServerPlayer serverPlayer && IEMCProxy.INSTANCE.hasValue(stack)) {
                ItemStack fixed = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)).createStack();
                
                // Add knowledge and burn the item for EMC
                tryAddKnowledge(fixed);
                
                long totalValue = (long) (IEMCProxy.INSTANCE.getValue(stack) * stack.getCount() 
                    * ProjectEConfig.server.difficulty.covalenceLoss.get());
                knowledgeProvider.setEmc(knowledgeProvider.getEmc().add(BigInteger.valueOf(totalValue)));
                knowledgeProvider.sync(serverPlayer);
                
                ItemStack result = stack.copy();
                slot.set(ItemStack.EMPTY);
                return result;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }

    public class ArcaneTabletResultSlot extends ResultSlot {
        public ArcaneTabletResultSlot(Player player, CraftingContainer matrix, Container result, int slot, int x, int y) {
            super(player, matrix, result, slot, x, y);
        }
        
        @Override
        public void onTake(Player player, ItemStack stack) {
            // When extracting result, try to backfill the crafting matrix with items from EMC
            
            // Remember what items were in the crafting matrix before consumption
            ItemStack[] previousItems = new ItemStack[craftMatrix.getContainerSize()];
            for (int i = 0; i < previousItems.length; i++) {
                previousItems[i] = craftMatrix.getItem(i);
                if (!previousItems[i].isEmpty()) {
                    previousItems[i] = previousItems[i].copyWithCount(1);
                }
            }
            
            // Call parent onTake to consume crafting materials
            super.onTake(player, stack);
            
            // Try to refill empty slots with items from EMC
            for (int i = 0; i < previousItems.length; i++) {
                if (!previousItems[i].isEmpty() && craftMatrix.getItem(i).isEmpty()) {
                    // Try to refill this slot from EMC
                    List<ItemStack> candidates = List.of(previousItems[i]);
                    transferFromTablet(i, candidates);
                }
            }
        }
        
        /**
         * Alternative onTake that doesn't refill materials (used for shift-clicking)
         */
        public void onTakeNoRefill(Player player, ItemStack stack) {
            super.onTake(player, stack);
        }

    }

    public class ArcaneTabletCraftingContainer implements CraftingContainer {
        private final IItemHandlerModifiable items;
        private final int width, height;
        private final ContainerArcaneTablet container;

        public ArcaneTabletCraftingContainer(ContainerArcaneTablet container, int width, int height, IItemHandlerModifiable items) {
            this.container = container;
            this.items = items;
            this.width = width;
            this.height = height;
        }

        @Override
        public int getContainerSize() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < items.getSlots(); i++) {
                if (!items.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            return index < 0 || index >= getContainerSize() ? ItemStack.EMPTY : items.getStackInSlot(index);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.setStackInSlot(slot, stack);
            setChanged();
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            if (index < 0 || index >= getContainerSize()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = items.getStackInSlot(index);
            items.setStackInSlot(index, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            if (index < 0 || index >= getContainerSize() || count <= 0 || items.getStackInSlot(index).isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            ItemStack stack = items.getStackInSlot(index).split(count);
            if (!stack.isEmpty()) {
                setChanged();
            }
            return stack;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        @Override
        public void fillStackedContents(StackedContents helper) {
            for (int i = 0; i < items.getSlots(); i++) {
                helper.accountStack(items.getStackInSlot(i));
            }
        }

        @Override
        public void setChanged() {
            container.slotsChanged(this);
        }

        @Override
        public boolean stillValid(Player player) {
            return container.stillValid(player);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public List<ItemStack> getItems() {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < getContainerSize(); i++) {
                list.add(getItem(i));
            }
            return list;
        }
    }
}
