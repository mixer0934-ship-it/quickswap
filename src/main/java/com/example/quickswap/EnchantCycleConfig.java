package com.example.quickswap;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Settings for the enchanting-table cycler. Serialized inside quickswap.json. */
public class EnchantCycleConfig {

    public static class Target {
        public String id = "minecraft:mending";
        public int minLevel = 1;

        public Target() {}

        public Target(String id, int minLevel) {
            this.id = id;
            this.minLevel = minLevel;
        }
    }

    public boolean enabled = true;

    /** Key pressed while an enchanting table is open to start/stop cycling. */
    public int toggleKey = GLFW.GLFW_KEY_G;

    /** Ticks to wait between each step (20 ticks = 1 second). Raise this if a server kicks you for spam. */
    public int delayTicks = 4;

    public int maxCycles = 300;

    public List<Target> targets = new ArrayList<>();

    public static EnchantCycleConfig defaults() {
        EnchantCycleConfig c = new EnchantCycleConfig();
        c.targets.add(new Target("minecraft:mending", 1));
        return c;
    }
}
