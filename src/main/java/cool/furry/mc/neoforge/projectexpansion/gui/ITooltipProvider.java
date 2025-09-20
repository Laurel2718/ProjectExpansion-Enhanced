package cool.furry.mc.neoforge.projectexpansion.gui;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Interface for GUI components that can provide tooltips
 */
public interface ITooltipProvider {
    void addTooltip(double mouseX, double mouseY, List<Component> curTip, boolean shift);
    
    default boolean shouldProvide() {
        return true;
    }
}




