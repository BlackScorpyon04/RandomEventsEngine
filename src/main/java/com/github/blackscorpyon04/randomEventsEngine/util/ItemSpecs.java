package com.github.blackscorpyon04.randomEventsEngine.util;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

public final class ItemSpecs {

    public static final class ItemSpec {
        public final Material material;
        public final int min;
        public final int max;

        public ItemSpec(Material material, int min, int max) {
            this.material = material;
            this.min = Math.max(1, min);
            this.max = Math.max(this.min, max);
        }

        public org.bukkit.inventory.ItemStack newStack(Random rng) {
            int amt = (min == max) ? min : (min + rng.nextInt(max - min + 1));
            int cap = Math.max(1, material.getMaxStackSize());
            amt = Math.min(amt, cap);
            return new org.bukkit.inventory.ItemStack(material, amt);
        }

        @Override public String toString() {
            return material + ":" + (min == max ? String.valueOf(min) : (min + ".." + max));
        }
    }

    private static final String COUNT_PATTERN = "\\d+(?:\\.\\.\\d+)?";

    public static List<ItemSpec> parse(List<String> lines, Plugin plugin) {
        Logger log = plugin.getLogger();
        List<ItemSpec> out = new ArrayList<>();
        if (lines == null) return out;

        for (String raw : lines) {
            if (raw == null) continue;
            String s = raw.trim();
            if (s.isEmpty()) continue;

            // Split on the LAST ':' only if the part after it looks like a count/range
            String matPart = s;
            String countPart = null;

            int lastColon = s.lastIndexOf(':');
            if (lastColon >= 0) {
                String tail = s.substring(lastColon + 1).trim();
                if (tail.matches(COUNT_PATTERN)) {
                    matPart = s.substring(0, lastColon).trim();
                    countPart = tail;
                }
            }

            // Normalize material name (allow both namespaced and vanilla short names)
            String matName = matPart;
            if (matName.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
                matName = matName.substring("minecraft:".length());
            }

            // Try to resolve material (case-insensitive)
            Material mat = Material.matchMaterial(matName, false); // Paper/modern Spigot: case-insensitive
            if (mat == null) {
                // Fallback: uppercase with underscores for older matchers
                mat = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
            }
            if (mat == null) {
                log.warning("[RandomEvents] ItemRain: unknown material in '" + raw + "' → skipping");
                continue;
            }

            int min = 1, max = 1;
            if (countPart != null) {
                int dots = countPart.indexOf("..");
                try {
                    if (dots >= 0) {
                        int a = Integer.parseInt(countPart.substring(0, dots).trim());
                        int b = Integer.parseInt(countPart.substring(dots + 2).trim());
                        min = Math.min(a, b);
                        max = Math.max(a, b);
                    } else {
                        int v = Integer.parseInt(countPart.trim());
                        min = max = v;
                    }
                } catch (NumberFormatException nfe) {
                    log.warning("[RandomEvents] ItemRain: bad count '" + countPart + "' in '" + raw + "' → defaulting to 1");
                }
            }

            // Clamp to stack size
            int cap = Math.max(1, mat.getMaxStackSize());
            min = Math.max(1, Math.min(min, cap));
            max = Math.max(min, Math.min(max, cap));

            out.add(new ItemSpec(mat, min, max));
        }
        return out;
    }

    private ItemSpecs() {}
}
