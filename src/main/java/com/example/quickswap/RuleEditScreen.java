package com.example.quickswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Edits a single SwapRule. All widgets write straight into the rule object. */
public class RuleEditScreen extends Screen {
    private static final String[] HOLD_PRESETS = {
            "any", "empty", "#minecraft:pickaxes", "#minecraft:axes", "#minecraft:shovels",
            "#minecraft:hoes", "#minecraft:swords", "minecraft:shield", "minecraft:bow", "minecraft:fishing_rod"
    };
    private static final String[] TARGET_PRESETS = {
            "#minecraft:swords", "#minecraft:pickaxes", "#minecraft:axes", "#minecraft:shovels",
            "#minecraft:hoes", "minecraft:shield", "minecraft:bow", "minecraft:ender_pearl",
            "minecraft:golden_apple", "minecraft:totem_of_undying", "empty",
            "slot:1", "slot:2", "slot:3", "slot:4", "slot:5", "slot:6", "slot:7", "slot:8", "slot:9"
    };

    private static final int Y_NAME = 36, Y_HOLD = 70, Y_TARGET = 104, Y_KEY = 138, Y_BACK = 166, Y_DELAY = 200;

    private final Screen parent;
    private final SwapRule rule;
    private boolean listening = false;
    private ButtonWidget keyButton;

    public RuleEditScreen(Screen parent, SwapRule rule) {
        super(Text.literal("Edit rule"));
        this.parent = parent;
        this.rule = rule;
    }

    public static String keyName(int code) {
        return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
    }

    @Override
    protected void init() {
        int w = 240;
        int left = width / 2 - w / 2;

        TextFieldWidget name = new TextFieldWidget(textRenderer, left, Y_NAME, w, 20, Text.literal("Name"));
        name.setMaxLength(40);
        name.setText(rule.name);
        name.setChangedListener(s -> rule.name = s);
        addDrawableChild(name);

        TextFieldWidget hold = new TextFieldWidget(textRenderer, left, Y_HOLD, w - 64, 20, Text.literal("Holding"));
        hold.setMaxLength(100);
        hold.setText(rule.holding);
        hold.setChangedListener(s -> rule.holding = s);
        addDrawableChild(hold);
        addDrawableChild(ButtonWidget.builder(Text.literal("Presets"), b -> {
            rule.holding = cycle(HOLD_PRESETS, rule.holding);
            hold.setText(rule.holding);
        }).dimensions(left + w - 60, Y_HOLD, 60, 20).build());

        TextFieldWidget target = new TextFieldWidget(textRenderer, left, Y_TARGET, w - 64, 20, Text.literal("Target"));
        target.setMaxLength(100);
        target.setText(rule.target);
        target.setChangedListener(s -> rule.target = s);
        addDrawableChild(target);
        addDrawableChild(ButtonWidget.builder(Text.literal("Presets"), b -> {
            rule.target = cycle(TARGET_PRESETS, rule.target);
            target.setText(rule.target);
        }).dimensions(left + w - 60, Y_TARGET, 60, 20).build());

        keyButton = addDrawableChild(ButtonWidget.builder(keyLabel(), b -> {
            listening = true;
            keyButton.setMessage(keyLabel());
        }).dimensions(left, Y_KEY, w - 64, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            rule.keyCode = -1;
            listening = false;
            keyButton.setMessage(keyLabel());
        }).dimensions(left + w - 60, Y_KEY, 60, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Swap back: " + rule.swapBack.label), b -> {
            rule.swapBack = rule.swapBack.next();
            clearAndInit();
        }).dimensions(left, Y_BACK, w, 20).build());

        if (rule.swapBack == SwapRule.SwapBack.AFTER_DELAY) {
            TextFieldWidget delay = new TextFieldWidget(textRenderer, left, Y_DELAY, 80, 20, Text.literal("Delay"));
            delay.setTextPredicate(s -> s.matches("\\d{0,4}"));
            delay.setText(String.valueOf(rule.delayTicks));
            delay.setChangedListener(s -> {
                try {
                    rule.delayTicks = Math.max(1, Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                    // empty field while typing
                }
            });
            addDrawableChild(delay);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 100, height - 28, 200, 20).build());
    }

    private Text keyLabel() {
        if (listening) return Text.literal("> press a key (Esc cancels) <");
        if (rule.keyCode < 0) return Text.literal("Hotkey: not bound");
        return Text.literal("Hotkey: " + keyName(rule.keyCode));
    }

    private static String cycle(String[] list, String current) {
        for (int i = 0; i < list.length; i++) {
            if (list[i].equalsIgnoreCase(current == null ? "" : current.trim())) {
                return list[(i + 1) % list.length];
            }
        }
        return list[0];
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening) {
            listening = false;
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                rule.keyCode = input.key();
            }
            keyButton.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int left = width / 2 - 120;
        int gray = 0xFFAAAAAA;

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Rule name", left, Y_NAME - 10, gray);
        context.drawTextWithShadow(textRenderer, "When I'm holding...", left, Y_HOLD - 10, gray);
        context.drawTextWithShadow(textRenderer, "Swap to...", left, Y_TARGET - 10, gray);
        context.drawTextWithShadow(textRenderer, "Hotkey", left, Y_KEY - 10, gray);
        if (rule.swapBack == SwapRule.SwapBack.AFTER_DELAY) {
            context.drawTextWithShadow(textRenderer, "Delay in ticks (20 = 1 second)", left, Y_DELAY - 10, gray);
        }
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Accepts: any, empty, minecraft:id, #tag, a word like \"pickaxe\", slot:1-9"),
                width / 2, height - 46, gray);
    }

    @Override
    public void removed() {
        QuickSwapConfig.save();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
