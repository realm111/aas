package pl.autotpa;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutoTpaScreen extends Screen {
    private Button toggleButton;

    public AutoTpaScreen() {
        super(Component.literal("AutoTPA"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        toggleButton = Button.builder(toggleText(), b -> {
            AutoTpaClient.enabled = !AutoTpaClient.enabled;
            b.setMessage(toggleText());
        }).bounds(cx - 100, cy - 30, 200, 20).build();
        addRenderableWidget(toggleButton);

        addRenderableWidget(Button.builder(Component.literal("-10 s"), b ->
            AutoTpaClient.cooldownSeconds = Math.max(10, AutoTpaClient.cooldownSeconds - 10))
            .bounds(cx - 100, cy + 15, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+10 s"), b ->
            AutoTpaClient.cooldownSeconds = Math.min(3600, AutoTpaClient.cooldownSeconds + 10))
            .bounds(cx + 40, cy + 15, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Zamknij"), b -> onClose())
            .bounds(cx - 50, cy + 50, 100, 20).build());
    }

    private Component toggleText() {
        return Component.literal(AutoTpaClient.enabled ? "AutoTPA: WŁĄCZONY" : "AutoTPA: WYŁĄCZONY");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, "AutoTPA", cx, cy - 55, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Cooldown na gracza: " + AutoTpaClient.cooldownSeconds + " s", cx, cy + 20, 0xFFFFFFFF);
    }
}
