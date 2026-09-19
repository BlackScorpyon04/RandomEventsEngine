package com.github.blackscorpyon04.randomEventsEngine.api;

import java.time.Duration;

public interface RandomEvent {
    String id();                         // "meteor_shower"
    String displayName();                // "Meteor Shower"
    int weight();                        // selection weight (config-overridable)
    int minPlayers();                    // selection eligibility
    Duration cooldown();                 // per-event cooldown

    boolean isAllowed(EventContext ctx); // world/region/perm checks
    CleanupHandle run(EventContext ctx); // execute; return cleanup if needed
}
