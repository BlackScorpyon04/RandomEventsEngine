package com.github.blackscorpyon04.randomEventsEngine.events;

import com.github.blackscorpyon04.randomEventsEngine.api.CleanupHandle;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class PotionRouletteEvent implements RandomEvent {
    @Override public String id() { return "potion_roulette"; }
    @Override public String displayName() { return "Potion Roulette"; }
    @Override public int weight() { return 1; }
    @Override public int minPlayers() { return 1; }
    @Override public Duration cooldown() { return Duration.ofSeconds(25); }

    @Override
    public boolean isAllowed(EventContext ctx) {
        return ctx.world() != null && !ctx.players().isEmpty();
    }

    @Override public CleanupHandle run(EventContext ctx) {
        var cfg = ctx.presetConfig();
        int dur = Math.max(5, cfg.getInt("durationSeconds", 20));
        int amp = Math.max(0, cfg.getInt("amplifier", 0));
        int goodW = Math.max(0, cfg.getInt("goodWeight", 2));

        var good = parseEffects(cfg.getStringList("good"));
        var bad  = parseEffects(cfg.getStringList("bad"));
        if (good.isEmpty()) good = List.of(org.bukkit.potion.PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST);
        if (bad.isEmpty())  bad  = List.of(PotionEffectType.SLOWNESS, org.bukkit.potion.PotionEffectType.WEAKNESS);

        boolean pickGood = ctx.rng().nextInt(goodW + 1) < goodW;
        var effectType = (pickGood ? good : bad).get(ctx.rng().nextInt(pickGood ? good.size() : bad.size()));

        ctx.title("§dPotion Roulette!", "§7You got: §f" + effectType.getName());

        for (var p : ctx.players()) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(effectType, dur * 20, amp, true, true, true));
        }

        // Cleanup: optional immediate clear on stop
        return () -> {
            for (var p : ctx.players()) p.removePotionEffect(effectType);
        };
    }

    private static List<org.bukkit.potion.PotionEffectType> parseEffects(List<String> names) {
        if (names == null) return List.of();
        List<org.bukkit.potion.PotionEffectType> out = new ArrayList<>();
        for (String n : names) {
            var t = org.bukkit.potion.PotionEffectType.getByName(n);
            if (t != null) out.add(t);
        }
        return out;
    }
}