package top.skillstone.consumableskillbridge.model;

import org.bukkit.Sound;

public class ConsumableSoundSpec {
    private final Sound sound;
    private final float volume;
    private final float pitch;

    public ConsumableSoundSpec(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public Sound getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}
