package com.github.blackscorpyon04.randomEventsEngine.events;

import com.github.blackscorpyon04.randomEventsEngine.api.CleanupHandle;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class MobAmbushEvent implements RandomEvent {

    @Override public String id() { return "mob_ambush"; }
    @Override public String displayName() { return "Mob Ambush"; }
    @Override public int weight() { return 1; }
    @Override public int minPlayers() { return 1; }
    @Override public Duration cooldown() { return Duration.ofSeconds(25); }

    @Override
    public boolean isAllowed(EventContext ctx) {
        return ctx.world() != null && !ctx.players().isEmpty();
    }

    @Override
    public CleanupHandle run(EventContext ctx) {
        final ConfigurationSection cfg = ctx.presetConfig();
        final Random rng = ctx.rng();

        int count = Math.max(1, cfg.getInt("count", 5));
        int capPerPlayer = Math.max(1, cfg.getInt("capPerPlayer", 8));
        int radius = Math.max(2, cfg.getInt("radius", 6));
        int yOffset = cfg.getInt("yOffset", 0);
        int despawnSeconds = Math.max(5, cfg.getInt("despawnSeconds", 25));
        boolean aggro = cfg.getBoolean("aggro", true);

        // Parse entity types
        List<EntityType> pool = parseTypes(cfg.getStringList("types"));
        if (pool.isEmpty()) {
            pool = List.of(EntityType.ZOMBIE, EntityType.SKELETON);
        }

        ctx.title("§cMob Ambush!", "§7Brace yourselves…");
        ctx.sound(Sound.ENTITY_WITHER_SPAWN);

        List<LivingEntity> spawned = new ArrayList<>();

        for (Player target : ctx.players()) {
            // skip spectators or dead
            if (!target.isValid() || target.isDead() || target.getGameMode() == GameMode.SPECTATOR) continue;

            // cap per player
            int n = Math.min(count, capPerPlayer);

            // spawn in a ring around the player
            Location base = target.getLocation().clone().add(0, yOffset, 0);
            for (int i = 0; i < n; i++) {
                double angle = (2 * Math.PI * i) / n + rng.nextDouble() * 0.6 - 0.3; // small jitter
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;

                Location loc = safeSpawnNear(base.clone().add(dx, 0, dz));
                if (loc == null) continue;

                EntityType type = pool.get(rng.nextInt(pool.size()));
                LivingEntity mob = (LivingEntity) ctx.world().spawnEntity(loc, type);

                prepareMob(mob, cfg.getConfigurationSection("gear"), rng);
                if (aggro) setAggro(mob, target);

                spawned.add(mob);
            }
        }

        // Cleanup (despawn)
        long ticks = Math.max(1, despawnSeconds) * 20L;
        AtomicBoolean cleared = new AtomicBoolean(false);

        Runnable doClear = () -> {
            if (cleared.compareAndSet(false, true)) {
                for (LivingEntity e : spawned) {
                    if (e != null && e.isValid() && !e.isDead()) e.remove();
                }
                spawned.clear();
            }
        };

        var task = ctx.scheduler().runTaskLater(ctx.plugin(), doClear, ticks);

        return () -> {
            try { task.cancel(); } catch (Throwable ignored) {}
            doClear.run();
        };
    }

    // --- helpers ---

    private static List<EntityType> parseTypes(List<String> names) {
        if (names == null) return List.of();
        return names.stream()
                .map(s -> safeType(s))
                .filter(Objects::nonNull)
                .filter(EntityType::isAlive)
                .filter(t -> LivingEntity.class.isAssignableFrom(t.getEntityClass()))
                .collect(Collectors.toList());
    }

    private static EntityType safeType(String s) {
        if (s == null) return null;
        try { return EntityType.valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }

    /** Try to place on top of solid ground; returns null if unsafe. */
    private static Location safeSpawnNear(Location guess) {
        World w = guess.getWorld();
        if (w == null) return null;

        // scan up and down a few blocks to find a spot with solid ground and two-air headroom
        int x = guess.getBlockX();
        int z = guess.getBlockZ();
        for (int dy = 6; dy >= -6; dy--) {
            int y = guess.getBlockY() + dy;
            if (y < w.getMinHeight() + 2 || y >= w.getMaxHeight() - 2) continue;

            Block feet = w.getBlockAt(x, y, z);
            Block head = w.getBlockAt(x, y + 1, z);
            Block ground = w.getBlockAt(x, y - 1, z);

            if (feet.isPassable() && head.isPassable() && ground.getType().isSolid()) {
                Location loc = new Location(w, x + 0.5, y, z + 0.5);
                return loc;
            }
        }
        return null;
    }

    private static void prepareMob(LivingEntity mob, ConfigurationSection gear, Random rng) {
        // Basic tuning so it's fair but noticeable
        mob.setRemoveWhenFarAway(true);
        var max = mob.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) { max.setBaseValue(Math.min(40.0, max.getBaseValue())); mob.setHealth(max.getBaseValue()); }

        if (gear == null) return;
        EntityEquipment eq = mob.getEquipment();
        if (eq == null) return;

        setItem(eq::setHelmet, gear.getString("helmet"));
        setItem(eq::setChestplate, gear.getString("chestplate"));
        setItem(eq::setLeggings, gear.getString("leggings"));
        setItem(eq::setBoots, gear.getString("boots"));
        setItem(eq::setItemInMainHand, gear.getString("mainHand"));
        setItem(eq::setItemInOffHand, gear.getString("offHand"));

        // Reduce drop chance so it doesn't shower loot
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
        eq.setItemInOffHandDropChance(0f);
    }

    private static void setAggro(LivingEntity mob, Player target) {
        if (mob instanceof Mob m) {
            m.setTarget(target);
        }
    }

    private static void setItem(java.util.function.Consumer<ItemStack> setter, String matName) {
        if (matName == null || matName.isBlank()) return;
        Material mat = Material.matchMaterial(matName, false);
        if (mat == null) mat = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
        if (mat == null) return;
        setter.accept(new ItemStack(mat, 1));
    }
}
