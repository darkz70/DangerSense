package com.darkz.dangersense.render;

/**
 * Produces a smooth 0.0-1.0 pulse value over time, driven by intensity
 * (higher intensity = faster pulse). Stateless w.r.t. allocations: only a
 * running phase double is kept.
 */
public final class PulseAnimation {

    private double phase = 0.0;

    /**
     * Advances the pulse by one frame and returns the current 0.0-1.0 value.
     *
     * @param deltaSeconds time since the last frame, in seconds
     * @param intensity    0.0-1.0 danger intensity; scales pulse speed
     */
    public float tick(double deltaSeconds, float intensity) {
        // Speed ranges from ~1.5 pulses/sec (low intensity) to ~4 pulses/sec (critical).
        double speed = 1.5 + intensity * 2.5;
        phase = (phase + deltaSeconds * speed) % 1.0;
        // Sine-based ease so it "beats" rather than sawtooths.
        return (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2.0));
    }

    public void reset() {
        phase = 0.0;
    }
}
