package com.github.blackscorpyon04.randomEventsEngine.events;

import com.github.blackscorpyon04.randomEventsEngine.api.CleanupHandle;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FloorIsLavaEvent implements RandomEvent {
    @Override public String id() { return "floor_is_lava"; }
    @Override public String displayName() { return "Floor Is Lava"; }
    @Override public int weight() { return 1; }
    @Override public int minPlayers() { return 1; }
    @Override public Duration cooldown() { return Duration.ofSeconds(20); }

    @Override public boolean isAllowed(EventContext ctx) { return !ctx.players().isEmpty(); }

    @Override public CleanupHandle run(EventContext ctx) {
        var cfg = ctx.presetConfig();
        int r = Math.max(2, cfg.getInt("radius", 5));
        int dur = Math.max(5, cfg.getInt("durationSeconds", 10));
        Material lavaBlock = Optional.ofNullable(Material.matchMaterial(cfg.getString("block","MAGMA_BLOCK"))).orElse(Material.MAGMA_BLOCK);
        boolean revert = cfg.getBoolean("revert", true);
        boolean perPlayer = cfg.getBoolean("perPlayer", true);

        ctx.title("§cFloor is Lava!", "§7Watch your step");

        List<BlockState> saved = new ArrayList<>();

        java.util.function.Consumer<org.bukkit.Location> paint = (center) -> {
            var w = center.getWorld();
            int cx = center.getBlockX(), cy = center.getBlockY()-1, cz = center.getBlockZ();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx*dx + dz*dz > r*r) continue;
                    var b = w.getBlockAt(cx+dx, cy, cz+dz);
                    if (b.getType() == lavaBlock) continue;
                    if (!b.getType().isSolid()) continue;
                    saved.add(b.getState());
                    b.setType(lavaBlock, false);
                }
            }
        };

        if (perPlayer) ctx.players().forEach(p -> paint.accept(p.getLocation()));
        else paint.accept(ctx.players().get(0).getLocation());

        AtomicBoolean reverted = new AtomicBoolean(false);

        Runnable doRevert = () -> {
            if (reverted.compareAndSet(false, true)) {
                for (var s : saved) s.update(true, false);  // put blocks back
                saved.clear();
                // optional: ctx.bossbar().hide();
            }
        };

        // schedule timed revert
        var end = ctx.scheduler().runTaskLater(ctx.plugin(), doRevert, dur * 20L);

        // cleanup handle: cancel timer and revert immediately if not yet reverted
        return () -> {
            try { end.cancel(); } catch (Throwable ignored) {}
            doRevert.run();
        };
    }
}