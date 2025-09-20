package cool.furry.mc.neoforge.projectexpansion.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Base GUI screen class for Project Expansion, adapted from Extended Exchange
 */
public abstract class AbstractEXScreen<C extends AbstractContainerMenu> extends AbstractContainerScreen<C> {
    
    public AbstractEXScreen(C menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public static void bindTexture(ResourceLocation guiTexture) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, guiTexture);
    }

    final void bindGuiTexture() {
        ResourceLocation guiTexture = getGuiTexture();
        if (guiTexture != null) {
            bindTexture(guiTexture);
        }
    }

    protected abstract ResourceLocation getGuiTexture();

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render tooltips from custom buttons
        List<Component> tooltip = new ArrayList<>();
        renderables.stream()
                .filter(w -> w instanceof ITooltipProvider p && p.shouldProvide())
                .forEach(w -> ((ITooltipProvider) w).addTooltip(mouseX, mouseY, tooltip, Screen.hasShiftDown()));
        if (!tooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        bindGuiTexture();

        int xStart = (width - imageWidth) / 2;
        int yStart = (height - imageHeight) / 2;
        guiGraphics.blit(getGuiTexture(), xStart, yStart, 0, 0, imageWidth, imageHeight);
    }
}




