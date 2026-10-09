package com.example.quickswap;

/** One "if holding X and key pressed, swap to Y" rule. Serialized to JSON by Gson. */
public class SwapRule {

    public enum SwapBack {
        NONE("Never"),
        ON_RELEASE("When key is released"),
        AFTER_DELAY("After a delay"),
        TOGGLE("On next key press");

        public final String label;

        SwapBack(String label) {
            this.label = label;
        }

        public SwapBack next() {
            SwapBack[] v = values();
            return v[(ordinal() + 1) % v.length];
        }
    }

    public String name = "New rule";
    public boolean enabled = true;

    /** What you must be holding for the rule to fire: any, empty, item id, #tag, word, slot:N */
    public String holding = "any";

    /** What to swap to: item id, #tag, word (matches part of item id), or slot:1-9 */
    public String target = "";

    /** GLFW key code, -1 = unbound */
    public int keyCode = -1;

    public SwapBack swapBack = SwapBack.NONE;

    /** Only used when swapBack == AFTER_DELAY. 20 ticks = 1 second. */
    public int delayTicks = 20;
}
