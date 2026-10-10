package com.example.quickswap;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

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
    private static final int ROW_H = 12;

    /** Autocomplete state for one text field. */
    private static final class Sug {
        final TextFieldWidget field;
        final boolean allowAny;
        List<String> list = List.of();
        int selected = 0;
        boolean hidden = false;

        Sug(TextFieldWidget field, boolean allowAny) {
            this.field = field;
            this.allowAny = allowAny;
        }

        void refresh() {
            list = ItemSuggestions.suggest(field.getText(), allowAny);
            selected = 0;
            hidden = false;
        }

        boolean visible() {
            if (hidden || list.isEmpty() || !field.isFocused()) return false;
            return !(list.size() == 1 && list.get(0).equalsIgnoreCase(field.getText().trim()));
        }
    }

    private final Screen parent;
    private final SwapRule rule;
    private boolean listening = false;
    private ButtonWidget keyButton;
    private Sug holdSug;
    private Sug targetSug;

    public RuleEditScreen(Screen parent, SwapRule rule) {
        super(Text.literal("Edit rule"));
        this.parent = parent;
        this.rule = rule;
    }

    public static String keyName(int code) {
        return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
    }

    public static String mouseName(int button) {
        return switch (button) {
            case 0 -> "Left Mouse";
            case 1 -> "Right Mouse";
            case 2 -> "Middle Mouse";
            default -> "Mouse Button " + (button + 1);
        };
    }

    /** Human-readable name of whatever the rule is bound to. */
    public static String bindName(SwapRule r) {
        return r.mouse ? mouseName(r.keyCode) : keyName(r.keyCode);
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
        holdSug = new Sug(hold, true);
        holdSug.refresh();
        hold.setChangedListener(s -> {
            rule.holding = s;
            holdSug.refresh();
        });
        addDrawableChild(hold);
        addDrawableChild(ButtonWidget.builder(Text.literal("Presets"), b -> {
            rule.holding = cycle(HOLD_PRESETS, rule.holding);
            hold.setText(rule.holding);
            holdSug.hidden = true;
        }).dimensions(left + w - 60, Y_HOLD, 60, 20).build());

        TextFieldWidget target = new TextFieldWidget(textRenderer, left, Y_TARGET, w - 64, 20, Text.literal("Target"));
        target.setMaxLength(100);
        target.setText(rule.target);
        targetSug = new Sug(target, false);
        targetSug.refresh();
        target.setChangedListener(s -> {
            rule.target = s;
            targetSug.refresh();
        });
        addDrawableChild(target);
        addDrawableChild(ButtonWidget.builder(Text.literal("Presets"), b -> {
            rule.target = cycle(TARGET_PRESETS, rule.target);
            target.setText(rule.target);
            targetSug.hidden = true;
        }).dimensions(left + w - 60, Y_TARGET, 60, 20).build());

        keyButton = addDrawableChild(ButtonWidget.builder(keyLabel(), b -> {
            listening = true;
            keyButton.setMessage(keyLabel());
        }).dimensions(left, Y_KEY, w - 64, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            rule.keyCode = -1;
            rule.mouse = false;
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
        if (listening) return Text.literal("> press key / mouse (Esc) <");
        if (rule.keyCode < 0) return Text.literal("Hotkey: not bound");
        return Text.literal("Hotkey: " + bindName(rule));
    }

    private static String cycle(String[] list, String current) {
        for (int i = 0; i < list.length; i++) {
            if (list[i].equalsIgnoreCase(current == null ? "" : current.trim())) {
                return list[(i + 1) % list.length];
            }
        }
        return list[0];
    }

    // ---------- suggestions ----------

    private Sug activeSug() {
        if (holdSug != null && holdSug.field.isFocused()) return holdSug;
        if (targetSug != null && targetSug.field.isFocused()) return targetSug;
        return null;
    }

    private void accept(Sug s) {
        String choice = s.list.get(s.selected);
        s.field.setText(choice);
        s.hidden = true;
    }

    private int popupWidth(Sug s) {
        int w = s.field.getWidth();
        for (String e : s.list) {
            w = Math.max(w, textRenderer.getWidth(e) + 8);
        }
        return w;
    }

    private int rowAt(Sug s, double mx, double my) {
        int x = s.field.getX();
        int y = s.field.getY() + s.field.getHeight() + 1;
        int w = popupWidth(s);
        int rows = Math.min(s.list.size(), ItemSuggestions.MAX);
        if (mx < x || mx > x + w || my < y || my >= y + rows * ROW_H) return -1;
        return (int) ((my - y) / ROW_H);
    }

    private void drawPopup(DrawContext ctx, Sug s) {
        int rows = Math.min(s.list.size(), ItemSuggestions.MAX);
        int x = s.field.getX();
        int y = s.field.getY() + s.field.getHeight() + 1;
        int w = popupWidth(s);

        ctx.fill(x - 1, y - 1, x + w + 1, y + rows * ROW_H + 1, 0xE6000000);
        for (int i = 0; i < rows; i++) {
            int color = (i == s.selected) ? 0xFFFFFF55 : 0xFFAAAAAA;
            ctx.drawTextWithShadow(textRenderer, s.list.get(i), x + 3, y + i * ROW_H + 2, color);
        }
    }

    private void updateGhost(Sug s, Sug active) {
        String ghost = null;
        if (s == active && s.visible()) {
            String typed = s.field.getText();
            String cand = s.list.get(s.selected);
            if (!typed.isEmpty() && cand.length() > typed.length()
                    && cand.toLowerCase().startsWith(typed.toLowerCase())) {
                ghost = cand.substring(typed.length());
            }
        }
        s.field.setSuggestion(ghost);
    }

    // ---------- input ----------

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening) {
            listening = false;
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                rule.keyCode = input.key();
                rule.mouse = false;
            }
            keyButton.setMessage(keyLabel());
            return true;
        }

        Sug active = activeSug();
        if (active != null && active.visible()) {
            int n = active.list.size();
            int k = input.key();
            if (k == GLFW.GLFW_KEY_DOWN) {
                active.selected = (active.selected + 1) % n;
                return true;
            }
            if (k == GLFW.GLFW_KEY_UP) {
                active.selected = (active.selected - 1 + n) % n;
                return true;
            }
            if (k == GLFW.GLFW_KEY_TAB || k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                accept(active);
                return true;
            }
            if (k == GLFW.GLFW_KEY_ESCAPE) {
                active.hidden = true;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    /** On macOS, turning Caps Lock OFF only sends a release event, so accept that when binding. */
    @Override
    public boolean keyReleased(KeyInput input) {
        if (listening && input.key() == GLFW.GLFW_KEY_CAPS_LOCK) {
            listening = false;
            rule.keyCode = GLFW.GLFW_KEY_CAPS_LOCK;
            rule.mouse = false;
            keyButton.setMessage(keyLabel());
            return true;
        }
        return super.keyReleased(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (listening) {
            int b = click.button();
            if (b >= 0 && b <= 7) {
                rule.keyCode = b;
                rule.mouse = true;
            }
            listening = false;
            keyButton.setMessage(keyLabel());
            return true;
        }
        Sug active = activeSug();
        if (active != null && active.visible()) {
            int row = rowAt(active, click.x(), click.y());
            if (row >= 0 && row < active.list.size()) {
                active.selected = row;
                accept(active);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    // ---------- drawing ----------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Sug active = activeSug();
        updateGhost(holdSug, active);
        updateGhost(targetSug, active);

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
                Text.literal("Type for suggestions. Up/Down to choose, Tab or Enter to accept"),
                width / 2, height - 46, gray);

        if (active != null && active.visible()) {
            drawPopup(context, active);
        }
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
