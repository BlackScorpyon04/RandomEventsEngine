package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.BossbarOverlay;
import com.github.blackscorpyon04.randomEventsEngine.api.CleanupHandle;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;
import com.github.blackscorpyon04.randomEventsEngine.util.WeightedPicker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class EventEngine {
    private final JavaPlugin plugin;
    private final EventRegistry registry;
    private final Map<String, Long> lastRun = new HashMap<>();
    private BukkitTask loop;
    private Preset active;
    private CleanupHandle activeCleanup = CleanupHandle.noop();
    private final Random rng = new Random();
    private long periodTicks;        // how often events run
    private long nextAtMillis;       // wall-clock time when the next event should fire
    private org.bukkit.scheduler.BukkitTask progressTask;
    private long flashUntilMillis = 0L;

    public enum FireStatus { OK, NO_ACTIVE_PRESET, NOT_FOUND, NOT_ELIGIBLE, COOLDOWN; }

    public EventEngine(JavaPlugin plugin, EventRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start(Preset preset) {
        stop();
        this.active = preset;

        this.periodTicks = Math.max(1L, preset.frequencyTicks());
        long now = System.currentTimeMillis();
        this.nextAtMillis = now + (periodTicks * 50L);

        // event tick (fires event when time comes)
        loop = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, periodTicks);

        // progress updater (every 10 ticks ~ 0.5s) - shows countdown
        progressTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossbarCountdown, 0L, 10L);
    }

    public void stop() {
        if (loop != null) loop.cancel();
        if (progressTask != null) progressTask.cancel();
        loop = null;
        progressTask = null;

        activeCleanup.cancel();
        activeCleanup = CleanupHandle.noop();

        // hide overlay on stop
        if (active != null) {
            try { active.bossbar().hide(); } catch (Throwable ignored) {}
        }
        active = null;
    }

    private void tick() {
        if (active == null) return;

        // build base context for eligibility
        EventContext baseCtx = contextFor(active, new org.bukkit.configuration.MemoryConfiguration());
        var candidates = registry.eligible(baseCtx, lastRun, active);
        if (candidates.isEmpty()) {
            // push next event window forward anyway so countdown doesn’t get stuck
            nextAtMillis = System.currentTimeMillis() + (periodTicks * 50L);
            return;
        }

        var chosen = WeightedPicker.pick(candidates, e -> active.weightFor(e));

        // per-event cfg + run
        var perEventCfg = resolvePerEventConfig(active, chosen.id());
        EventContext ctx = contextFor(active, perEventCfg);

        if (active.bossbarEnabled()) {
            active.bossbar().setText("§b" + chosen.displayName());
            active.bossbar().setProgress(1.0);
            flashUntilMillis = System.currentTimeMillis() + 3000L; // ~3s flash
        }

        activeCleanup.cancel();
        activeCleanup = chosen.run(ctx);
        lastRun.put(chosen.id(), System.currentTimeMillis());

        // reset countdown
        nextAtMillis = System.currentTimeMillis() + (periodTicks * 50L);
    }

    private void updateBossbarCountdown() {
        if (active == null || !active.bossbarEnabled()) return;

        long now = System.currentTimeMillis();
        if (now < flashUntilMillis) return; // don’t overwrite the flash

        long remaining = Math.max(0L, nextAtMillis - now);
        double progress = 1.0 - (remaining / (double) (periodTicks * 50L));
        progress = Math.max(0.0, Math.min(1.0, progress));

        long secs = Math.max(0L, (remaining + 500) / 1000);

        String txt = active.bossbarShowPreset()
                ? "Next event in " + secs + "s • " + active.name()
                : "Next event in " + secs + "s";

        active.bossbar().setText(txt);
        active.bossbar().setProgress(progress);
    }

    /** Fire a specific event by id immediately. Returns status for messaging. */
    public FireStatus fireNow(String eventId, boolean force) {
        if (active == null) return FireStatus.NO_ACTIVE_PRESET;

        EventContext baseCtx = contextFor(active, new org.bukkit.configuration.MemoryConfiguration());
        RandomEvent ev = registry.getById(eventId);
        if (ev == null) return FireStatus.NOT_FOUND;

        if (!force) {
            if (baseCtx.players().size() < ev.minPlayers()) return FireStatus.NOT_ELIGIBLE;
            if (!ev.isAllowed(baseCtx)) return FireStatus.NOT_ELIGIBLE;

            Long last = lastRun.get(ev.id());
            if (last != null && (System.currentTimeMillis() - last) < ev.cooldown().toMillis())
                return FireStatus.COOLDOWN;
        }

        var perEventCfg = resolvePerEventConfig(active, ev.id());
        EventContext runCtx = contextFor(active, perEventCfg);

        // 🔹 Add this flash section
        if (active.bossbarEnabled()) {
            String title = "§b" + ev.displayName();
            active.bossbar().setText(title);
            active.bossbar().setProgress(1.0);
            flashUntilMillis = System.currentTimeMillis() + 3000L; // 3s lockout
        }

        activeCleanup.cancel();
        activeCleanup = ev.run(runCtx);
        lastRun.put(ev.id(), System.currentTimeMillis());
        return FireStatus.OK;
    }

    /** Create a context for the current world/players + specific per-event config */
    private EventContext contextFor(Preset preset, ConfigurationSection perEventCfg) {
        var worlds = org.bukkit.Bukkit.getWorlds();
        org.bukkit.World world = worlds.isEmpty() ? null : worlds.getFirst();
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        return new BasicEventContext(
                plugin,
                world,
                players,
                rng,
                preset.bossbar(),
                perEventCfg
        );
    }

    /** config.yml -> presets.<name>.per-event.<eventId> */
    private ConfigurationSection resolvePerEventConfig(Preset preset, String eventId) {
        var root = plugin.getConfig();
        String base = "presets." + preset.name();
        var presetSec = root.getConfigurationSection(base);
        if (presetSec == null) return new MemoryConfiguration();

        var perEvent = presetSec.getConfigurationSection("per-event." + eventId);
        return perEvent != null ? perEvent : new MemoryConfiguration();
    }
}