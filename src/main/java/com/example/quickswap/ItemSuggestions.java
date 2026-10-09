package com.example.quickswap;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Builds the autocomplete list for the "holding" and "swap to" fields. */
public final class ItemSuggestions {
    private ItemSuggestions() {}

    public static final int MAX = 8;

    private static final String[] KEYWORDS = {
            "any", "empty",
            "slot:1", "slot:2", "slot:3", "slot:4", "slot:5", "slot:6", "slot:7", "slot:8", "slot:9"
    };

    private static final String[] TAGS = {
            "#minecraft:pickaxes", "#minecraft:axes", "#minecraft:shovels", "#minecraft:hoes",
            "#minecraft:swords", "#minecraft:head_armor", "#minecraft:chest_armor",
            "#minecraft:leg_armor", "#minecraft:foot_armor", "#minecraft:logs",
            "#minecraft:planks", "#minecraft:wool", "#minecraft:arrows"
    };

    private static List<String> itemIds;

    private static List<String> ids() {
        if (itemIds == null) {
            List<String> list = new ArrayList<>();
            for (Identifier id : Registries.ITEM.getIds()) {
                String s = id.toString();
                if (!s.equals("minecraft:air")) list.add(s);
            }
            Collections.sort(list);
            itemIds = list;
        }
        return itemIds;
    }

    public static List<String> suggest(String input, boolean allowAny) {
        String q = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        LinkedHashSet<String> out = new LinkedHashSet<>();

        if (q.isEmpty()) {
            if (allowAny) out.add("any");
            out.add("empty");
            out.add("#minecraft:pickaxes");
            out.add("#minecraft:axes");
            out.add("#minecraft:swords");
            out.add("minecraft:shield");
            out.add("minecraft:bow");
            out.add("slot:1");
            return trim(out);
        }

        if (q.startsWith("#")) {
            for (String t : TAGS) if (t.startsWith(q)) out.add(t);
            String bare = q.substring(1);
            for (String t : TAGS) if (t.contains(bare)) out.add(t);
            return trim(out);
        }

        for (String k : KEYWORDS) {
            if (!allowAny && k.equals("any")) continue;
            if (k.startsWith(q)) out.add(k);
        }
        for (String t : TAGS) {
            if (t.contains(q)) out.add(t);
        }
        for (String id : ids()) {
            if (out.size() >= MAX) break;
            String path = id.substring(id.indexOf(':') + 1);
            if (path.startsWith(q) || id.startsWith(q)) out.add(id);
        }
        for (String id : ids()) {
            if (out.size() >= MAX) break;
            if (id.contains(q)) out.add(id);
        }
        return trim(out);
    }

    private static List<String> trim(LinkedHashSet<String> set) {
        List<String> list = new ArrayList<>(set);
        return list.size() > MAX ? new ArrayList<>(list.subList(0, MAX)) : list;
    }
}
