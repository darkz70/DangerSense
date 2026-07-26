package com.darkz.dangersense.danger;

/**
 * Discrete danger levels. Each carries a base intensity (0.0 - 1.0) used
 * to scale the vignette alpha and heartbeat pacing. Detectors may further
 * scale this base value (e.g. by distance) before reporting it.
 */
public enum DangerLevel {
    NONE(0.0f),
    LOW(0.25f),
    MEDIUM(0.5f),
    HIGH(0.75f),
    CRITICAL(1.0f);

    private final float baseIntensity;

    DangerLevel(float baseIntensity) {
        this.baseIntensity = baseIntensity;
    }

    public float baseIntensity() {
        return baseIntensity;
    }

    public boolean isDangerous() {
        return this != NONE;
    }
}
