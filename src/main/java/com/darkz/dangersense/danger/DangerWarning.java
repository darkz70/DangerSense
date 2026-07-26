package com.darkz.dangersense.danger;

/**
 * Immutable snapshot of the strongest active warning for a given tick.
 * Passed from {@link WarningManager} to the render/sound layers.
 */
public record DangerWarning(String sourceId, DangerLevel level, float intensity) {

    public static final DangerWarning NONE = new DangerWarning("none", DangerLevel.NONE, 0.0f);

    public boolean isActive() {
        return level.isDangerous() && intensity > 0.0f;
    }
}
