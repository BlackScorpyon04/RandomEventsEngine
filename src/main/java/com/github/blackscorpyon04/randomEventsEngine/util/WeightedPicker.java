package com.github.blackscorpyon04.randomEventsEngine.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;

public final class WeightedPicker {
    public static <T> T pick(List<T> items, ToIntFunction<T> weightFn) {
        int total = 0;
        for (T t : items) total += Math.max(0, weightFn.applyAsInt(t));
        if (total <= 0) return items.get(ThreadLocalRandom.current().nextInt(items.size()));
        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (T t : items) {
            acc += Math.max(0, weightFn.applyAsInt(t));
            if (r < acc) return t;
        }
        return items.get(items.size()-1);
    }
}