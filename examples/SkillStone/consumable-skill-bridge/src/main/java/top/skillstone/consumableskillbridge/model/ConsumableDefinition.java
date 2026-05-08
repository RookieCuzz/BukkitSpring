package top.skillstone.consumableskillbridge.model;

import org.bukkit.Material;
import top.skillstone.consumableskillbridge.ConsumeBehavior;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConsumableDefinition {
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final Integer customModelData;
    private final String itemModel;
    private final String craftEngineId;
    private final TriggerMode trigger;
    private final String skillId;
    private final Map<String, Double> skillParameters;
    private final ConsumeBehavior consumeBehavior;
    private final int maxConsume;
    private final double cooldownSeconds;
    private final String cooldownKey;
    private final boolean vanillaEating;
    private final boolean disableRightClickConsume;
    private final UseOnItemSpec useOnItem;
    private final boolean inedible;
    private final double restoreHealth;
    private final int restoreFood;
    private final float restoreSaturation;
    private final double restoreMana;
    private final double restoreStamina;
    private final RequirementSpec requirements;
    private final List<ConsumableEffectSpec> effects;
    private final List<ConsumableCommandSpec> commands;
    private final List<ConsumableSoundSpec> consumeSounds;

    public ConsumableDefinition(
            String id,
            Material material,
            String displayName,
            List<String> lore,
            Integer customModelData,
            String itemModel,
            String craftEngineId,
            TriggerMode trigger,
            String skillId,
            Map<String, Double> skillParameters,
            ConsumeBehavior consumeBehavior,
            int maxConsume,
            double cooldownSeconds,
            String cooldownKey,
            boolean vanillaEating,
            boolean disableRightClickConsume,
            UseOnItemSpec useOnItem,
            boolean inedible,
            double restoreHealth,
            int restoreFood,
            float restoreSaturation,
            double restoreMana,
            double restoreStamina,
            RequirementSpec requirements,
            List<ConsumableEffectSpec> effects,
            List<ConsumableCommandSpec> commands,
            List<ConsumableSoundSpec> consumeSounds
    ) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
        this.itemModel = itemModel == null ? "" : itemModel.trim();
        this.craftEngineId = craftEngineId == null ? "" : craftEngineId.trim().toLowerCase(Locale.ROOT);
        this.trigger = trigger;
        this.skillId = skillId == null ? "" : skillId;
        this.skillParameters = sanitizeSkillParameters(skillParameters);
        this.consumeBehavior = consumeBehavior;
        this.maxConsume = Math.max(1, maxConsume);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.cooldownKey = cooldownKey == null || cooldownKey.trim().isEmpty() ? "consumable:" + id.toLowerCase() : cooldownKey;
        this.vanillaEating = vanillaEating;
        this.disableRightClickConsume = disableRightClickConsume;
        this.useOnItem = useOnItem == null ? UseOnItemSpec.disabled() : useOnItem;
        this.inedible = inedible;
        this.restoreHealth = Math.max(0D, restoreHealth);
        this.restoreFood = Math.max(0, restoreFood);
        this.restoreSaturation = Math.max(0F, restoreSaturation);
        this.restoreMana = Math.max(0D, restoreMana);
        this.restoreStamina = Math.max(0D, restoreStamina);
        this.requirements = requirements;
        this.effects = effects;
        this.commands = commands;
        this.consumeSounds = consumeSounds;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public String getItemModel() {
        return itemModel;
    }

    public String getCraftEngineId() {
        return craftEngineId;
    }

    public TriggerMode getTrigger() {
        return trigger;
    }

    public String getSkillId() {
        return skillId;
    }

    public Map<String, Double> getSkillParameters() {
        return skillParameters;
    }

    public ConsumeBehavior getConsumeBehavior() {
        return consumeBehavior;
    }

    public int getMaxConsume() {
        return maxConsume;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public String getCooldownKey() {
        return cooldownKey;
    }

    public boolean isVanillaEating() {
        return vanillaEating;
    }

    public boolean isDisableRightClickConsume() {
        return disableRightClickConsume;
    }

    public UseOnItemSpec getUseOnItem() {
        return useOnItem;
    }

    public boolean isInedible() {
        return inedible;
    }

    public double getRestoreHealth() {
        return restoreHealth;
    }

    public int getRestoreFood() {
        return restoreFood;
    }

    public float getRestoreSaturation() {
        return restoreSaturation;
    }

    public double getRestoreMana() {
        return restoreMana;
    }

    public double getRestoreStamina() {
        return restoreStamina;
    }

    public RequirementSpec getRequirements() {
        return requirements;
    }

    public List<ConsumableEffectSpec> getEffects() {
        return effects;
    }

    public List<ConsumableCommandSpec> getCommands() {
        return commands;
    }

    public List<ConsumableSoundSpec> getConsumeSounds() {
        return consumeSounds;
    }

    private Map<String, Double> sanitizeSkillParameters(Map<String, Double> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : input.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT);
            Double value = entry.getValue();
            if (key.isEmpty() || value == null) {
                continue;
            }
            out.put(key, value);
        }
        return out.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(out);
    }
}
