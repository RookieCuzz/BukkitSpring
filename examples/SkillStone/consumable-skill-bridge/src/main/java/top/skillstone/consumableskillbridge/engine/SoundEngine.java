package top.skillstone.consumableskillbridge.engine;

import org.bukkit.entity.Player;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.model.ConsumableSoundSpec;

public class SoundEngine {
    public void playConsume(Player player, ConsumableDefinition definition) {
        for (ConsumableSoundSpec sound : definition.getConsumeSounds()) {
            player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
        }
    }
}
