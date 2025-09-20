package cool.furry.mc.neoforge.projectexpansion.gui;

import cool.furry.mc.neoforge.projectexpansion.gui.buttons.ExtractItemButton;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.neoforge.projectexpansion.util.PinYinUtils;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Base functionality for the table GUIs with knowledge/EMC integration
 * Adapted from Extended Exchange for Project Expansion
 */
public abstract class AbstractTableScreen<C extends ContainerBase> extends AbstractEXScreen<C> {

    // static so they persist across GUI invocations
    private static int staticPage = 0;
    private static String staticSearch = "";
    
    private final List<ItemStack> validItems = new ArrayList<>();
    protected final List<ExtractItemButton> extractButtons = new ArrayList<>();
    private EditBox searchField;
    protected IKnowledgeProvider knowledgeProvider;

    public AbstractTableScreen(C menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 217; // Extended height for table GUIs
        this.knowledgeProvider = getKnowledgeProvider();
    }

    private IKnowledgeProvider getKnowledgeProvider() {
        return Util.getKnowledgeProvider(Minecraft.getInstance().player);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        Rect2i tb = searchFieldPos();
        searchField = new EditBox(font, tb.getX(), tb.getY(), tb.getWidth(), tb.getHeight(), Component.empty());
        searchField.setTextColor(0xFFFFFFFF);
        searchField.setTextColorUneditable(0xFF808080);
        searchField.setBordered(false);
        searchField.setMaxLength(35);
        searchField.setValue(staticSearch);
        setInitialFocus(searchField);
        addRenderableWidget(searchField);
    }

    protected abstract Rect2i searchFieldPos();

    protected void addExtractButton(ExtractItemButton extractItemButton) {
        extractButtons.add(extractItemButton);
        addRenderableWidget(extractItemButton);
    }

    @Override
    public void containerTick() {
        if (!staticSearch.equals(searchField.getValue())) {
            staticSearch = searchField.getValue();
            staticPage = 0;
            updateValidItemList();
            // TODO: JEI sync when implemented
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        changePage(deltaY < 0);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Right-click to clear search
        Rect2i searchPos = searchFieldPos();
        if (mouseX >= searchPos.getX() && mouseX <= searchPos.getX() + searchPos.getWidth() &&
            mouseY >= searchPos.getY() && mouseY <= searchPos.getY() + searchPos.getHeight() && button == 1) {
            searchField.setValue("");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
        }
        if (this.searchField.keyPressed(keyCode, scanCode, modifiers) || this.searchField.canConsumeInput()) {
            if (keyCode == GLFW.GLFW_KEY_TAB) changeFocus(getCurrentFocusPath());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void changePage(boolean forward) {
        if (forward) {
            if (staticPage < Math.ceil(validItems.size() / (float) extractButtons.size()) - 1) {
                staticPage++;
                updateDisplayedItems();
            }
        } else if (staticPage > 0) {
            staticPage--;
            updateDisplayedItems();
        }
    }

    protected void updateValidItemList() {
        if (knowledgeProvider == null) return;
        
        validItems.clear();
        String srchStr = ChatFormatting.stripFormatting(staticSearch).trim();
        boolean mod = srchStr.startsWith("@");
        if (mod) {
            srchStr = srchStr.substring(1);
        }
        String lowerSrchStr = srchStr.toLowerCase();

        for (ItemInfo itemInfo : knowledgeProvider.getKnowledge()) {
            ItemStack stack = itemInfo.createStack();
            
            if (stack.isEmpty()) continue;
            
            Item item = itemInfo.getItem().value();
            ResourceLocation registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (registryName == null) continue;
            
            String itemName = stack.getDisplayName().getString();
            
            // Namespace matching (@mod:item search pattern)
            boolean namespaceMatch = mod && registryName.getNamespace().toLowerCase().startsWith(lowerSrchStr);
            
            // Use PinYinUtils high-performance matching method (inspired by JustEnoughCharacters)
            boolean matches = false;
            
            if (lowerSrchStr.isEmpty()) {
                matches = true; // Empty search shows all items
            } else if (mod) {
                matches = namespaceMatch; // Mod namespace matching
            } else {
                // Use pinyin4j library for pinyin matching
                matches = PinYinUtils.matchesSearch(itemName, srchStr);
            }
            
            if (matches) {
                validItems.add(IEMCProxy.INSTANCE.getPersistentInfo(itemInfo).createStack());
            }
        }
        
        // Sort by EMC value from high to low
        validItems.sort(Comparator.comparingLong((ItemStack o) -> IEMCProxy.INSTANCE.getValue(o)).reversed());
        
        // Rearrange items to match clockwise layout starting from 12 o'clock
        // Original button index -> clockwise order mapping
        reorderForClockwiseDisplay(validItems);
        updateDisplayedItems();
    }
    
    /**
     * Rearrange item list to match clockwise layout
     * 12 o'clock position has highest EMC, decreasing clockwise
     * Directly modifies the passed list
     */
    private void reorderForClockwiseDisplay(List<ItemStack> sortedItems) {
        if (sortedItems.isEmpty()) {
            return;
        }
        
        // Button clockwise arrangement order (recalculated based on GUI coordinates)
        // Starting from 12 o'clock, clockwise arrangement:
        // 12(0) -> 1(1) -> 2(3) -> 3(5) -> 4(7) -> 5(9) -> 6(11) -> 7(10) -> 8(8) -> 9(6) -> 10(4) -> 11(2)
        final int[] clockwiseOrder = {0, 1, 3, 5, 7, 9, 11, 10, 8, 6, 4, 2};
        final int buttonsPerPage = extractButtons.size();
        
        List<ItemStack> reorderedItems = new ArrayList<>(sortedItems.size());
        
        // Rearrange items for each page
        for (int page = 0; page < (sortedItems.size() + buttonsPerPage - 1) / buttonsPerPage; page++) {
            int pageStart = page * buttonsPerPage;
            int pageEnd = Math.min(pageStart + buttonsPerPage, sortedItems.size());
            
            // Get items for this page
            List<ItemStack> pageItems = sortedItems.subList(pageStart, pageEnd);
            ItemStack[] reorderedPageItems = new ItemStack[buttonsPerPage];
            
            // Arrange items on this page in clockwise order
            for (int i = 0; i < pageItems.size(); i++) {
                int clockwiseIndex = (i < clockwiseOrder.length) ? clockwiseOrder[i] : i;
                reorderedPageItems[clockwiseIndex] = pageItems.get(i);
            }
            
            // Add to final list (skip null items)
            for (int i = 0; i < buttonsPerPage; i++) {
                if (reorderedPageItems[i] != null) {
                    reorderedItems.add(reorderedPageItems[i]);
                }
            }
        }
        
        // Clear original list and add rearranged items
        sortedItems.clear();
        sortedItems.addAll(reorderedItems);
    }

    protected void updateDisplayedItems() {
        for (int i = 0; i < extractButtons.size(); i++) {
            int index = i + staticPage * extractButtons.size();
            if (index >= 0 && index < validItems.size()) {
                extractButtons.get(i).setItem(validItems.get(index));
                extractButtons.get(i).visible = true;
            } else {
                extractButtons.get(i).setItem(ItemStack.EMPTY);
                extractButtons.get(i).visible = false;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (knowledgeProvider != null) {
            // Use conditional formatting - show full number when shift is pressed
            String s = cool.furry.mc.neoforge.projectexpansion.util.EMCFormat.formatConditional(knowledgeProvider.getEmc(), true);
            graphics.drawString(font, s, ((imageWidth - font.width(s)) / 2f), -9f, 0xFFB5B5B5, false);
        }
    }
}
