package com.example.quickswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.Map;

/** Polls rule hotkeys every client tick and performs the hotbar swaps. */
public final class SwapEngine {
    private SwapEngine() {}

    private static final class State {
        boolean wasDown;
        int returnSlot = -1;
        int swappedTo = -1;
        int ticksLeft;
    }

    private static final Map<SwapRule, State> STATES = new IdentityHashMap<>();

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        QuickSwapConfig cfg = QuickSwapConfig.get();
        if (player == null || !cfg.enabled) {
            STATES.clear();
            return;
        }

        STATES.keySet().removeIf(r -> !cfg.rules.contains(r));

        long window = client.getWindow().getHandle();
        boolean screenOpen = client.currentScreen != null;

        for (SwapRule rule : cfg.rules) {
            State st = STATES.computeIfAbsent(rule, r -> new State());

            if (!rule.enabled || rule.keyCode < 0) {
                st.wasDown = false;
                st.returnSlot = -1;
                continue;
            }

            boolean down = GLFW.glfwGetKey(window, rule.keyCode) == GLFW.GLFW_PRESS;
            boolean pressed = down && !st.wasDown;
            boolean released = !down && st.wasDown;
            st.wasDown = down;

            if (pressed && !screenOpen) {
                onPress(client, player, rule, st, cfg);
            }
            if (released && rule.swapBack == SwapRule.SwapBack.ON_RELEASE) {
                swapBack(client, player, st, cfg);
            }
            if (st.returnSlot >= 0 && rule.swapBack == SwapRule.SwapBack.AFTER_DELAY) {
                if (--st.ticksLeft <= 0) {
                    swapBack(client, player, st, cfg);
                }
            }
        }
    }

    private static void onPress(MinecraftClient client, ClientPlayerEntity player, SwapRule rule, State st, QuickSwapConfig cfg) {
        if (rule.swapBack == SwapRule.SwapBack.TOGGLE && st.returnSlot >= 0) {
            swapBack(client, player, st, cfg);
            return;
        }

        PlayerInventory inv = player.getInventory();
        int current = inv.getSelectedSlot();
        if (!ItemMatcher.matches(player.getMainHandStack(), rule.holding)) return;

        int slot = findTarget(inv, rule.target, current);
        if (slot < 0) return;

        inv.setSelectedSlot(slot);

        if (rule.swapBack != SwapRule.SwapBack.NONE) {
            st.returnSlot = current;
            st.swappedTo = slot;
            st.ticksLeft = Math.max(1, rule.delayTicks);
        }
        message(client, cfg, Text.literal("QuickSwap: ").append(inv.getStack(slot).isEmpty()
                ? Text.literal("empty hand") : inv.getStack(slot).getName()));
    }

    private static void swapBack(MinecraftClient client, ClientPlayerEntity player, State st, QuickSwapConfig cfg) {
        if (st.returnSlot >= 0) {
            PlayerInventory inv = player.getInventory();
            // Only swap back if the player hasn't manually moved off the swapped slot.
            if (inv.getSelectedSlot() == st.swappedTo) {
                inv.setSelectedSlot(st.returnSlot);
                message(client, cfg, Text.literal("QuickSwap: swapped back"));
            }
        }
        st.returnSlot = -1;
        st.swappedTo = -1;
    }

    /** Searches the hotbar starting after the current slot so repeated presses cycle through matches. */
    private static int findTarget(PlayerInventory inv, String spec, int current) {
        int fixed = ItemMatcher.parseSlot(spec);
        if (fixed >= 0) return fixed == current ? -1 : fixed;
        if (ItemMatcher.isAny(spec)) return -1;

        for (int i = 1; i < 9; i++) {
            int slot = (current + i) % 9;
            ItemStack s = inv.getStack(slot);
            if (ItemMatcher.matches(s, spec)) return slot;
        }
        return -1;
    }

    private static void message(MinecraftClient client, QuickSwapConfig cfg, Text text) {
        if (cfg.showMessages) {
            client.inGameHud.setOverlayMessage(text, false);
        }
    }
}
