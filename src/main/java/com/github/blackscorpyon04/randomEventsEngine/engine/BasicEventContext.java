package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.BossbarOverlay;
import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BasicEventContext implements EventContext {
    private final JavaPlugin plugin;
    private final Server server;
    private final World world;
    private final List<Player> players;
    private final Random rng;
    private final BukkitScheduler scheduler;
    private final BossbarOverlay bossbar;
    private final ConfigurationSection presetConfig;

    public BasicEventContext(JavaPlugin plugin,
                             World world,
                             List<Player> players,
                             Random rng,
                             BossbarOverlay bossbar,
                             ConfigurationSection presetConfig) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.world = world;
        this.players = players;
        this.rng = rng;
        this.scheduler = Bukkit.getScheduler();
        this.bossbar = bossbar;
        this.presetConfig = (presetConfig != null) ? presetConfig : new MemoryConfiguration();
    }

    @Override public Server server() { return server; }
    @Override public World world() { return world; }
    @Override public List<Player> players() { return players; }
    @Override public Random rng() { return rng; }
    @Override public void broadcast(String msg) { server.broadcastMessage(msg); }
    @Override public void title(String title, String subtitle) {
        for (Player p : players) p.sendTitle(title, subtitle, 10, 40, 10);
    }
    @Override public void sound(Sound s) {
        for (Player p : players) p.playSound(p.getLocation(), s, 1f, 1f);
    }
    @Override public BukkitScheduler scheduler() { return scheduler; }
    @Override public BossbarOverlay bossbar() { return bossbar; }
    @Override public ConfigurationSection presetConfig() { return presetConfig; }
    @Override public JavaPlugin plugin() { return plugin; }
}
