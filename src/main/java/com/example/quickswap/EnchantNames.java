package com.example.quickswap;

import java.util.Locale;

/** Static list of vanilla enchantment names, used for ghost-text completion in the config screen. */
public final class EnchantNames {
    private EnchantNames() {}

    private static final String[] NAMES = {
            "mending", "unbreaking", "efficiency", "fortune", "silk_touch", "sharpness", "smite",
            "bane_of_arthropods", "looting", "sweeping_edge", "fire_aspect", "knockback",
            "protection", "fire_protection", "blast_protection", "projectile_protection",
            "feather_falling", "thorns", "respiration", "aqua_affinity", "depth_strider",
            "frost_walker", "soul_speed", "swift_sneak", "power", "punch", "flame", "infinity",
            "luck_of_the_sea", "lure", "loyalty", "riptide", "channeling", "impaling",
            "quick_charge", "piercing", "multishot", "density", "breach", "wind_burst",
            "lunge", "binding_curse", "vanishing_curse"
    };

    /** Returns the first full id ("minecraft:mending") that starts with what was typed, or null. */
    public static String complete(String typed) {
        String t = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return null;
        for (String n : NAMES) {
            String full = "minecraft:" + n;
            if (t.contains(":") ? full.startsWith(t) : n.startsWith(t)) return full;
        }
        return null;
    }
}
