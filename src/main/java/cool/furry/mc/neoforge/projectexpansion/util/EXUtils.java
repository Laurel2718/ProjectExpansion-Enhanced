package cool.furry.mc.neoforge.projectexpansion.util;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Utility methods for knowledge management
 * Adapted from Extended Exchange for Project Expansion
 */
public class EXUtils {
    
    public static KnowledgeAddResult addKnowledge(Player player, IKnowledgeProvider knowledgeProvider, ItemStack stack) {
        if (stack.isEmpty() || !IEMCProxy.INSTANCE.hasValue(stack)) {
            return KnowledgeAddResult.NOT_ADDED;
        }

        // Use the cleaned/persistent info for knowledge checks and addition
        ItemInfo info = ItemInfo.fromStack(stack);
        ItemInfo cleaned = IEMCProxy.INSTANCE.getPersistentInfo(info);
        ItemStack cleanedStack = cleaned.createStack();
        
        if (!knowledgeProvider.hasKnowledge(cleanedStack)) {
            // In 1.21.1, we'll skip the event check for now and directly add knowledge
            boolean added = knowledgeProvider.addKnowledge(cleanedStack);
            return added ? KnowledgeAddResult.ADDED : KnowledgeAddResult.NOT_ADDED;
        }

        return KnowledgeAddResult.ALREADY_KNOWN;
    }

    public enum KnowledgeAddResult {
        NOT_ADDED,
        ALREADY_KNOWN,
        ADDED
    }
}
