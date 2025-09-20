package cool.furry.mc.neoforge.projectexpansion.gui.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Button for extracting items from EMC knowledge
 * Adapted from Extended Exchange
 */
public class ExtractItemButton extends EXButton {
    private final IKnowledgeProvider provider;
    private ItemStack item = ItemStack.EMPTY;
    private final int x;
    private final int y;

    public ExtractItemButton(int x, int y, IKnowledgeProvider provider) {
        super(x, y, 18, 18, b -> {});
        this.provider = provider;
        this.x = x;
        this.y = y;
    }

    @Override
    public void onPress() {
        if (!item.isEmpty()) {
            // Use the item's registry name for extraction
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
            super.withTag("extract:" + itemId);
            super.onPress();
        }
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        if (!item.isEmpty()) {
            // Draw the item icon
            Font font = Minecraft.getInstance().font;
            guiGraphics.renderItem(item, x, y);
            
            // Draw extraction count label
            String label = getExtractionCountStr();
            if (!label.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + 17, y + 12, 200d);
                guiGraphics.pose().scale(0.5F, 0.5F, 0.5F);
                guiGraphics.drawString(font, label, -font.width(label), 0, 0xFFFFFFFF, true);
                guiGraphics.pose().popPose();
            }
        }

        // Draw highlight overlay when hovered
        if (isHovered) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            guiGraphics.fill(x, y, x + width, y + height, 0x80FFFFFF);
            RenderSystem.disableBlend();
        }
    }

    private static final BigDecimal ONE_TENTH = BigDecimal.valueOf(1L, 1);
    
    private String getExtractionCountStr() {
        if (provider == null) return "";
        
        long emc = IEMCProxy.INSTANCE.getValue(item);
        if (emc == 0L) return "???"; // shouldn't happen, but...

        String label = "";
        BigDecimal d = new BigDecimal(provider.getEmc()).setScale(1, RoundingMode.DOWN)
                .divide(BigDecimal.valueOf(emc), RoundingMode.DOWN);
        if (d.compareTo(BigDecimal.ONE) >= 0) {
            // Always use short format for item counts
            label = EMCFormat.formatShort(d.setScale(0, RoundingMode.DOWN).toBigInteger());
        } else {
            // When less than 1, show 0 instead of decimal
            label = "0";
        }
        return label;
    }

    @Override
    public void addTooltip(double mouseX, double mouseY, List<Component> curTip, boolean shift) {
        if (isHovered && !item.isEmpty()) {
            curTip.addAll(item.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().level),
                Minecraft.getInstance().player, 
                Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
            ));
        }
    }
}
