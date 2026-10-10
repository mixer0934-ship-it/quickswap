package com.example.quickswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QuickSwapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QuickSwapConfig instance = new QuickSwapConfig();

    public boolean enabled = true;
    public boolean showMessages = true;
    public boolean bookFill = true;
    public List<SwapRule> rules = new ArrayList<>();
    public EnchantCycleConfig enchant = EnchantCycleConfig.defaults();

    public static QuickSwapConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("quickswap.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                QuickSwapConfig loaded = GSON.fromJson(r, QuickSwapConfig.class);
                if (loaded != null) {
                    if (loaded.rules == null) loaded.rules = new ArrayList<>();
                    if (loaded.enchant == null) loaded.enchant = EnchantCycleConfig.defaults();
                    if (loaded.enchant.targets == null) loaded.enchant.targets = new ArrayList<>();
                    instance = loaded;
                }
            } catch (Exception e) {
                // Don't overwrite a file we failed to read.
                QuickSwapClient.LOGGER.error("Failed to read quickswap.json, using defaults for this session", e);
            }
            return;
        }
        instance = new QuickSwapConfig();
        instance.rules.add(exampleRule());
        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            QuickSwapClient.LOGGER.error("Failed to save quickswap.json", e);
        }
    }

    private static SwapRule exampleRule() {
        SwapRule r = new SwapRule();
        r.name = "Pickaxe -> Sword (hold R)";
        r.holding = "#minecraft:pickaxes";
        r.target = "#minecraft:swords";
        r.keyCode = GLFW.GLFW_KEY_R;
        r.swapBack = SwapRule.SwapBack.ON_RELEASE;
        return r;
    }
}
