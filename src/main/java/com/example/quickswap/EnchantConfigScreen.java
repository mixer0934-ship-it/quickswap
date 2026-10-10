package com.example.quickswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Config screen for the enchanting-table cycler. */
public class EnchantConfigScreen extends Screen {
    private static final int MAX_TARGETS = 5;

    private final Screen parent;
    private boolean listening = false;
    private ButtonWidget keyButton;
    private final List<TextFieldWidget> idFields = new ArrayList<>();
    private final Map<TextFieldWidget, String> ghostFull = new IdentityHashMap<>();

    public EnchantConfigScreen(Screen parent) {
        super(Text.literal("Enchant Cycler"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        EnchantCycleConfig cfg = QuickSwapConfig.get().enchant;
        int left = width / 2 - 130;

        addDrawableChild(ButtonWidget.builder(Text.literal("Enchant cycler: " + (cfg.enabled ? "ON" : "OFF")), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(Text.literal("Enchant cycler: " + (cfg.enabled ? "ON" : "OFF")));
        }).dimensions(left, 28, 260, 20).build());

        keyButton = addDrawableChild(ButtonWidget.builder(keyLabel(cfg), b -> {
            listening = true;
            keyButton.setMessage(keyLabel(cfg));
        }).dimensions(left, 52, 260, 20).build());

        TextFieldWidget delay = new TextFieldWidget(textRenderer, left, 92, 120, 20, Text.literal("Delay"));
        delay.setTextPredicate(s -> s.matches("\\d{0,3}"));
        delay.setText(String.valueOf(cfg.delayTicks));
        delay.setChangedListener(s -> {
            try {
                cfg.delayTicks = Math.max(1, Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // empty while typing
            }
        });
        addDrawableChild(delay);

        TextFieldWidget max = new TextFieldWidget(textRenderer, left + 140, 92, 120, 20, Text.literal("Max cycles"));
        max.setTextPredicate(s -> s.matches("\\d{0,4}"));
        max.setText(String.valueOf(cfg.maxCycles));
        max.setChangedListener(s -> {
            try {
                cfg.maxCycles = Math.max(1, Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // empty while typing
            }
        });
        addDrawableChild(max);

        idFields.clear();
        ghostFull.clear();
        int y = 130;
        for (EnchantCycleConfig.Target t : new ArrayList<>(cfg.targets)) {
            TextFieldWidget idf = new TextFieldWidget(textRenderer, left, y, 170, 20, Text.literal("Enchantment"));
            idf.setMaxLength(60);
            idf.setText(t.id);
            idf.setChangedListener(s -> {
                t.id = s;
                updateGhost(idf, s);
            });
            idFields.add(idf);
            addDrawableChild(idf);

            TextFieldWidget lv = new TextFieldWidget(textRenderer, left + 174, y, 40, 20, Text.literal("Min level"));
            lv.setTextPredicate(s -> s.matches("\\d{0,2}"));
            lv.setText(String.valueOf(t.minLevel));
            lv.setChangedListener(s -> {
                try {
                    t.minLevel = Math.max(1, Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                    // empty while typing
                }
            });
            addDrawableChild(lv);

            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                cfg.targets.remove(t);
                clearAndInit();
            }).dimensions(left + 218, y, 42, 20).build());
            y += 22;
        }

        if (cfg.targets.size() < MAX_TARGETS) {
            addDrawableChild(ButtonWidget.builder(Text.literal("+ Add target"), b -> {
                cfg.targets.add(new EnchantCycleConfig.Target("", 1));
                clearAndInit();
            }).dimensions(left, y, 260, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 100, height - 28, 200, 20).build());
    }

    private Text keyLabel(EnchantCycleConfig cfg) {
        if (listening) return Text.literal("> press a key (Esc cancels) <");
        if (cfg.toggleKey < 0) return Text.literal("Start/stop key: not bound");
        return Text.literal("Start/stop key (in table screen): " + RuleEditScreen.keyName(cfg.toggleKey));
    }

    private void updateGhost(TextFieldWidget field, String typed) {
        String cand = EnchantNames.complete(typed);
        String ghost = null;
        if (cand != null && typed != null) {
            String t = typed.trim().toLowerCase();
            if (t.contains(":")) {
                if (cand.startsWith(t) && cand.length() > t.length()) ghost = cand.substring(t.length());
            } else {
                String path = cand.substring(cand.indexOf(':') + 1);
                if (path.startsWith(t) && path.length() > t.length()) ghost = path.substring(t.length());
            }
        }
        if (ghost == null) {
            ghostFull.remove(field);
        } else {
            ghostFull.put(field, cand);
        }
        field.setSuggestion(ghost);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        EnchantCycleConfig cfg = QuickSwapConfig.get().enchant;
        if (listening) {
            listening = false;
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                cfg.toggleKey = input.key();
            }
            keyButton.setMessage(keyLabel(cfg));
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_TAB) {
            for (TextFieldWidget f : idFields) {
                String full = ghostFull.get(f);
                if (f.isFocused() && full != null) {
                    f.setText(full);
                    return true;
                }
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int left = width / 2 - 130;
        int gray = 0xFFAAAAAA;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Delay per step (ticks)", left, 82, gray);
        context.drawTextWithShadow(textRenderer, "Max cycles", left + 140, 82, gray);
        context.drawTextWithShadow(textRenderer,
                "Stop when ANY of these is offered (name, min level). Tab completes.", left, 118, gray);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Put 1 item + lapis in the table, keep spare copies in your inventory"),
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
