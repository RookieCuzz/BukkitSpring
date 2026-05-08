package top.skillstone.consumableskillbridge.engine;

import org.bukkit.entity.Player;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.integration.ResourceRestoreBridge;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.model.RequirementSpec;

import java.util.Locale;

public class RequirementEngine {
    private final ConsumableSkillBridgePlugin plugin;

    public RequirementEngine(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public RequirementResult check(Player player, ConsumableDefinition definition) {
        RequirementSpec spec = definition.getRequirements();
        if (spec == null) {
            return RequirementResult.success();
        }

        String permission = spec.getPermission();
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return RequirementResult.fail("Missing permission: " + permission);
        }

        if (spec.getMinLevel() > 0 && player.getLevel() < spec.getMinLevel()) {
            return RequirementResult.fail("Requires level " + spec.getMinLevel());
        }

        if (spec.getMinFood() > 0 && player.getFoodLevel() < spec.getMinFood()) {
            return RequirementResult.fail("Requires food level " + spec.getMinFood());
        }

        if (spec.hasResourceRequirementOrCost()) {
            ResourceRestoreBridge bridge = plugin.getResourceRestoreBridge();
            if (!bridge.isAvailable()) {
                return RequirementResult.fail("Resource provider unavailable (requires MMOCore)");
            }

            double currentMana = bridge.getMana(player);
            double currentStamina = bridge.getStamina(player);

            if (spec.getMinMana() > 0D && currentMana < spec.getMinMana()) {
                return RequirementResult.fail("Requires mana >= " + format(spec.getMinMana()));
            }
            if (spec.getMinStamina() > 0D && currentStamina < spec.getMinStamina()) {
                return RequirementResult.fail("Requires stamina >= " + format(spec.getMinStamina()));
            }
            if (spec.getManaCost() > 0D && currentMana < spec.getManaCost()) {
                return RequirementResult.fail("Not enough mana (cost: " + format(spec.getManaCost()) + ")");
            }
            if (spec.getStaminaCost() > 0D && currentStamina < spec.getStaminaCost()) {
                return RequirementResult.fail("Not enough stamina (cost: " + format(spec.getStaminaCost()) + ")");
            }
        }

        return RequirementResult.success();
    }

    public void applyCosts(Player player, ConsumableDefinition definition) {
        RequirementSpec spec = definition.getRequirements();
        if (spec == null) {
            return;
        }

        double manaCost = spec.getManaCost();
        double staminaCost = spec.getStaminaCost();
        if (manaCost <= 0D && staminaCost <= 0D) {
            return;
        }

        ResourceRestoreBridge bridge = plugin.getResourceRestoreBridge();
        if (!bridge.isAvailable()) {
            return;
        }
        bridge.consume(player, manaCost, staminaCost);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static final class RequirementResult {
        private final boolean success;
        private final String reason;

        private RequirementResult(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }

        public static RequirementResult success() {
            return new RequirementResult(true, "");
        }

        public static RequirementResult fail(String reason) {
            return new RequirementResult(false, reason);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getReason() {
            return reason;
        }
    }
}
