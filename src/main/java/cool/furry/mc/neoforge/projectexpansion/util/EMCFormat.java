package cool.furry.mc.neoforge.projectexpansion.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Utility class for formatting EMC values
 * Adapted from Extended Exchange
 */
public class EMCFormat {
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat SHORT_FORMAT = new DecimalFormat("#.#");
    public static final NumberFormat INSTANCE = FORMAT;
    
    public static String format(BigInteger emc) {
        return formatShort(emc);
    }
    
    /**
     * Format EMC value with short notation (K, M, B, T, Qa, Qi, Sx, Sp, O)
     */
    public static String formatShort(BigInteger emc) {
        // Octillion (O) - 10^27
        if (emc.compareTo(new BigInteger("1000000000000000000000000000")) >= 0) {
            double val = emc.doubleValue() / 1.0e27;
            return SHORT_FORMAT.format(val) + "O";
        }
        // Septillion (Sp) - 10^24
        else if (emc.compareTo(new BigInteger("1000000000000000000000000")) >= 0) {
            double val = emc.doubleValue() / 1.0e24;
            return SHORT_FORMAT.format(val) + "Sp";
        }
        // Sextillion (Sx) - 10^21
        else if (emc.compareTo(new BigInteger("1000000000000000000000")) >= 0) {
            double val = emc.doubleValue() / 1.0e21;
            return SHORT_FORMAT.format(val) + "Sx";
        }
        // Quintillion (Qi) - 10^18
        else if (emc.compareTo(new BigInteger("1000000000000000000")) >= 0) {
            double val = emc.doubleValue() / 1.0e18;
            return SHORT_FORMAT.format(val) + "Qi";
        }
        // Quadrillion (Qa) - 10^15
        else if (emc.compareTo(new BigInteger("1000000000000000")) >= 0) {
            double val = emc.doubleValue() / 1.0e15;
            return SHORT_FORMAT.format(val) + "Qa";
        }
        // Trillions (T) - 10^12
        else if (emc.compareTo(BigInteger.valueOf(1_000_000_000_000L)) >= 0) {
            double val = emc.doubleValue() / 1_000_000_000_000.0;
            return SHORT_FORMAT.format(val) + "T";
        }
        // Billions (B) - 10^9
        else if (emc.compareTo(BigInteger.valueOf(1_000_000_000L)) >= 0) {
            double val = emc.doubleValue() / 1_000_000_000.0;
            return SHORT_FORMAT.format(val) + "B";
        }
        // Millions (M) - 10^6
        else if (emc.compareTo(BigInteger.valueOf(1_000_000L)) >= 0) {
            double val = emc.doubleValue() / 1_000_000.0;
            return SHORT_FORMAT.format(val) + "M";
        }
        // Thousands (K) - 10^3
        else if (emc.compareTo(BigInteger.valueOf(1_000L)) >= 0) {
            double val = emc.doubleValue() / 1_000.0;
            return SHORT_FORMAT.format(val) + "K";
        } else {
            // Less than 1000, show full number
            return FORMAT.format(emc);
        }
    }
    
    /**
     * Format with original full number display
     */
    public static String formatFull(BigInteger emc) {
        return FORMAT.format(emc);
    }
    
    /**
     * Format EMC with conditional display based on ctrl key
     * @param emc The EMC value to format
     * @param showFullWhenCtrl If true, shows full number when ctrl is pressed, otherwise shows short format
     * @return Formatted string
     */
    public static String formatConditional(BigInteger emc, boolean showFullWhenCtrl) {
        boolean isCtrlPressed = net.minecraft.client.gui.screens.Screen.hasControlDown();
        
        if (showFullWhenCtrl && isCtrlPressed) {
            return formatFull(emc);
        } else {
            return formatShort(emc);
        }
    }
    
    public static String formatBigDecimal(BigDecimal value) {
        return formatShort(value.toBigInteger());
    }
    
    public static String formatForceLong(BigInteger value) {
        return formatShort(value);
    }
    
    // Component generation methods for chat formatting
    public static MutableComponent getComponent(BigInteger value) {
        return Component.literal(formatShort(value));
    }
    
    public static MutableComponent getComponent(BigDecimal value) {
        return Component.literal(formatShort(value.toBigInteger()));
    }
    
    public static MutableComponent getComponent(long value) {
        return Component.literal(formatShort(BigInteger.valueOf(value)));
    }
    
    public static MutableComponent getComponent(int value) {
        return Component.literal(formatShort(BigInteger.valueOf(value)));
    }
    
    // Conditional component generation methods
    public static MutableComponent getComponentConditional(BigInteger value, boolean showFullWhenCtrl) {
        return Component.literal(formatConditional(value, showFullWhenCtrl));
    }
    
    public static MutableComponent getComponentConditional(long value, boolean showFullWhenCtrl) {
        return Component.literal(formatConditional(BigInteger.valueOf(value), showFullWhenCtrl));
    }
    
    // Wrapper for advanced formatting
    public static EMCWrapper wrap(BigInteger value) {
        return new EMCWrapper(value);
    }
    
    public static EMCWrapper wrap(BigDecimal value) {
        return new EMCWrapper(value);
    }
    
    public static class EMCWrapper {
        private final Object value;
        private boolean forceShort = false;
        private boolean ignoreShift = false;
        private boolean force = false;
        
        EMCWrapper(Object value) {
            this.value = value;
        }
        
        public EMCWrapper forceShort() {
            this.forceShort = true;
            return this;
        }
        
        public EMCWrapper ignoreShift() {
            this.ignoreShift = true;
            return this;
        }
        
        public EMCWrapper force() {
            this.force = true;
            return this;
        }
        
        public String format() {
            if (value instanceof BigInteger) {
                return forceShort ? formatShort((BigInteger) value) : formatFull((BigInteger) value);
            } else if (value instanceof BigDecimal) {
                return forceShort ? formatShort(((BigDecimal) value).toBigInteger()) : FORMAT.format(value);
            }
            return value.toString();
        }
    }
}