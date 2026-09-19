package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.BossbarOverlay;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;

public final class BossbarOverlayImpl implements BossbarOverlay {
    private final Plugin plugin;
    private final BossBar bar;

    public BossbarOverlayImpl(Plugin plugin, BarColor color, BarStyle style) {
        this.plugin = plugin;
        this.bar = Bukkit.createBossBar("", color, style);
        bar.setVisible(false);
    }

    @Override public void flash(String text, Duration duration) {
        setText(text);
        setProgress(1.0);
        showToAllOnline();
        long ticks = Math.max(1L, duration.toMillis() / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> bar.setVisible(false), ticks);
    }

    @Override public void setText(String text) {
        bar.setTitle(text == null ? "" : text);
        showToAllOnline();
    }

    @Override public void setProgress(double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        bar.setProgress(clamped);
        showToAllOnline();
    }

    @Override public void hide() {
        bar.setVisible(false);
    }

    private void showToAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        }
        bar.setVisible(true);
    }

    public static BarColor safeColor(String v, BarColor def) {
        if (v == null || v.isBlank()) return def;
        try {
            return BarColor.valueOf(v.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    public static BarStyle safeStyle(String v, BarStyle def) {
        if (v == null || v.isBlank()) return def;
        try {
            return org.bukkit.boss.BarStyle.valueOf(v.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
