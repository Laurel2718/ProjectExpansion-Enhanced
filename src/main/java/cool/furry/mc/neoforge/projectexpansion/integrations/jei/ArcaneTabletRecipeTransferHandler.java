package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.net.PacketHandler;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTabletRecipeTransfer;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JEI Recipe Transfer Handler for Arcane Tablet
 * Adapted from Extended Exchange for Project Expansion
 */
public class ArcaneTabletRecipeTransferHandler implements IRecipeTransferHandler<ContainerArcaneTablet, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper transferHelper;
    
    public ArcaneTabletRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }
    
    @Override
    public Class<ContainerArcaneTablet> getContainerClass() {
        return ContainerArcaneTablet.class;
    }
    
    @Override
    public Optional<MenuType<ContainerArcaneTablet>> getMenuType() {
        return Optional.of(MenuTypes.ARCANE_TABLET.get());
    }
    
    @SuppressWarnings("unchecked")
    public Class<RecipeHolder<CraftingRecipe>> getRecipeClass() {
        return (Class<RecipeHolder<CraftingRecipe>>) (Class<?>) RecipeHolder.class;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return new mezz.jei.api.recipe.RecipeType<>(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "crafting"),
            (Class<RecipeHolder<CraftingRecipe>>) (Class<?>) RecipeHolder.class
        );
    }
    
    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            @Nonnull ContainerArcaneTablet container, 
            @Nonnull RecipeHolder<CraftingRecipe> recipeHolder, 
            @Nonnull IRecipeSlotsView recipeSlots, 
            @Nonnull Player player, 
            boolean maxTransfer, 
            boolean doTransfer) {
        
        if (doTransfer) {
            // Get input ingredients from JEI recipe view
            List<IRecipeSlotView> inputViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            
            // Get crafting slots from the container
            List<Slot> craftingSlots = container.slots.stream()
                .filter(slot -> slot.container instanceof cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet.ArcaneTabletCraftingContainer)
                .toList();
            
            // Ensure we have the right number of slots
            if (inputViews.size() > craftingSlots.size()) {
                return transferHelper.createInternalError();
            }
            
            // Build ingredient map for each crafting slot
            Int2ObjectMap<List<ItemStack>> ingredientMap = new Int2ObjectOpenHashMap<>();
            for (int i = 0; i < inputViews.size(); i++) {
                List<ItemStack> ingredients = inputViews.get(i)
                    .getIngredients(VanillaTypes.ITEM_STACK)
                    .collect(Collectors.toList());
                ingredientMap.put(i, ingredients);
            }
            
            // Send packet to server to handle the transfer
            PacketHandler.sendToServer(new PacketArcaneTabletRecipeTransfer(ingredientMap, maxTransfer));
        }
        else {
            // Check for missing knowledge/items when not transferring (for highlighting)
            IKnowledgeProvider knowledgeProvider = Util.getKnowledgeProvider(player);
            if (knowledgeProvider == null) {
                // If we can't get the knowledge provider, assume no highlighting is needed
                return null;
            }
            
            List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            List<Integer> missingSlots = new ArrayList<>(); // Store indices of slots missing items
            
            // Iterate through all input slots
            for (int slotIndex = 0; slotIndex < inputSlots.size(); slotIndex++) {
                IRecipeSlotView slot = inputSlots.get(slotIndex);
                boolean hasAnyIngredient = false;
                
                // Get items in the slot
                Optional<ITypedIngredient<?>> optionalIngredient = slot.getDisplayedIngredient();
                if (optionalIngredient.isPresent()) {
                    ITypedIngredient<?> ingredient = optionalIngredient.get();
                    
                    // Only handle item-type materials
                    if (ingredient.getType() == VanillaTypes.ITEM_STACK) {
                        List<ItemStack> items = ingredient.getIngredient(VanillaTypes.ITEM_STACK).stream().toList();
                        
                        // Check if player has knowledge or items for any slot item
                        for (ItemStack requiredStack : items) {
                            // Check if player has knowledge or sufficient items
                            if (knowledgeProvider.hasKnowledge(requiredStack) || 
                                player.getInventory().countItem(requiredStack.getItem()) >= requiredStack.getCount()) {
                                hasAnyIngredient = true;
                                break;
                            }
                        }
                    }
                }
                
                // If this slot has no available materials, mark as missing
                if (!hasAnyIngredient && optionalIngredient.isPresent()) {
                    missingSlots.add(slotIndex);
                }
            }
            
            // If there are missing slots, return error renderer to highlight them
            if (!missingSlots.isEmpty()) {
                return new ErrorRender(missingSlots);
            }
        }
        
        return null;
    }
    
    /**
     * Error renderer for highlighting missing knowledge/items in crafting slots
     * Error renderer for highlighting missing knowledge/items in crafting slots
     */
    private record ErrorRender(List<Integer> missingSlots) implements IRecipeTransferError {
        
        @Override
        public Type getType() {
            return Type.COSMETIC;
        }
        
        @Override
        public void showError(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, @Nonnull IRecipeSlotsView slots, int recipeX, int recipeY) {
            var poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(recipeX, recipeY, 0);
            
            // Draw slot highlight
            var slotViews = slots.getSlotViews(RecipeIngredientRole.INPUT);
            for (int i = 0; i < slotViews.size(); i++) {
                if (missingSlots.contains(i)) {
                    var slotView = slotViews.get(i);
                    // Use semi-transparent red to highlight slots missing knowledge/items
                    slotView.drawHighlight(guiGraphics, 0x66FF0000);
                }
            }
            
            poseStack.popPose();
        }
    }
}
