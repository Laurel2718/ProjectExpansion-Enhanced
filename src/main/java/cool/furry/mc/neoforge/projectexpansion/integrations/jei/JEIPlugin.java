package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTablet;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

/**
 * JEI Plugin for Project Expansion
 * Handles recipe transfer integration for Arcane Tablet
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
    
    private static IJeiRuntime jeiRuntime;
    
    /**
     * Get the JEI runtime instance for accessing key mappings and other JEI features
     */
    public static IJeiRuntime getJeiRuntime() {
        return jeiRuntime;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return Main.rl("jei_plugin");
    }
    
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JEIPlugin.jeiRuntime = jeiRuntime;
    }
    
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Register GUI container handler to enable R/U key bindings for items in extract buttons
        registration.addGuiContainerHandler(GUIArcaneTablet.class, new ArcaneTabletGuiHandler());
        
        // Register click areas for JEI integration (optional - adds recipe click area)
        registration.addRecipeClickArea(GUIArcaneTablet.class, 
            88, 32, 24, 16, // Result slot area (x, y, width, height)
            mezz.jei.api.constants.RecipeTypes.CRAFTING);
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





