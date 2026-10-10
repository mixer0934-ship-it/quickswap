package com.example.quickswap;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Rerolls enchanting-table options by enchanting spare copies of the item in the table,
 * one per cycle, until one of the three offered enchantments matches a target.
 *
 * Each cycle: check offers -> click the cheapest option -> shift-click the result out
 * -> shift-click a fresh spare copy in -> repeat.
 */
public final class EnchantCycler {
    private EnchantCycler() {}

    private enum Step { CHECK, ENCHANT, TAKE_OUT, INSERT }

    private static boolean running;
    private static Step step = Step.CHECK;
    private static int wait;
    private static int cycles;
    private static Item template;
    private static boolean keyWasDown;
    private static String status = "";

    public static void registerOverlay() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof EnchantmentScreen) {
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> drawOverlay(client, ctx));
            }
        });
    }

    public static void tick(MinecraftClient client) {
        EnchantCycleConfig cfg = QuickSwapConfig.get().enchant;
        ClientPlayerEntity player = client.player;

        if (!(client.currentScreen instanceof EnchantmentScreen screen)
                || player == null || client.interactionManager == null || !cfg.enabled) {
            if (running) stop("Stopped: table closed");
            keyWasDown = true; // don't trigger the moment the screen opens
            return;
        }

        long window = client.getWindow().getHandle();
        boolean down = cfg.toggleKey >= 0 && GLFW.glfwGetKey(window, cfg.toggleKey) == GLFW.GLFW_PRESS;
        if (down && !keyWasDown) {
            if (running) stop("Stopped");
            else start(screen, cfg);
        }
        keyWasDown = down;

        if (!running) return;
        if (wait > 0) {
            wait--;
            return;
        }

        EnchantmentScreenHandler handler = screen.getScreenHandler();
        int delay = Math.max(1, cfg.delayTicks);

        switch (step) {
            case CHECK -> {
                int found = findMatch(client, handler, cfg);
                if (found >= 0) {
                    finish(client, handler, found);
                    return;
                }
                if (cycles >= cfg.maxCycles) {
                    stop("Stopped: reached max cycles (" + cfg.maxCycles + ")");
                    return;
                }
                if (handler.getSlot(0).getStack().isEmpty()) {
                    step = Step.INSERT;
                    return;
                }
                if (handler.enchantmentPower[0] <= 0) {
                    stop("Stopped: this item has no enchant options");
                    return;
                }
                if (!player.isCreative() && player.experienceLevel < Math.max(1, handler.enchantmentPower[0])) {
                    stop("Stopped: not enough XP levels");
                    return;
                }
                if (handler.getSlot(1).getStack().isEmpty()) {
                    int lapis = findInventorySlot(handler, s -> s.isOf(Items.LAPIS_LAZULI));
                    if (lapis < 0) {
                        stop("Stopped: out of lapis lazuli");
                        return;
                    }
                    client.interactionManager.clickSlot(handler.syncId, lapis, 0, SlotActionType.QUICK_MOVE, player);
                    wait = delay;
                    return;
                }
                step = Step.ENCHANT;
            }
            case ENCHANT -> {
                client.interactionManager.clickButton(handler.syncId, 0);
                cycles++;
                status = "Cycling... " + cycles + " tried";
                step = Step.TAKE_OUT;
                wait = delay;
            }
            case TAKE_OUT -> {
                client.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, player);
                step = Step.INSERT;
                wait = delay;
            }
            case INSERT -> {
                if (!handler.getSlot(0).getStack().isEmpty()) {
                    stop("Stopped: couldn't clear the table slot (inventory full?)");
                    return;
                }
                int spare = findInventorySlot(handler, s -> s.isOf(template) && !s.hasEnchantments());
                if (spare < 0) {
                    stop("Stopped: out of spare items to cycle with");
                    return;
                }
                client.interactionManager.clickSlot(handler.syncId, spare, 0, SlotActionType.QUICK_MOVE, player);
                step = Step.CHECK;
                wait = delay;
            }
        }
    }

    private static void start(EnchantmentScreen screen, EnchantCycleConfig cfg) {
        ItemStack inTable = screen.getScreenHandler().getSlot(0).getStack();
        if (inTable.isEmpty()) {
            status = "Put the item you want enchanted in the table first";
            return;
        }
        if (cfg.targets.isEmpty()) {
            status = "Add at least one target in Mod Menu > QuickSwap > Enchant";
            return;
        }
        template = inTable.getItem();
        running = true;
        cycles = 0;
        step = Step.CHECK;
        wait = 0;
        status = "Cycling...";
    }

    private static void stop(String why) {
        running = false;
        status = why;
    }

    private static void finish(MinecraftClient client, EnchantmentScreenHandler handler, int slot) {
        running = false;
        String name = enchantName(client, handler.enchantmentId[slot]);
        int level = handler.enchantmentLevel[slot];
        status = "FOUND " + name.replace("minecraft:", "") + " " + level + " in option " + (slot + 1)
                + " after " + cycles + " cycles";
        if (client.player != null) {
            client.player.sendMessage(Text.literal("QuickSwap: " + status), false);
        }
    }

    private static int findMatch(MinecraftClient client, EnchantmentScreenHandler handler, EnchantCycleConfig cfg) {
        for (int i = 0; i < 3; i++) {
            int rawId = handler.enchantmentId[i];
            if (rawId < 0) continue;
            String name = enchantName(client, rawId);
            if (name.isEmpty()) continue;
            int level = handler.enchantmentLevel[i];
            for (EnchantCycleConfig.Target t : cfg.targets) {
                if (normalize(t.id).equals(name) && level >= t.minLevel) return i;
            }
        }
        return -1;
    }

    private static String enchantName(MinecraftClient client, int rawId) {
        if (rawId < 0 || client.world == null) return "";
        Registry<Enchantment> reg = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        return reg.getEntry(rawId)
                .flatMap(e -> e.getKey())
                .map(k -> k.getValue().toString())
                .orElse("");
    }

    private static String normalize(String id) {
        String s = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        return s.contains(":") ? s : "minecraft:" + s;
    }

    /** Finds a player-inventory slot (screen slots 2+) whose stack matches. Returns -1 if none. */
    private static int findInventorySlot(EnchantmentScreenHandler handler, java.util.function.Predicate<ItemStack> test) {
        for (int i = 2; i < handler.slots.size(); i++) {
            ItemStack s = handler.getSlot(i).getStack();
            if (!s.isEmpty() && test.test(s)) return i;
        }
        return -1;
    }

    private static void drawOverlay(MinecraftClient client, DrawContext ctx) {
        EnchantCycleConfig cfg = QuickSwapConfig.get().enchant;
        if (!cfg.enabled) return;
        int x = 6, y = 6;
        String key = cfg.toggleKey < 0 ? "unbound" : RuleEditScreen.keyName(cfg.toggleKey);
        ctx.drawTextWithShadow(client.textRenderer, "QuickSwap Enchant Cycler", x, y, 0xFFFFFF55);
        ctx.drawTextWithShadow(client.textRenderer,
                (running ? "Press " + key + " to stop" : "Press " + key + " to start"), x, y + 11, 0xFFAAAAAA);
        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(client.textRenderer, status, x, y + 22,
                    status.startsWith("FOUND") ? 0xFF55FF55 : 0xFFFFFFFF);
        }
        int ty = y + 38;
        for (EnchantCycleConfig.Target t : cfg.targets) {
            ctx.drawTextWithShadow(client.textRenderer,
                    "- " + t.id.replace("minecraft:", "") + " " + t.minLevel + "+", x, ty, 0xFFAAAAAA);
            ty += 10;
        }
    }
}
