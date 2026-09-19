package com.github.blackscorpyon04.randomEventsEngine.api;

import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.Random;

public interface EventContext {
    Server server();
    World world();                       // target world (config-selectable)
    List<Player> players();              // eligible players
    Random rng();
    void broadcast(String msg);
    void title(String title, String subtitle);
    void sound(Sound s);
    BukkitScheduler scheduler();
    BossbarOverlay bossbar();
    ConfigurationSection presetConfig(); // per-preset overrides for this event
    JavaPlugin plugin();
}
