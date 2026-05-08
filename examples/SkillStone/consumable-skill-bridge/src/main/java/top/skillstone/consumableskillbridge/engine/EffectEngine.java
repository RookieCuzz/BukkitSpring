package top.skillstone.consumableskillbridge.engine;

import io.lumine.mythic.lib.MythicLib;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import org.bukkit.potion.PotionEffect;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.model.ConsumableEffectSpec;

public class EffectEngine {
    private final ConsumableSkillBridgePlugin plugin;

    public EffectEngine(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player, ItemStack sourceItem, ConsumableDefinition definition, boolean vanillaConsumption) {
        applyRestore(player, sourceItem, definition, vanillaConsumption);

        for (ConsumableEffectSpec effect : definition.getEffects()) {
            player.addPotionEffect(new PotionEffect(
                    effect.getType(),
                    effect.getDurationTicks(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()
            ));
        }
    }

    private void applyRestore(Player player, ItemStack sourceItem, ConsumableDefinition definition, boolean vanillaConsumption) {
        if (definition.getRestoreHealth() > 0D) {
            double maxHealth = player.getMaxHealth();
            double targetHealth = Math.min(maxHealth, player.getHealth() + definition.getRestoreHealth());
            player.setHealth(Math.max(0D, targetHealth));
        }

        int foodDelta = definition.getRestoreFood();
        if (vanillaConsumption && foodDelta != 0) {
            foodDelta -= getVanillaFoodRestored(sourceItem);
        }
        if (foodDelta != 0) {
            player.setFoodLevel(Math.max(0, Math.min(20, player.getFoodLevel() + foodDelta)));
        }

        float saturationDelta = definition.getRestoreSaturation();
        if (vanillaConsumption && saturationDelta != 0F) {
            saturationDelta -= getVanillaSaturationRestored(sourceItem);
        }
        if (saturationDelta != 0F) {
            float cap = Math.max(0F, player.getFoodLevel());
            float saturation = Math.min(cap, player.getSaturation() + saturationDelta);
            player.setSaturation(Math.max(0F, saturation));
        }

        if (definition.getRestoreMana() > 0D || definition.getRestoreStamina() > 0D) {
            plugin.getResourceRestoreBridge().restore(player, definition.getRestoreMana(), definition.getRestoreStamina());
        }
    }

    private int getVanillaFoodRestored(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        try {
            return MythicLib.plugin.getVersion().getWrapper().getFoodRestored(item);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private float getVanillaSaturationRestored(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0F;
        }
        try {
            return MythicLib.plugin.getVersion().getWrapper().getSaturationRestored(item);
        } catch (Throwable ignored) {
            return 0F;
        }
    }
}
