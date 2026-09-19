package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.EventContext;
import com.github.blackscorpyon04.randomEventsEngine.api.RandomEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EventRegistry {
    private final List<RandomEvent> events = new ArrayList<>();

    public void register(RandomEvent e) { events.add(e); }

    public List<RandomEvent> eligible(EventContext ctx, Map<String,Long> lastRun, Preset preset) {
        long now = System.currentTimeMillis();
        List<RandomEvent> out = new ArrayList<>();
        for (var e : events) {
            if (ctx.players().size() < e.minPlayers()) continue;
            Long last = lastRun.get(e.id());
            if (last != null && (now - last) < e.cooldown().toMillis()) continue;
            if (!e.isAllowed(ctx)) continue;
            out.add(e);
        }
        return out;
    }

    public RandomEvent getById(String id) {
        for (var e : events) if (e.id().equalsIgnoreCase(id)) return e;
        return null;
    }

    public List<String> ids() {
        return events.stream().map(RandomEvent::id).toList();
    }

    public List<RandomEvent> all() { return List.copyOf(events); }
}
