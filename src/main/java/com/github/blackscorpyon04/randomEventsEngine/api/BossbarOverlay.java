package com.github.blackscorpyon04.randomEventsEngine.api;

import java.time.Duration;

public interface BossbarOverlay {
    void flash(String text, Duration duration);
    void setText(String text);
    void setProgress(double progress); // 0..1
    void hide();
}