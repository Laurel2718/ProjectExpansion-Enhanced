package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

/**
 * JEI Plugin for Project Expansion
 * Handles recipe transfer integration for Arcane Tablet
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
    
    @Override
    public ResourceLocation getPluginUid() {
        return Main.rl("jei_plugin");
    }
    
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Register Arcane Tablet recipe transfer handler
        registration.addRecipeTransferHandler(
            new ArcaneTabletRecipeTransferHandler(registration.getTransferHelper()),
            mezz.jei.api.constants.RecipeTypes.CRAFTING
        );
    }
}





