package com.darkz.dangersense.render;

import com.darkz.dangersense.danger.DangerWarning;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

/**
 * Draws a red screen-edge vignette whose alpha is driven by the current
 * {@link DangerWarning} intensity and a {@link PulseAnimation}. No texture
 * asset is required — the four edge gradients are drawn procedurally, which
 * keeps the mod lightweight and avoids shipping/loading an extra PNG.
 *
 * VERSION NOTE: {@code HudRenderCallback#onHudRender(GuiGraphics, DeltaTracker)}
 * is Fabric API's own method signature remapped against your chosen mapping
 * set (official Mojang mappings here, via FigureStonePlugin). The class
 * names GuiGraphics/DeltaTracker only exist from roughly 1.20+ — on 1.20.1
 * itself they should already be correct, but this is unverified against a
 * real build log for every version in the Stonecutter range.
 */
public final class VignetteOverlay implements HudRenderCallback {

    private static final int VIGNETTE_COLOR_ARGB_BASE = 0xAA0000; // dark red, alpha applied separately
    private static final int EDGE_THICKNESS_RATIO = 5; // 1/5th of screen height per edge band

    private final PulseAnimation pulse = new PulseAnimation();
    private DangerWarning currentWarning = DangerWarning.NONE;
    private long lastFrameNanos = System.nanoTime();

    public void setWarning(DangerWarning warning) {
        this.currentWarning = warning;
    }

    @Override
    public void onHudRender(GuiGraphics context, DeltaTracker tickCounter) {
        if (!currentWarning.isActive()) {
            pulse.reset();
            return;
        }

        long now = System.nanoTime();
        double deltaSeconds = Math.min(0.1, (now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;

        float pulseValue = pulse.tick(deltaSeconds, currentWarning.intensity());
        float alpha = currentWarning.intensity() * (0.4f + 0.6f * pulseValue);
        alpha = Math.max(0f, Math.min(1f, alpha));

        int width = context.guiWidth();
        int height = context.guiHeight();
        int band = Math.max(20, height / EDGE_THICKNESS_RATIO);

        int colorFull = (Math.round(alpha * 255f) << 24) | VIGNETTE_COLOR_ARGB_BASE;
        int colorNone = VIGNETTE_COLOR_ARGB_BASE; // alpha 0

        // Top edge
        context.fillGradient(0, 0, width, band, colorFull, colorNone);
        // Bottom edge
        context.fillGradient(0, height - band, width, height, colorNone, colorFull);
        // Left edge
        context.fillGradient(0, 0, band, height, colorFull, colorNone);
        // Right edge
        context.fillGradient(width - band, 0, width, height, colorNone, colorFull);
    }
}
