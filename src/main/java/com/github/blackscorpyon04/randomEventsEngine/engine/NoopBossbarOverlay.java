package com.github.blackscorpyon04.randomEventsEngine.engine;

import com.github.blackscorpyon04.randomEventsEngine.api.BossbarOverlay;

public final class NoopBossbarOverlay implements BossbarOverlay {
    public void flash(String t, java.time.Duration d) {}
    public void setText(String t) {}
    public void setProgress(double p) {}
    public void hide() {}
}