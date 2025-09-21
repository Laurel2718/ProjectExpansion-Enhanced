package cool.furry.mc.neoforge.projectexpansion.gui;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.buttons.*;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.integrations.jei.JEIPlugin;
import cool.furry.mc.neoforge.projectexpansion.net.PacketHandler;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTabletAction;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Arcane Tablet GUI - Advanced transmutation table with crafting functionality
 * Adapted from Extended Exchange for Project Expansion
 */
public class GUIArcaneTablet extends AbstractTableScreen<ContainerArcaneTablet> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/arcane_tablet.png");
    private EXButton burnButton;

    public GUIArcaneTablet(ContainerArcaneTablet menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // imageHeight is set to 217 in AbstractTableScreen
    }

    @Override
    protected void init() {
        super.init();

        // Page navigation arrows (18x18 buttons)
        addRenderableWidget(new ArrowButton(leftPos + 7, topPos + 20, b -> changePage(false))
                .withTexture(TEXTURE, 196, 0));
        addRenderableWidget(new ArrowButton(leftPos + 151, topPos + 20, b -> changePage(true))
                .withTexture(TEXTURE, 215, 0));

        // Burn slot highlight button (center of extraction circle)
        burnButton = new HighlightButton(leftPos + 80, topPos + 68).withTag("burn");
        addRenderableWidget(burnButton);

        // Extract buttons arranged in circle around burn slot (12 buttons total)
        extractButtons.clear();
        addExtractButton(new ExtractItemButton(leftPos + 80, topPos + 20, knowledgeProvider));   // top
        addExtractButton(new ExtractItemButton(leftPos + 105, topPos + 26, knowledgeProvider));  // top-right
        addExtractButton(new ExtractItemButton(leftPos + 55, topPos + 26, knowledgeProvider));   // top-left
        addExtractButton(new ExtractItemButton(leftPos + 123, topPos + 44, knowledgeProvider));  // right
        addExtractButton(new ExtractItemButton(leftPos + 37, topPos + 44, knowledgeProvider));   // left
        addExtractButton(new ExtractItemButton(leftPos + 128, topPos + 68, knowledgeProvider));  // far-right
        addExtractButton(new ExtractItemButton(leftPos + 32, topPos + 68, knowledgeProvider));   // far-left
        addExtractButton(new ExtractItemButton(leftPos + 123, topPos + 92, knowledgeProvider));  // bottom-right
        addExtractButton(new ExtractItemButton(leftPos + 37, topPos + 92, knowledgeProvider));   // bottom-left
        addExtractButton(new ExtractItemButton(leftPos + 105, topPos + 110, knowledgeProvider)); // bottom-right-2
        addExtractButton(new ExtractItemButton(leftPos + 55, topPos + 110, knowledgeProvider));  // bottom-left-2
        addExtractButton(new ExtractItemButton(leftPos + 80, topPos + 116, knowledgeProvider));  // bottom

        // Learn and unlearn buttons (bottom of main GUI)
        addRenderableWidget(new HighlightButton(leftPos + 9, topPos + 116)
                .withTag("learn"));
        addRenderableWidget(new HighlightButton(leftPos + 153, topPos + 116)
                .withTag("unlearn"));

        // Left side buttons for crafting matrix operations (small 9x9 buttons)
        addRenderableWidget(new HighlightButton(leftPos - 71, topPos + 16, 9, 9)
                .withTag("rotate"));
        addRenderableWidget(new HighlightButton(leftPos - 71, topPos + 26, 9, 9)
                .withTag("balance"));
        addRenderableWidget(new HighlightButton(leftPos - 71, topPos + 61, 9, 9)
                .withTag("clear"));

        // Initialize the item list
        updateValidItemList();
    }

    @Override
    protected Rect2i searchFieldPos() {
        return new Rect2i(leftPos + 8, topPos + 7, 160, 11);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return TEXTURE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle right-click on burn button to burn single item
        if (button == 1 && burnButton != null && 
            mouseX >= burnButton.getX() && mouseX < burnButton.getX() + burnButton.getWidth() && 
            mouseY >= burnButton.getY() && mouseY < burnButton.getY() + burnButton.getHeight()) {
            
            // Send packet to burn one item (right-click)
            PacketHandler.sendToServer(new PacketArcaneTabletAction("burn_one", false));
            return true;
        }
        
        // Handle Shift+Left click on result slot for batch crafting
        if (button == 0 && net.minecraft.client.gui.screens.Screen.hasShiftDown() && menu != null) {
            // Check if clicking on the result slot (slot 0)
            net.minecraft.world.inventory.Slot resultSlot = menu.getSlot(0);
            if (resultSlot.hasItem()) {
                // Calculate if the click is within the result slot area
                int slotX = leftPos + resultSlot.x;
                int slotY = topPos + resultSlot.y;
                
                if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                    // Send packet to perform batch crafting
                    PacketHandler.sendToServer(new PacketArcaneTabletAction("batch_craft", false));
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        // Render the left side additional area for crafting grid and buttons
        // This draws the 76x89 pixel area that contains the crafting grid
        guiGraphics.blit(TEXTURE, leftPos - 75, topPos + 10, 180, 19, 76, 89);
    }
    
    /**
     * Client-side check if an item can be batch crafted
     * This is a simplified version of the server-side logic
     */
    private boolean canBatchCraftOnClient(ItemStack stack) {
        if (stack.isEmpty() || menu == null) {
            return false;
        }
        
        // Simple heuristic: if the stack size is less than max stack size, 
        // it's likely a craftable item that can be batched
        // For items like potions that typically don't stack, don't batch craft
        return stack.getMaxStackSize() > 1 && stack.getCount() < stack.getMaxStackSize();
    }
    
    /**
     * Refresh the item list after knowledge changes
     */
    public void refreshItemList() {
        // Update knowledge provider reference
        this.knowledgeProvider = getKnowledgeProvider();
        // Refresh the valid items and update displayed buttons
        updateValidItemList();
    }
    
    private moze_intel.projecte.api.capabilities.IKnowledgeProvider getKnowledgeProvider() {
        return cool.furry.mc.neoforge.projectexpansion.util.Util.getKnowledgeProvider(net.minecraft.client.Minecraft.getInstance().player);
    }
    
    /**
     * Get the list of extract buttons for JEI integration
     * This allows JEI to detect items in the extract buttons for R/U key bindings
     */
    public java.util.List<ExtractItemButton> getExtractButtons() {
        return extractButtons;
    }
    
    /**
     * Handle key presses for JEI integration (R/U keys)
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // First check if JEI runtime is available
        IJeiRuntime jeiRuntime = JEIPlugin.getJeiRuntime();
        if (jeiRuntime == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        // Get mouse position relative to screen
        double mouseX = this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
        double mouseY = this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
        
        // Convert keyCode to InputConstants.Key
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        
        // Check if a key matches JEI's show recipe (R) or show uses (U) key
        if (jeiRuntime.getKeyMappings().getShowRecipe().isActiveAndMatches(key)) {
            ItemStack itemUnderMouse = getItemUnderMouse(mouseX, mouseY);
            if (!itemUnderMouse.isEmpty()) {
                // Create focus for showing recipes (looking for recipes that produce this item)
                var focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, itemUnderMouse);
                jeiRuntime.getRecipesGui().show(focus);
                return true;
            }
        } else if (jeiRuntime.getKeyMappings().getShowUses().isActiveAndMatches(key)) {
            ItemStack itemUnderMouse = getItemUnderMouse(mouseX, mouseY);
            if (!itemUnderMouse.isEmpty()) {
                // Create focus for showing uses (looking for recipes that use this item)
                var focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, itemUnderMouse);
                jeiRuntime.getRecipesGui().show(focus);
                return true;
            }
        }
        
        // If we didn't handle the key, pass it to the parent
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Get the item under the mouse cursor
     * This checks both regular slots and extract buttons
     */
    private ItemStack getItemUnderMouse(double mouseX, double mouseY) {
        // Check regular slots first
        Slot hoveredSlot = getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            return hoveredSlot.getItem();
        }
        
        // Check extract buttons
        for (ExtractItemButton extractButton : extractButtons) {
            if (extractButton.isMouseOver(mouseX, mouseY)) {
                ItemStack buttonItem = extractButton.getCurrentItem();
                if (!buttonItem.isEmpty()) {
                    return buttonItem;
                }
            }
        }
        
        return ItemStack.EMPTY;
    }
}