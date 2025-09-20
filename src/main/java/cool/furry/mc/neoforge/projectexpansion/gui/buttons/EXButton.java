package cool.furry.mc.neoforge.projectexpansion.gui.buttons;

import cool.furry.mc.neoforge.projectexpansion.gui.ITooltipProvider;
import cool.furry.mc.neoforge.projectexpansion.net.PacketHandler;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketArcaneTabletAction;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

/**
 * Base button class for Project Expansion GUI buttons
 * Adapted from Extended Exchange
 */
public abstract class EXButton extends Button implements ITooltipProvider {
    protected ResourceLocation texture;
    protected int textureX;
    protected int textureY;
    private String tag = "";
    private List<Component> tooltip = Collections.emptyList();

    public EXButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
    }

    @Override
    public void onPress() {
        super.onPress();

        if (tag != null && !tag.isEmpty()) {
            PacketHandler.sendToServer(new PacketArcaneTabletAction(tag, Screen.hasShiftDown()));
        }
    }

    public EXButton withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public EXButton withTexture(ResourceLocation texture, int tx, int ty) {
        this.texture = texture;
        this.textureX = tx;
        this.textureY = ty;
        return this;
    }

    public EXButton withTooltip(Component tooltip) {
        this.tooltip = Collections.singletonList(tooltip);
        return this;
    }

    @Override
    public void playDownSound(SoundManager handler) {
        // silence
    }

    @Override
    public void addTooltip(double mouseX, double mouseY, List<Component> curTip, boolean shift) {
        if (isHovered) {
            curTip.addAll(tooltip);
        }
    }
}




