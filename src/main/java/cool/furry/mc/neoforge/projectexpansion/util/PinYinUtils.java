package cool.furry.mc.neoforge.projectexpansion.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import me.towdium.pinin.DictLoader;
import me.towdium.pinin.PinIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class PinYinUtils {

    private static final Logger LOGGER = LogManager.getLogger("PinYinUtils");
    private static final PinIn context = new PinIn(new CustomLoader()).config().accelerate(true).commit();
    
    // DoS protection: limit search term length and complexity
    private static final int MAX_SEARCH_TERM_LENGTH = 64;
    private static final int MAX_ITEM_NAME_LENGTH = 256;
    
    // High-performance cache mechanism to avoid repeated calculations
    private static final Map<String, Boolean> searchCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 2000; // Increased cache size for better hit rate
    private static final int CACHE_CLEANUP_THRESHOLD = 1500; // Clean when 75% full
    private static volatile long cacheAccessCount = 0;

    /**
     * Check if pinyin functionality is available
     */
    public static boolean isPinyinAvailable() {
        return true; // PinIn library is always available
    }

    /**
     * Check if search string matches item name - Uses PinIn library for pinyin matching (with DoS protection)
     */
    public static boolean matchesSearch(String itemName, String searchTerm) {
        if (itemName == null || searchTerm == null || searchTerm.isEmpty()) {
            return true;
        }
        
        // DoS protection: limit input length
        if (searchTerm.length() > MAX_SEARCH_TERM_LENGTH || 
            itemName.length() > MAX_ITEM_NAME_LENGTH) {
            return false;
        }
        
        // Check cache for performance optimization
        String cacheKey = itemName + ":" + searchTerm;
        Boolean cached = searchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String lowerItemName = itemName.toLowerCase();
        String lowerSearchTerm = searchTerm.toLowerCase();

        // 1. Direct string matching
        if (lowerItemName.contains(lowerSearchTerm)) {
            cacheResult(cacheKey, true);
            return true;
        }

        // 2. Pinyin matching using PinIn library
        try {
            boolean matches = context.contains(itemName, searchTerm);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("contains({}, {}) -> {}", itemName, searchTerm, matches);
            }
            cacheResult(cacheKey, matches);
            return matches;
        } catch (Exception e) {
            LOGGER.warn("Failed to perform pinyin search for '{}' with '{}' - {}", itemName, searchTerm, e.getMessage());
            cacheResult(cacheKey, false);
            return false;
        }
    }
    
    /**
     * Cache search results with intelligent cleanup - Performance optimized
     * Uses access count to determine when to clean cache
     */
    private static void cacheResult(String key, boolean result) {
        // Increment access counter for cleanup decisions
        cacheAccessCount++;
        
        // Proactive cache management - clean before reaching max size
        if (searchCache.size() >= CACHE_CLEANUP_THRESHOLD) {
            cleanupCache();
        }
        
        searchCache.put(key, result);
    }
    
    /**
     * Optimized cache cleanup - removes approximately 25% of entries
     * Uses modulo operation for even distribution
     */
    private static void cleanupCache() {
        Iterator<String> it = searchCache.keySet().iterator();
        int cleanupInterval = 4; // Remove every 4th entry (25%)
        int counter = 0;
        
        while (it.hasNext()) {
            it.next();
            if (counter++ % cleanupInterval == 0) {
                it.remove();
            }
        }
    }

    /**
     * Get pinyin representation of item name (kept for compatibility)
     */
    public static String getItemPinyin(ItemStack stack) {
        String displayName = stack.getDisplayName().getString();
        return toPinyin(displayName);
    }

    /**
     * Convert Chinese to pinyin (kept for compatibility, but actually uses PinIn library)
     */
    public static String toPinyin(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return " | ";
        }
        // Simplified version, directly return original string
        // PinIn library handles pinyin conversion internally
        return chinese + "|" + chinese;
    }

    /**
     * Custom dictionary loader with special character pinyin mappings
     * Based on JustEnoughCharacters implementation
     */
    private static class CustomLoader extends DictLoader.Default {
        @Override
        public void load(BiConsumer<Character, String[]> feed) {
            super.load(feed);
            // Add common special character mappings used in Minecraft
            feed.accept('\u9FCF', new String[]{"mai4"});   // Meitnerium
            feed.accept('\u9FD4', new String[]{"ge1"});    // Roentgenium
            feed.accept('\u9FED', new String[]{"ni3"});    // Nihonium
            feed.accept('\u9FEC', new String[]{"tian2"});  // Flerovium
            feed.accept('\u9FEB', new String[]{"ao4"});    // Oganesson
            feed.accept('\uE900', new String[]{"lu2"});    // Lutetium
            feed.accept('\uE901', new String[]{"du4"});    // Dubnium
            feed.accept('\uE902', new String[]{"xi3"});    // Hassium
            feed.accept('\uE903', new String[]{"bo1"});    // Bohrium
            feed.accept('\uE904', new String[]{"hei1"});   // Seaborgium
            feed.accept('\uE906', new String[]{"da2"});    // Darmstadtium
            feed.accept('\uE907', new String[]{"lun2"});   // Copernicium
            feed.accept('\uE910', new String[]{"fu1"});    // Flerovium alt
            feed.accept('\uE912', new String[]{"li4"});    // Livermorium
        }
    }
}