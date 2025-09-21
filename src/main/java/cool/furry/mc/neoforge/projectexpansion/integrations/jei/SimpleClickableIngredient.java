package cool.furry.mc.neoforge.projectexpansion.integrations.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/**
 * Simple implementation of IClickableIngredient for ItemStack
 * Used to enable JEI R/U key bindings for extract buttons
 */
public class SimpleClickableIngredient implements IClickableIngredient<ItemStack> {
    private final ItemStack itemStack;
    private final Rect2i area;
    
    public SimpleClickableIngredient(ItemStack itemStack, Rect2i area) {
        this.itemStack = itemStack;
        this.area = area;
    }
    
    @Override
    public ITypedIngredient<ItemStack> getTypedIngredient() {
        return new SimpleTypedIngredient(itemStack);
    }
    
    @Override
    public Rect2i getArea() {
        return area;
    }
    
    /**
     * Simple implementation of ITypedIngredient for ItemStack
     */
    private static class SimpleTypedIngredient implements ITypedIngredient<ItemStack> {
        private final ItemStack itemStack;
        
        public SimpleTypedIngredient(ItemStack itemStack) {
            this.itemStack = itemStack;
        }
        
        @Override
        public IIngredientType<ItemStack> getType() {
            return VanillaTypes.ITEM_STACK;
        }
        
        @Override
        public ItemStack getIngredient() {
            return itemStack;
        }
    }
}

