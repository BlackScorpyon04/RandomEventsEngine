package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.BossbarOverlay;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PresetManager {
    private final Plugin plugin;

    public PresetManager(Plugin plugin) { this.plugin = plugin; }

    public Map<String, Preset> loadAll() {
        Map<String, Preset> out = new LinkedHashMap<>();
        ConfigurationSection presets = plugin.getConfig().getConfigurationSection("presets");
        if (presets == null) return out;

        for (String name : presets.getKeys(false)) {
            ConfigurationSection p = presets.getConfigurationSection(name);
            long freqSec = p.getLong("frequency-seconds", 60);

            Map<String,Integer> weights = new HashMap<>();
            ConfigurationSection w = p.getConfigurationSection("weights");
            if (w != null) for (String k : w.getKeys(false)) weights.put(k, w.getInt(k, 1));

            ConfigurationSection bb = p.getConfigurationSection("bossbar");
            boolean enabled = bb == null || bb.getBoolean("enabled", true);
            boolean showPreset = bb != null && bb.getBoolean("showPreset", false);

            BarColor color = BossbarOverlayImpl.safeColor(bb == null ? null : bb.getString("color"), BarColor.BLUE);
            BarStyle style = BossbarOverlayImpl.safeStyle(bb == null ? null : bb.getString("style"), BarStyle.SOLID);

            BossbarOverlay overlay = enabled
                    ? new BossbarOverlayImpl(plugin, color, style)
                    : new NoopBossbarOverlay(); // a do-nothing implementation

            Preset preset = new Preset(name, Duration.ofSeconds(freqSec), weights, overlay, enabled, showPreset);
            out.put(name, preset);
        }
        return out;
    }

    private static BarColor safeColor(String v, BarColor def) {
        if (v == null) return def;
        try { return BarColor.valueOf(v.toUpperCase(Locale.ROOT)); } catch (Exception e) { return def; }
    }
    private static BarStyle safeStyle(String v, BarStyle def) {
        if (v == null) return def;
        try { return BarStyle.valueOf(v.toUpperCase(Locale.ROOT)); } catch (Exception e) { return def; }
    }
}