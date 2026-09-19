package com.github.blackscorpyon04.randomEventsEngine.commands;

import com.github.blackscorpyon04.randomEventsEngine.engine.EventEngine;
import com.github.blackscorpyon04.randomEventsEngine.engine.EventRegistry;
import com.github.blackscorpyon04.randomEventsEngine.engine.Preset;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class ReCommand implements org.bukkit.command.CommandExecutor, org.bukkit.command.TabCompleter {
    private final EventEngine engine;
    private final Map<String,Preset> presets;
    private Preset currentDefault;
    private final EventRegistry registry;

    public ReCommand(EventEngine engine, Map<String,Preset> presets, Preset defaultPreset, EventRegistry registry) {
        this.engine = engine;
        this.presets = presets;
        this.currentDefault = defaultPreset;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0) {
            s.sendMessage("§7Usage: /re start [preset] | /re stop | /re list | /re setdefault <name> | §e/re fire <eventId> [--force]§7 | /re events");
            return true;
        }

        String sub = a[0].toLowerCase();

        // Small helper to check permissions
        if (sub.equals("start") || sub.equals("stop") || sub.equals("setdefault") || sub.equals("fire")) {
            if (!s.hasPermission("randomevents.admin")) {
                s.sendMessage("§cYou do not have permission to do that.");
                return true;
            }
        }

        switch (sub) {
            case "start" -> {
                Preset p = (a.length >= 2) ? presets.get(a[1]) : currentDefault;
                if (p == null) { s.sendMessage("§cPreset not found."); return true; }
                engine.start(p);
                s.sendMessage("§aStarted with preset §e" + p.name());
            }
            case "stop" -> {
                engine.stop();
                s.sendMessage("§cStopped.");
            }
            case "list" -> {
                s.sendMessage("§ePresets: §7" + String.join(", ", presets.keySet()));
            }
            case "setdefault" -> {
                if (a.length < 2) { s.sendMessage("§cUsage: /re setdefault <name>"); return true; }
                Preset p = presets.get(a[1]);
                if (p == null) { s.sendMessage("§cUnknown preset."); return true; }
                currentDefault = p;
                s.sendMessage("§aDefault preset set to §e" + p.name());
            }
            case "events" -> {
                s.sendMessage("§eEvents: §7" + String.join(", ", registry.ids()));
            }
            case "fire" -> {
                if (a.length < 2) { s.sendMessage("§cUsage: /re fire <eventId> [--force]"); return true; }
                String id = a[1];
                boolean force = a.length >= 3 && a[2].equalsIgnoreCase("--force");
                var status = engine.fireNow(id, force);
                switch (status) {
                    case OK -> s.sendMessage("§aFired §e" + id + "§a.");
                    case NO_ACTIVE_PRESET -> s.sendMessage("§cNo active preset. §7Use §f/re start");
                    case NOT_FOUND -> s.sendMessage("§cUnknown event id: §f" + id + "§7. Try §f/re events");
                    case NOT_ELIGIBLE -> s.sendMessage("§eEvent not eligible right now (min players or environment).");
                    case COOLDOWN -> s.sendMessage("§eEvent is on cooldown. Use §f--force §eto override.");
                }
            }
            default -> s.sendMessage("§7Usage: /re start [preset] | /re stop | /re list | /re setdefault <name> | §e/re fire <eventId> [--force]§7 | /re events");
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender s, org.bukkit.command.Command c, String l, String[] a) {
        if (a.length == 1) {
            return java.util.stream.Stream.of("start","stop","list","setdefault","events","fire")
                    .filter(x -> x.startsWith(a[0].toLowerCase())).toList();
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("start")) {
            return presets.keySet().stream().filter(k -> k.startsWith(a[1].toLowerCase())).toList();
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("fire")) {
            return registry.ids().stream().filter(id -> id.startsWith(a[1].toLowerCase())).toList();
        }
        if (a.length == 3 && a[0].equalsIgnoreCase("fire")) {
            return java.util.List.of("--force").stream().filter(f -> f.startsWith(a[2].toLowerCase())).toList();
        }
        return java.util.List.of();
    }
}