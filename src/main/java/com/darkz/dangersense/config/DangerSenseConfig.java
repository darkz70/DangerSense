package com.darkz.dangersense.config;

/**
 * Plain data holder serialized to/from {@code config/dangersense.json}.
 * Every field has a sane default so a missing/corrupt file never crashes
 * the client — {@link ConfigManager} always falls back to {@code new DangerSenseConfig()}.
 */
public class DangerSenseConfig {

    // ---- Master toggles per detector ----
    public boolean creeperAlertEnabled = true;
    public boolean tntAlertEnabled = true;
    public boolean wardenAlertEnabled = true;
    public boolean lowHealthAlertEnabled = true;
    public boolean drowningAlertEnabled = true;
    public boolean lavaAlertEnabled = true;
    public boolean fallAlertEnabled = true;

    // ---- Detection ranges (blocks) ----
    public double creeperDetectionDistance = 8.0;
    public double tntDetectionDistance = 8.0;
    public double wardenDetectionDistance = 16.0;

    // ---- Thresholds ----
    /** Health (in half-hearts) below which the low-health alert fires. */
    public double healthThreshold = 6.0;
    /** Remaining air (in ticks, 300 = full) below which the drowning alert fires. */
    public int airThreshold = 100;
    /** Predicted fall damage (half-hearts) above which the fall alert fires. */
    public double fallDamageThreshold = 5.0;

    // ---- Warning presentation ----
    /** 0.0 - 1.0 global multiplier applied to the vignette overlay alpha. */
    public double screenIntensity = 1.0;
    /** 0.0 - 1.0 volume multiplier for the heartbeat sound. */
    public double heartbeatVolume = 1.0;
    /** Minimum ticks between heartbeat sound plays at maximum danger. */
    public int heartbeatInterval = 20;

    // ---- Performance ----
    /** How often (in ticks) detectors are re-evaluated. Higher = cheaper. */
    public int scanIntervalTicks = 4;

    // ---- Misc ----
    public boolean hotReloadEnabled = false;
}
