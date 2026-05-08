package top.skillstone.consumableskillbridge.model;

import org.bukkit.potion.PotionEffectType;

public class ConsumableEffectSpec {
    private final PotionEffectType type;
    private final int durationTicks;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;
    private final boolean icon;

    public ConsumableEffectSpec(PotionEffectType type, int durationTicks, int amplifier, boolean ambient, boolean particles, boolean icon) {
        this.type = type;
        this.durationTicks = Math.max(1, durationTicks);
        this.amplifier = Math.max(0, amplifier);
        this.ambient = ambient;
        this.particles = particles;
        this.icon = icon;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public boolean isAmbient() {
        return ambient;
    }

    public boolean hasParticles() {
        return particles;
    }

    public boolean hasIcon() {
        return icon;
    }
}
