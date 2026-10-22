package com.chanwoo.deeperdark.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DeeperDarkConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 240;

    @Nullable
    private final Screen parent;

    public DeeperDarkConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("config.deeper_dark.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        boolean installed = DeeperDarkConfig.isLodestoneInstalled();

        CyclingButtonWidget<Boolean> lodestone = CyclingButtonWidget.onOffBuilder(DeeperDarkConfig.getLodestoneParticlesSetting())
                .tooltip(value -> this.textRenderer.wrapLines(Text.translatable(installed
                        ? "config.deeper_dark.lodestone_particles.tooltip"
                        : "config.deeper_dark.lodestone_particles.missing"), 200))
                .build(x, this.height / 2 - 12, BUTTON_WIDTH, 20, Text.translatable("config.deeper_dark.lodestone_particles"),
                        (button, value) -> DeeperDarkConfig.setLodestoneParticles(value));
        lodestone.active = installed;
        this.addDrawableChild(lodestone);

        this.addDrawableChild(new ButtonWidget(x, this.height / 2 + 20, BUTTON_WIDTH, 20, ScreenTexts.DONE, button -> this.close()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        if (!DeeperDarkConfig.isLodestoneInstalled()) {
            drawCenteredText(matrices, this.textRenderer,
                    Text.translatable("config.deeper_dark.lodestone_particles.missing").formatted(Formatting.GRAY),
                    this.width / 2, this.height / 2 - 44, 0xFFFFFF);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
