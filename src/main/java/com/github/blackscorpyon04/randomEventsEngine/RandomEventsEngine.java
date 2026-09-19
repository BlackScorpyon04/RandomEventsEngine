package com.github.blackscorpyon04.randomEventsEngine;

import com.github.blackscorpyon04.randomEventsEngine.commands.ReCommand;
import com.github.blackscorpyon04.randomEventsEngine.engine.EventEngine;
import com.github.blackscorpyon04.randomEventsEngine.engine.EventRegistry;
import com.github.blackscorpyon04.randomEventsEngine.engine.Preset;
import com.github.blackscorpyon04.randomEventsEngine.engine.PresetManager;
import com.github.blackscorpyon04.randomEventsEngine.events.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Map;

public final class RandomEventsEngine extends JavaPlugin {

    private EventEngine engine;
    private EventRegistry registry;

    @Override public void onEnable() {
        saveDefaultConfig();

        // events
        registry = new EventRegistry();
        registry.register(new ItemRainEvent());
        registry.register(new HotbarShuffleEvent());
        registry.register(new MobAmbushEvent());
        registry.register(new FloorIsLavaEvent());
        registry.register(new PotionRouletteEvent());

        // presets
        PresetManager pm = new PresetManager(this);
        Map<String, Preset> presets = pm.loadAll();
        String defName = getConfig().getString("default-preset", "chaos-60s");
        Preset defaultPreset = presets.getOrDefault(defName, presets.values().stream().findFirst().orElse(null));

        // engine
        engine = new EventEngine(this, registry);

        // command
        var reCmd = new ReCommand(engine, presets, defaultPreset, registry);
        getCommand("re").setExecutor(reCmd);
        getCommand("re").setTabCompleter(reCmd);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (engine != null) engine.stop();
    }
}
