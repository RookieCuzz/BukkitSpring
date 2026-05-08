package top.skillstone.consumableskillbridge.integration;

import org.bukkit.entity.Player;

public final class NoopResourceRestoreBridge implements ResourceRestoreBridge {
    public static final NoopResourceRestoreBridge INSTANCE = new NoopResourceRestoreBridge();

    private NoopResourceRestoreBridge() {
    }

    @Override
    public String getProviderName() {
        return "none";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public double getMana(Player player) {
        return 0D;
    }

    @Override
    public double getStamina(Player player) {
        return 0D;
    }

    @Override
    public void consume(Player player, double mana, double stamina) {
    }

    @Override
    public void restore(Player player, double mana, double stamina) {
    }
}
