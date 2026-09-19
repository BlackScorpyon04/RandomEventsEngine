package com.github.blackscorpyon04.randomEventsEngine.events;

import com.github.blackscorpyon04.randomEventsEngine.api.CleanupHandle;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;
import com.github.blackscorpyon04.randomEventsEngine.util.ItemSpecs;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ItemRainEvent implements RandomEvent {
    @Override public String id() { return "item_rain"; }
    @Override public String displayName() { return "Item Rain"; }
    @Override public int weight() { return 1; }
    @Override public int minPlayers() { return 1; }
    @Override public Duration cooldown() { return Duration.ofSeconds(15); }

    @Override public boolean isAllowed(EventContext ctx) {
        return !ctx.players().isEmpty();
    }

    @Override public CleanupHandle run(EventContext ctx) {
        var cfg = ctx.presetConfig();
        int height = cfg.getInt("height", 16);
        int count  = cfg.getInt("count", 30);

        // Parse once
        var items = ItemSpecs.parse(cfg.getStringList("items"), ctx.plugin());
        if (items.isEmpty()) {
            ctx.broadcast("§e[ItemRain] No valid items configured.");
            return CleanupHandle.noop();
        }

        ctx.title("§aItem Rain!", "§7Look up…");

        List<org.bukkit.entity.Item> spawned = new ArrayList<>();
        Random rng = ctx.rng();

        for (int i = 0; i < count; i++) {
            var p = ctx.players().get(rng.nextInt(ctx.players().size()));
            var loc = p.getLocation().clone().add(rng.nextInt(13) - 6, height, rng.nextInt(13) - 6);

            // pick a spec and build a stack with a random amount in range
            var spec = items.get(rng.nextInt(items.size()));
            var stack = spec.newStack(rng);

            var drop = ctx.world().dropItem(loc, stack);
            drop.setPickupDelay(20);
            spawned.add(drop);
        }

        int despawn = Math.max(0, cfg.getInt("despawnSeconds", 45));
        AtomicBoolean cleared = new AtomicBoolean(false);

        Runnable doClear = () -> {
            if (cleared.compareAndSet(false, true)) {
                for (var e : spawned) {
                    if (e != null && !e.isDead()) e.remove();
                }
                spawned.clear();
            }
        };

        var task = ctx.scheduler().runTaskLater(ctx.plugin(), doClear, despawn * 20L);

        return () -> {
            try { task.cancel(); } catch (Throwable ignored) {}
            doClear.run();
        };
    }
}
