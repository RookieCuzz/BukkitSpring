package top.skillstone.consumableskillbridge.integration;

import org.bukkit.entity.Player;

public interface ResourceRestoreBridge {
    String getProviderName();

    boolean isAvailable();

    double getMana(Player player);

    double getStamina(Player player);

    void consume(Player player, double mana, double stamina);

    void restore(Player player, double mana, double stamina);
}
