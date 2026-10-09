package com.example.quickswap;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Matches an item against a spec string:
 *   "" / "any" / "*"     anything (including empty hand)
 *   "empty"              empty hand/slot
 *   "#minecraft:swords"  item tag
 *   "minecraft:shield"   exact item id
 *   "pickaxe"            any item whose id path contains this word
 */
public final class ItemMatcher {
    private ItemMatcher() {}

    public static boolean isAny(String spec) {
        String s = norm(spec);
        return s.isEmpty() || s.equals("any") || s.equals("*");
    }

    public static boolean matches(ItemStack stack, String spec) {
        String s = norm(spec);
        if (isAny(s)) return true;
        if (s.equals("empty")) return stack.isEmpty();
        if (stack.isEmpty()) return false;

        if (s.startsWith("#")) {
            Identifier id = Identifier.tryParse(s.substring(1));
            return id != null && stack.isIn(TagKey.of(RegistryKeys.ITEM, id));
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (s.contains(":")) return itemId.toString().equals(s);
        return itemId.getPath().contains(s);
    }

    /** Returns 0-8 for "slot:1".."slot:9", otherwise -1. */
    public static int parseSlot(String spec) {
        String s = norm(spec);
        if (!s.startsWith("slot:")) return -1;
        try {
            int n = Integer.parseInt(s.substring(5).trim());
            return (n >= 1 && n <= 9) ? n - 1 : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
