package com.savyous.pinged;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class PingedConfigScreen extends Screen {
    private final Screen parent;
    private ButtonWidget durationButton;

    public PingedConfigScreen(Screen parent) {
        super(Text.literal("Pinged Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4 + 24;

        this.durationButton = ButtonWidget.builder(
                Text.literal("Ping Duration: §e" + PingManager.getPingDurationSeconds() + "s"),
                btn -> {
                    int next = switch (PingManager.getPingDurationSeconds()) {
                        case 5 -> 10;
                        case 10 -> 15;
                        case 15 -> 20;
                        case 20 -> 30;
                        case 30 -> 60;
                        default -> 5;
                    };
                    PingManager.setPingDurationSeconds(next);
                    this.durationButton.setMessage(Text.literal("Ping Duration: §e" + next + "s"));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build();

        this.addDrawableChild(this.durationButton);

        this.addDrawableChild(ButtonWidget.builder(
                ScreenTexts.DONE,
                btn -> this.close()
        ).dimensions(centerX - 100, startY + 50, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 4, 0xFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
