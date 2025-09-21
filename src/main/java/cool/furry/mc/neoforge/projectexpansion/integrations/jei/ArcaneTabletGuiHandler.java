package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.gui.buttons.ExtractItemButton;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * GUI Container handler for JEI integration with Arcane Tablet
 * Enables R/U key bindings for items in both inventory slots and extract buttons
 */
public class ArcaneTabletGuiHandler implements IGuiContainerHandler<GUIArcaneTablet> {
    
    @Override
    public List<Rect2i> getGuiExtraAreas(GUIArcaneTablet containerScreen) {
        // Return any extra areas that should be excluded from JEI overlay
        // This can include areas where our custom buttons are located
        return List.of();
    }
    
    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(
            GUIArcaneTablet containerScreen,
            double mouseX,
            double mouseY) {
        
        // First check if mouse is over a normal slot (this should work automatically, but we'll be thorough)
        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack stackInSlot = hoveredSlot.getItem();
            if (!stackInSlot.isEmpty()) {
                // Create simple clickable ingredient for the slot
                Rect2i slotArea = new Rect2i(
                    containerScreen.getGuiLeft() + hoveredSlot.x,
                    containerScreen.getGuiTop() + hoveredSlot.y,
                    16, 16
                );
                return Optional.of(new SimpleClickableIngredient(stackInSlot, slotArea));
            }
        }
        
        // Check if mouse is over an extract button
        for (ExtractItemButton extractButton : containerScreen.getExtractButtons()) {
            if (extractButton.isMouseOver(mouseX, mouseY)) {
                ItemStack buttonItem = extractButton.getCurrentItem();
                if (!buttonItem.isEmpty()) {
                    // Create simple clickable ingredient for the extract button
                    Rect2i buttonArea = new Rect2i(
                        extractButton.getX(),
                        extractButton.getY(),
                        extractButton.getWidth(),
                        extractButton.getHeight()
                    );
                    return Optional.of(new SimpleClickableIngredient(buttonItem, buttonArea));
                }
            }
        }
        
        return Optional.empty();
    }
}
