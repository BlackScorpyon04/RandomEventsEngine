package com.github.blackscorpyon04.randomEventsEngine.api;

public interface CleanupHandle {
    void cancel();                       // stop ongoing tasks / restore state
    default boolean isNoop(){ return false; }
    static CleanupHandle noop(){ return () -> {}; }
}
