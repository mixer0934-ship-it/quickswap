package com.example.quickswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickSwapClient implements ClientModInitializer {
    public static final String MOD_ID = "quickswap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openConfigKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        QuickSwapConfig.load();
        EnchantCycler.registerOverlay();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickswap.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickswap.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                client.setScreen(new RuleListScreen(null));
            }
            while (toggleKey.wasPressed()) {
                QuickSwapConfig cfg = QuickSwapConfig.get();
                cfg.enabled = !cfg.enabled;
                QuickSwapConfig.save();
                if (client.player != null) {
                    client.inGameHud.setOverlayMessage(
                            Text.literal("QuickSwap: " + (cfg.enabled ? "ON" : "OFF")), false);
                }
            }
            SwapEngine.tick(client);
            EnchantCycler.tick(client);
        });
    }
}
