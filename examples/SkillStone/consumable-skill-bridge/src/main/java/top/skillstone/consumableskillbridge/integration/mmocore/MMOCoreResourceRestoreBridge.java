package top.skillstone.consumableskillbridge.integration.mmocore;

import net.Indyuce.mmocore.api.event.PlayerResourceUpdateEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.entity.Player;
import top.skillstone.consumableskillbridge.integration.ResourceRestoreBridge;

public class MMOCoreResourceRestoreBridge implements ResourceRestoreBridge {
    @Override
    public String getProviderName() {
        return "MMOCore";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public double getMana(Player player) {
        PlayerData data = PlayerData.get(player);
        return data == null ? 0D : Math.max(0D, data.getMana());
    }

    @Override
    public double getStamina(Player player) {
        PlayerData data = PlayerData.get(player);
        return data == null ? 0D : Math.max(0D, data.getStamina());
    }

    @Override
    public void consume(Player player, double mana, double stamina) {
        if (player == null || (mana <= 0D && stamina <= 0D)) {
            return;
        }

        PlayerData playerData = PlayerData.get(player);
        if (playerData == null) {
            return;
        }

        if (mana > 0D) {
            takeMana(playerData, mana);
        }
        if (stamina > 0D) {
            takeStamina(playerData, stamina);
        }
    }

    @Override
    public void restore(Player player, double mana, double stamina) {
        if (player == null || (mana <= 0D && stamina <= 0D)) {
            return;
        }

        PlayerData playerData = PlayerData.get(player);
        if (playerData == null) {
            return;
        }

        if (mana > 0D) {
            giveMana(playerData, mana);
        }
        if (stamina > 0D) {
            giveStamina(playerData, stamina);
        }
    }

    private void giveMana(PlayerData playerData, double amount) {
        try {
            playerData.giveMana(amount, PlayerResourceUpdateEvent.UpdateReason.OTHER);
        } catch (Throwable ignored) {
            playerData.setMana(playerData.getMana() + amount);
        }
    }

    private void giveStamina(PlayerData playerData, double amount) {
        try {
            playerData.giveStamina(amount, PlayerResourceUpdateEvent.UpdateReason.OTHER);
        } catch (Throwable ignored) {
            playerData.setStamina(playerData.getStamina() + amount);
        }
    }

    private void takeMana(PlayerData playerData, double amount) {
        try {
            playerData.giveMana(-amount, PlayerResourceUpdateEvent.UpdateReason.OTHER);
        } catch (Throwable ignored) {
            playerData.setMana(playerData.getMana() - amount);
        }
    }

    private void takeStamina(PlayerData playerData, double amount) {
        try {
            playerData.giveStamina(-amount, PlayerResourceUpdateEvent.UpdateReason.OTHER);
        } catch (Throwable ignored) {
            playerData.setStamina(playerData.getStamina() - amount);
        }
    }
}
