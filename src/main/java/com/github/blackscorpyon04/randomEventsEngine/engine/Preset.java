package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.BossbarOverlay;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;

import java.time.Duration;
import java.util.Map;

public final class Preset {
    private final String name;
    private final Duration frequency;
    private final Map<String,Integer> perEventWeights;
    private final BossbarOverlay bossbar;
    private final boolean bossbarEnabled;
    private final boolean bossbarShowPreset;

    public Preset(String name, Duration frequency,
                  Map<String,Integer> perEventWeights,
                  BossbarOverlay bossbar,
                  boolean bossbarEnabled,
                  boolean bossbarShowPreset) {
        this.name = name;
        this.frequency = frequency;
        this.perEventWeights = perEventWeights;
        this.bossbar = bossbar;
        this.bossbarEnabled = bossbarEnabled;
        this.bossbarShowPreset = bossbarShowPreset;
    }

    public String name() { return name; }
    public long frequencyTicks() { return Math.max(1, frequency.toMillis() / 50L); }
    public int weightFor(RandomEvent e) { return perEventWeights.getOrDefault(e.id(), e.weight()); }
    public BossbarOverlay bossbar() { return bossbar; }
    public boolean bossbarEnabled() { return bossbarEnabled; }
    public boolean bossbarShowPreset() { return bossbarShowPreset; }
}