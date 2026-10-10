package com.example.quickswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Main screen: global toggles + paged list of rules. Opened from Mod Menu. */
public class RuleListScreen extends Screen {
    private final Screen parent;
    private int page = 0;
    private int perPage = 6;

    public RuleListScreen(Screen parent) {
        super(Text.literal("QuickSwap"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QuickSwapConfig cfg = QuickSwapConfig.get();
        int cx = width / 2;
        int left = cx - 160;

        // Global toggles
        addDrawableChild(ButtonWidget.builder(Text.literal("Mod: " + onOff(cfg.enabled)), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(Text.literal("Mod: " + onOff(cfg.enabled)));
        }).dimensions(left, 26, 158, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Messages: " + onOff(cfg.showMessages)), b -> {
            cfg.showMessages = !cfg.showMessages;
            b.setMessage(Text.literal("Messages: " + onOff(cfg.showMessages)));
        }).dimensions(left + 162, 26, 158, 20).build());

        // Rule rows
        perPage = Math.max(1, Math.min(8, (height - 52 - 62) / 24));
        List<SwapRule> rules = cfg.rules;
        int pages = Math.max(1, (rules.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        int y = 52;
        int end = Math.min(rules.size(), (page + 1) * perPage);
        for (int i = page * perPage; i < end; i++) {
            final SwapRule rule = rules.get(i);

            addDrawableChild(ButtonWidget.builder(Text.literal(onOff(rule.enabled)), b -> {
                rule.enabled = !rule.enabled;
                b.setMessage(Text.literal(onOff(rule.enabled)));
            }).dimensions(left, y, 36, 20).build());

            String keyText = rule.keyCode < 0 ? "unbound" : RuleEditScreen.bindName(rule);
            addDrawableChild(ButtonWidget.builder(Text.literal(rule.name + "  [" + keyText + "]"),
                    b -> client.setScreen(new RuleEditScreen(this, rule)))
                    .dimensions(left + 40, y, 240, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                cfg.rules.remove(rule);
                clearAndInit();
            }).dimensions(left + 284, y, 36, 20).build());

            y += 24;
        }

        // Pagination + new rule
        int navY = height - 52;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            clearAndInit();
        }).dimensions(left, navY, 24, 20).build()).active = page > 0;

        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            clearAndInit();
        }).dimensions(left + 28, navY, 24, 20).build()).active = page < pages - 1;

        addDrawableChild(ButtonWidget.builder(Text.literal("+ New rule"), b -> {
            SwapRule r = new SwapRule();
            cfg.rules.add(r);
            page = Math.max(0, (cfg.rules.size() - 1) / perPage);
            client.setScreen(new RuleEditScreen(this, r));
        }).dimensions(left + 160, navY, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx - 100, height - 28, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        QuickSwapConfig cfg = QuickSwapConfig.get();
        int pages = Math.max(1, (cfg.rules.size() + perPage - 1) / perPage);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Page " + (page + 1) + "/" + pages,
                width / 2 - 160 + 58, height - 46, 0xFFAAAAAA);
        if (cfg.rules.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No rules yet. Click \"+ New rule\"."), width / 2, 90, 0xFFAAAAAA);
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

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }
}
