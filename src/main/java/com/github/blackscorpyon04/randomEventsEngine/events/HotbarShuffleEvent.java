package com.github.blackscorpyon04.randomEventsEngine.events;

import com.github.blackscorpyon04.randomEventsEngine.api.CleanupHandle;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HotbarShuffleEvent implements RandomEvent {
    @Override public String id() { return "hotbar_shuffle"; }
    @Override public String displayName() { return "Hotbar Shuffle"; }
    @Override public int weight() { return 1; }
    @Override public int minPlayers() { return 1; }
    @Override public Duration cooldown() { return Duration.ofSeconds(20); }

    @Override public boolean isAllowed(EventContext ctx) { return !ctx.players().isEmpty(); }

    @Override public CleanupHandle run(EventContext ctx) {
        ctx.title("§eHotbar Shuffle!", "Good luck finding your tools");
        Random rng = ctx.rng();

        // Save originals in case you want a timed restore (optional)
        Map<UUID, ItemStack[]> originals = new HashMap<>();

        for (Player p : ctx.players()) {
            var inv = p.getInventory();
            ItemStack[] hotbar = new ItemStack[9];
            for (int i = 0; i < 9; i++) hotbar[i] = inv.getItem(i);

            originals.put(p.getUniqueId(), Arrays.copyOf(hotbar, 9));

            // shuffle
            List<ItemStack> list = new ArrayList<>(Arrays.asList(hotbar));
            Collections.shuffle(list, rng);
            for (int i = 0; i < 9; i++) inv.setItem(i, list.get(i));
            p.updateInventory();
        }

        int restoreSecs = Math.max(0, ctx.presetConfig().getInt("restoreSeconds", 0));
        if (restoreSecs <= 0) return CleanupHandle.noop();

        AtomicBoolean restored = new AtomicBoolean(false);

        Runnable doRestore = () -> {
            if (restored.compareAndSet(false, true)) {
                for (Player p : ctx.players()) {
                    var saved = originals.get(p.getUniqueId());
                    if (saved == null) continue;
                    for (int i = 0; i < 9; i++) p.getInventory().setItem(i, saved[i]);
                    p.updateInventory();
                }
                originals.clear();
            }
        };

        var task = ctx.scheduler().runTaskLater(ctx.plugin(), doRestore, restoreSecs * 20L);

        return () -> {
            try { task.cancel(); } catch (Throwable ignored) {}
            doRestore.run();
        };
    }
}