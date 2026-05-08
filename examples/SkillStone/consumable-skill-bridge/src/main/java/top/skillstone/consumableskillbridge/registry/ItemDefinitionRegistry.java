package top.skillstone.consumableskillbridge.registry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;
import top.skillstone.consumableskillbridge.ConsumeBehavior;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.model.ConsumableCommandSpec;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.model.ConsumableEffectSpec;
import top.skillstone.consumableskillbridge.model.ConsumableSoundSpec;
import top.skillstone.consumableskillbridge.model.RequirementSpec;
import top.skillstone.consumableskillbridge.model.TriggerMode;
import top.skillstone.consumableskillbridge.model.UseOnItemSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ItemDefinitionRegistry {
    private final ConsumableSkillBridgePlugin plugin;
    private final Map<String, ConsumableDefinition> definitions = new LinkedHashMap<>();

    public ItemDefinitionRegistry(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        reload(null);
    }

    public void reload(ConfigurationSection legacyMappings) {
        definitions.clear();

        File folder = new File(plugin.getDataFolder(), "items/consumable");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Cannot create consumable definition folder: " + folder.getAbsolutePath());
            return;
        }

        ensureSampleFiles(folder);

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                loadFile(file);
            }
        }

        loadLegacyMappings(legacyMappings);
    }

    public ConsumableDefinition getDefinition(String id) {
        if (id == null) {
            return null;
        }
        return definitions.get(normalizeId(id));
    }

    public Collection<ConsumableDefinition> getDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<String> getDefinitionIds() {
        return new ArrayList<>(definitions.keySet());
    }

    public int size() {
        return definitions.size();
    }

    private void ensureSampleFiles(File folder) {
        ensureBundledFile(folder, "example_scroll.yml");
        ensureBundledFile(folder, "mythic_skillstones.yml");
        ensureBundledFile(folder, "mythicmobs_skillstones.yml");
        ensureBundledFile(folder, "mythicmobs_complex_skillstones.yml");
    }

    private void ensureBundledFile(File folder, String fileName) {
        File sample = new File(folder, fileName);
        if (sample.exists()) {
            return;
        }

        try {
            plugin.saveResource("items/consumable/" + fileName, false);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Missing bundled consumable file: " + fileName, ex);
        }
    }

    private void loadFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection defaults = config.getConfigurationSection("defaults");
        ConfigurationSection schemaConsumables = config.getConfigurationSection("consumables");
        String source = file.getName();

        if (schemaConsumables != null) {
            for (String key : schemaConsumables.getKeys(false)) {
                ConfigurationSection section = schemaConsumables.getConfigurationSection(key);
                if (section == null) {
                    plugin.warn("Skipping invalid consumable root '" + key + "' in " + source + " under 'consumables'.");
                    continue;
                }

                ConsumableDefinition definition = parseDefinition(key, section, defaults, source);
                if (definition != null) {
                    registerDefinition(definition, source, true);
                }
            }
            return;
        }

        for (String key : config.getKeys(false)) {
            if (isReservedRootKey(key)) {
                continue;
            }
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                plugin.warn("Skipping invalid consumable root '" + key + "' in " + source + ".");
                continue;
            }

            ConsumableDefinition definition = parseDefinition(key, section, defaults, source);
            if (definition != null) {
                registerDefinition(definition, source, true);
            }
        }
    }

    private void loadLegacyMappings(ConfigurationSection mappings) {
        if (mappings == null) {
            return;
        }

        ConfigurationSection defaults = mappings.getConfigurationSection("defaults");
        ConfigurationSection root = mappings.getConfigurationSection("consumables");
        if (root == null) {
            root = mappings;
        }

        for (String key : root.getKeys(false)) {
            if (isReservedMappingKey(key) && root == mappings) {
                continue;
            }

            ConsumableDefinition parsed = null;
            if (root.isString(key)) {
                String skillId = root.getString(key, "").trim();
                if (skillId.isEmpty()) {
                    plugin.warn("Skipping legacy mapping '" + key + "': empty skill id.");
                    continue;
                }

                YamlConfiguration synthetic = new YamlConfiguration();
                synthetic.set("skill.id", skillId);
                parsed = parseDefinition(key, synthetic, defaults, "config.yml:mappings");
            } else {
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    plugin.warn("Skipping invalid mapping root '" + key + "' in config.yml.");
                    continue;
                }
                parsed = parseDefinition(key, section, defaults, "config.yml:mappings");
            }

            if (parsed != null) {
                registerDefinition(parsed, "config.yml:mappings", false);
            }
        }
    }

    private ConsumableDefinition parseDefinition(String rawId, ConfigurationSection section, ConfigurationSection defaults, String source) {
        ConfigurationSection merged = mergeSections(defaults, section);
        String id = normalizeId(rawId);

        String materialName = readString(merged, "PAPER", "display.material", "material", "type");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem()) {
            plugin.warn("Consumable '" + id + "' (" + source + ") has invalid material '" + materialName + "'. Falling back to PAPER.");
            material = Material.PAPER;
        }

        String displayName = color(readString(merged, id, "display.name", "display-name", "name"));
        List<String> lore = colorList(readStringList(merged, "display.lore", "lore"));

        Integer customModelData = null;
        if (containsAny(merged, "display.custom-model-data", "custom-model-data", "custom_model_data")) {
            customModelData = readInt(merged, 0, "display.custom-model-data", "custom-model-data", "custom_model_data");
        }
        String itemModel = readString(merged, "", "display.item-model", "item-model", "item_model").trim();
        if (!itemModel.isEmpty() && NamespacedKey.fromString(itemModel) == null) {
            plugin.warn("Consumable '" + id + "' (" + source + ") has invalid item-model '" + itemModel + "'. Ignoring it.");
            itemModel = "";
        }
        String craftEngineId = readString(
                merged,
                "",
                "display.craftengine-id",
                "display.craftengine_id",
                "display.craftengineId",
                "craftengine-id",
                "craftengine_id",
                "craftengineId",
                "craft-engine-id",
                "ce-id",
                "ce_id"
        ).trim();

        TriggerMode trigger = TriggerMode.from(readString(merged, "RIGHT_CLICK", "trigger", "on-trigger", "use-trigger"));
        String skillId = readString(merged, "", "skill.id", "skill", "skill-id", "ability", "ability.id").trim();
        if (skillId.isEmpty()) {
            skillId = readString(merged, "", "cast-skill", "cast").trim();
        }
        Map<String, Double> skillParameters = parseSkillParameters(merged);

        ConsumeBehavior consumeBehavior = readConsumeBehavior(merged);
        int maxConsume = Math.max(1, readInt(merged, 1, "max-consume", "max_consume", "maxConsume", "uses"));
        double cooldownSeconds = Math.max(0D, readDouble(merged, 0D, "cooldown", "cooldown-seconds", "cooldown_seconds", "cd"));
        String cooldownKey = readString(merged, "consumable:" + id.toLowerCase(Locale.ROOT), "cooldown-key", "cooldown_key", "cd-key");
        boolean vanillaEating = readBoolean(merged, false, "vanilla-eating", "vanilla_eating", "vanillaEating");
        boolean disableRightClickConsume = readBoolean(
                merged,
                false,
                "disable-right-click-consume",
                "disable_right_click_consume",
                "disableRightClickConsume"
        );
        UseOnItemSpec useOnItem = parseUseOnItem(merged, trigger);
        boolean inedible = readBoolean(merged, false, "inedible");
        double restoreHealth = Math.max(0D, readDouble(merged, 0D, "restore-health", "restore_health", "restore.health", "restore-hp", "restore_hp"));
        int restoreFood = Math.max(0, readInt(merged, 0, "restore-food", "restore_food", "restore.food"));
        float restoreSaturation = (float) Math.max(0D, readDouble(merged, 0D, "restore-saturation", "restore_saturation", "restore.saturation"));
        double restoreMana = Math.max(0D, readDouble(merged, 0D, "restore-mana", "restore_mana", "restore.mana"));
        double restoreStamina = Math.max(0D, readDouble(merged, 0D, "restore-stamina", "restore_stamina", "restore.stamina"));

        RequirementSpec requirements = parseRequirements(merged);
        List<ConsumableEffectSpec> effects = parseEffects(merged);
        List<ConsumableCommandSpec> commands = parseCommands(merged);
        List<ConsumableSoundSpec> sounds = parseConsumeSounds(merged);

        return new ConsumableDefinition(
                id,
                material,
                displayName,
                lore,
                customModelData,
                itemModel,
                craftEngineId,
                trigger,
                skillId,
                skillParameters,
                consumeBehavior,
                maxConsume,
                cooldownSeconds,
                cooldownKey,
                vanillaEating,
                disableRightClickConsume,
                useOnItem,
                inedible,
                restoreHealth,
                restoreFood,
                restoreSaturation,
                restoreMana,
                restoreStamina,
                requirements,
                effects,
                commands,
                sounds
        );
    }

    private Map<String, Double> parseSkillParameters(ConfigurationSection root) {
        ConfigurationSection section = firstSection(
                root,
                "skill.parameters",
                "skill-parameters",
                "skill_parameters",
                "skill.modifiers",
                "skill-modifiers",
                "skill_modifiers",
                "parameters.skill",
                "skill-params",
                "skill_params"
        );
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, Double> out = new LinkedHashMap<>();
        for (String rawKey : section.getKeys(false)) {
            String key = rawKey == null ? "" : rawKey.trim();
            if (key.isEmpty()) {
                continue;
            }

            Object rawValue = section.get(rawKey);
            Double value = parseDoubleObject(rawValue);
            if (value == null) {
                plugin.warn("Invalid skill parameter value for '" + key + "' (expected number): " + rawValue);
                continue;
            }
            out.put(key, value);
        }
        return out.isEmpty() ? Collections.emptyMap() : out;
    }

    private UseOnItemSpec parseUseOnItem(ConfigurationSection root, TriggerMode trigger) {
        ConfigurationSection section = firstSection(root, "use-on-item", "use_on_item", "useOnItem");
        if (section == null) {
            return trigger == TriggerMode.ON_ITEM
                    ? new UseOnItemSpec(true, true, false, Collections.emptySet(), Collections.emptySet())
                    : UseOnItemSpec.disabled();
        }

        boolean enabled = readBoolean(section, trigger == TriggerMode.ON_ITEM, "enabled");
        boolean playerInventoryOnly = readBoolean(section, true, "player-inventory-only", "player_inventory_only", "playerInventoryOnly");
        boolean allowAirTarget = readBoolean(section, false, "allow-air-target", "allow_air_target", "allowAirTarget");
        Set<Material> allowed = parseMaterialSet(section, "allow-target-materials", "allow_target_materials", "allowed-materials", "allowed_materials");
        Set<Material> denied = parseMaterialSet(section, "deny-target-materials", "deny_target_materials", "denied-materials", "denied_materials");
        return new UseOnItemSpec(enabled, playerInventoryOnly, allowAirTarget, allowed, denied);
    }

    private RequirementSpec parseRequirements(ConfigurationSection root) {
        String permission = readString(root, "", "requirements.permission", "permission");
        int minLevel = readInt(root, 0, "requirements.min-level", "requirements.min_level", "min-level", "min_level");
        int minFood = readInt(root, 0, "requirements.min-food", "requirements.min_food", "min-food", "min_food");
        double minMana = Math.max(0D, readDouble(root, 0D, "requirements.min-mana", "requirements.min_mana", "min-mana", "min_mana"));
        double minStamina = Math.max(0D, readDouble(root, 0D, "requirements.min-stamina", "requirements.min_stamina", "min-stamina", "min_stamina"));
        double manaCost = Math.max(0D, readDouble(root, 0D, "mana-cost", "mana_cost", "cost.mana", "cost.mana-cost", "cost.mana_cost"));
        double staminaCost = Math.max(0D, readDouble(root, 0D, "stamina-cost", "stamina_cost", "cost.stamina", "cost.stamina-cost", "cost.stamina_cost"));
        return new RequirementSpec(permission, minLevel, minFood, minMana, minStamina, manaCost, staminaCost);
    }

    private List<ConsumableEffectSpec> parseEffects(ConfigurationSection root) {
        if (root == null || !root.contains("effects")) {
            return Collections.emptyList();
        }

        List<ConsumableEffectSpec> out = new ArrayList<>();
        if (root.isConfigurationSection("effects")) {
            ConfigurationSection section = root.getConfigurationSection("effects");
            if (section == null) {
                return out;
            }
            for (String key : section.getKeys(false)) {
                ConfigurationSection effectSection = section.getConfigurationSection(key);
                if (effectSection == null) {
                    if (section.isString(key)) {
                        addEffectSpec(out, key, section.getString(key), 5D, 0, false, true, true);
                    }
                    continue;
                }

                addEffectSpec(
                        out,
                        key,
                        readString(effectSection, key, "type"),
                        readDouble(effectSection, 5D, "duration"),
                        readInt(effectSection, 0, "amplifier"),
                        readBoolean(effectSection, false, "ambient"),
                        readBoolean(effectSection, true, "particles"),
                        readBoolean(effectSection, true, "icon"),
                        readInt(effectSection, -1, "duration-ticks", "duration_ticks")
                );
            }
            return out;
        }

        if (root.isList("effects")) {
            List<?> effects = root.getList("effects");
            if (effects == null) {
                return out;
            }
            int i = 0;
            for (Object raw : effects) {
                i++;
                if (raw instanceof String) {
                    addEffectSpec(out, "effects[" + i + "]", String.valueOf(raw), 5D, 0, false, true, true);
                    continue;
                }
                if (!(raw instanceof Map)) {
                    continue;
                }

                ConfigurationSection effectSection = mapToSection((Map<?, ?>) raw);
                addEffectSpec(
                        out,
                        "effects[" + i + "]",
                        readString(effectSection, "", "type"),
                        readDouble(effectSection, 5D, "duration"),
                        readInt(effectSection, 0, "amplifier"),
                        readBoolean(effectSection, false, "ambient"),
                        readBoolean(effectSection, true, "particles"),
                        readBoolean(effectSection, true, "icon"),
                        readInt(effectSection, -1, "duration-ticks", "duration_ticks")
                );
            }
        }
        return out;
    }

    private void addEffectSpec(List<ConsumableEffectSpec> out, String key, String typeRaw, double durationSeconds, int amplifier, boolean ambient, boolean particles, boolean icon) {
        addEffectSpec(out, key, typeRaw, durationSeconds, amplifier, ambient, particles, icon, -1);
    }

    private void addEffectSpec(List<ConsumableEffectSpec> out, String key, String typeRaw, double durationSeconds, int amplifier, boolean ambient, boolean particles, boolean icon, int durationTicksOverride) {
        if (typeRaw == null || typeRaw.trim().isEmpty()) {
            return;
        }

        PotionEffectType type = PotionEffectType.getByName(typeRaw.toUpperCase(Locale.ROOT));
        if (type == null) {
            plugin.warn("Unknown potion effect '" + typeRaw + "' in consumable effect '" + key + "'.");
            return;
        }

        int durationTicks = durationTicksOverride > 0
                ? durationTicksOverride
                : Math.max(1, (int) Math.round(Math.max(0D, durationSeconds) * 20D));

        out.add(new ConsumableEffectSpec(
                type,
                durationTicks,
                Math.max(0, amplifier),
                ambient,
                particles,
                icon
        ));
    }

    private List<ConsumableCommandSpec> parseCommands(ConfigurationSection root) {
        if (root == null || !root.contains("commands")) {
            return Collections.emptyList();
        }

        List<ConsumableCommandSpec> out = new ArrayList<>();
        if (root.isConfigurationSection("commands")) {
            ConfigurationSection section = root.getConfigurationSection("commands");
            if (section == null) {
                return out;
            }

            for (String key : section.getKeys(false)) {
                ConfigurationSection commandSection = section.getConfigurationSection(key);
                if (commandSection == null) {
                    if (section.isString(key)) {
                        out.add(new ConsumableCommandSpec(section.getString(key, ""), false, false, 1D, 0L));
                    }
                    continue;
                }
                addCommandSpec(out, commandSection);
            }
            return out;
        }

        if (root.isList("commands")) {
            List<?> commandList = root.getList("commands");
            if (commandList == null) {
                return out;
            }

            for (Object raw : commandList) {
                if (raw instanceof String) {
                    out.add(new ConsumableCommandSpec(String.valueOf(raw), false, false, 1D, 0L));
                    continue;
                }
                if (raw instanceof Map) {
                    addCommandSpec(out, mapToSection((Map<?, ?>) raw));
                }
            }
            return out;
        }

        if (root.isString("commands")) {
            out.add(new ConsumableCommandSpec(root.getString("commands", ""), false, false, 1D, 0L));
        }
        return out;
    }

    private void addCommandSpec(List<ConsumableCommandSpec> out, ConfigurationSection commandSection) {
        String format = readString(commandSection, "", "format", "command", "run");
        if (format.isEmpty()) {
            return;
        }

        boolean console = readBoolean(commandSection, false, "console", "as-console");
        boolean op = readBoolean(commandSection, false, "op", "as-op");
        double chance = Math.max(0D, readDouble(commandSection, 1D, "chance", "probability"));
        long delayTicks = Math.max(0L, readLong(commandSection, 0L, "delay-ticks", "delay_ticks", "delay"));
        out.add(new ConsumableCommandSpec(format, console, op, chance, delayTicks));
    }

    private List<ConsumableSoundSpec> parseConsumeSounds(ConfigurationSection root) {
        if (root == null) {
            return Collections.emptyList();
        }

        List<ConsumableSoundSpec> out = new ArrayList<>();
        if (root.contains("sounds.on-consume")) {
            parseSoundObject(out, root.get("sounds.on-consume"));
            return out;
        }

        if (root.contains("on-consume-sound")) {
            Sound sound = parseSound(root.getString("on-consume-sound"));
            if (sound != null) {
                out.add(new ConsumableSoundSpec(sound, 1f, 1f));
            }
            return out;
        }

        if (root.contains("sound")) {
            Sound sound = parseSound(root.getString("sound"));
            if (sound != null) {
                out.add(new ConsumableSoundSpec(sound, 1f, 1f));
            }
            return out;
        }

        return out;
    }

    private void parseSoundObject(List<ConsumableSoundSpec> out, Object object) {
        if (object == null) {
            return;
        }
        if (object instanceof String) {
            Sound sound = parseSound(String.valueOf(object));
            if (sound != null) {
                out.add(new ConsumableSoundSpec(sound, 1f, 1f));
            }
            return;
        }

        if (object instanceof ConfigurationSection) {
            parseSoundSection(out, (ConfigurationSection) object);
            return;
        }

        if (object instanceof Map) {
            parseSoundSection(out, mapToSection((Map<?, ?>) object));
            return;
        }

        if (object instanceof List) {
            for (Object entry : (List<?>) object) {
                parseSoundObject(out, entry);
            }
        }
    }

    private void parseSoundSection(List<ConsumableSoundSpec> out, ConfigurationSection section) {
        if (section.contains("sound")) {
            Sound sound = parseSound(section.getString("sound"));
            if (sound != null) {
                out.add(new ConsumableSoundSpec(
                        sound,
                        (float) readDouble(section, 1D, "volume"),
                        (float) readDouble(section, 1D, "pitch")
                ));
            }
        }

        for (String key : section.getKeys(false)) {
            if ("sound".equalsIgnoreCase(key) || "volume".equalsIgnoreCase(key) || "pitch".equalsIgnoreCase(key)) {
                continue;
            }

            Object value = section.get(key);
            if (value instanceof String) {
                Sound sound = parseSound(String.valueOf(value));
                if (sound != null) {
                    out.add(new ConsumableSoundSpec(sound, 1f, 1f));
                }
                continue;
            }

            if (value instanceof ConfigurationSection) {
                parseSoundSection(out, (ConfigurationSection) value);
                continue;
            }

            if (value instanceof Map) {
                parseSoundSection(out, mapToSection((Map<?, ?>) value));
            }
        }
    }

    private Sound parseSound(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plugin.warn("Unknown sound: " + raw);
            return null;
        }
    }

    private ConsumeBehavior readConsumeBehavior(ConfigurationSection section) {
        if (containsAny(section, "consume", "consume-behavior", "consume_behavior")) {
            String consumeRaw = readString(section, "DEFAULT", "consume", "consume-behavior", "consume_behavior");
            return ConsumeBehavior.from(consumeRaw);
        }

        if (containsAny(section, "consume-item", "consume_item")) {
            boolean consumeItem = readBoolean(section, true, "consume-item", "consume_item");
            return consumeItem ? ConsumeBehavior.ALWAYS : ConsumeBehavior.NEVER;
        }
        return ConsumeBehavior.DEFAULT;
    }

    private ConfigurationSection mergeSections(ConfigurationSection defaults, ConfigurationSection overrides) {
        YamlConfiguration merged = new YamlConfiguration();
        if (defaults != null) {
            copySection(defaults, merged, "");
        }
        if (overrides != null) {
            copySection(overrides, merged, "");
        }
        return merged;
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target, String prefix) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof ConfigurationSection) {
                copySection((ConfigurationSection) value, target, path);
                continue;
            }
            target.set(path, value);
        }
    }

    private ConfigurationSection mapToSection(Map<?, ?> map) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            configuration.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return configuration;
    }

    private void registerDefinition(ConsumableDefinition definition, String source, boolean override) {
        String id = definition.getId();
        if (!override && definitions.containsKey(id)) {
            plugin.debug("Skip legacy mapping '" + id + "' from " + source + " because file definition already exists.");
            return;
        }
        if (override && definitions.containsKey(id)) {
            plugin.warn("Duplicate consumable id '" + id + "' in " + source + ". Overwriting previous definition.");
        }
        definitions.put(id, definition);
    }

    private boolean isReservedRootKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return "schema-version".equals(lower) || "defaults".equals(lower) || "consumables".equals(lower);
    }

    private boolean isReservedMappingKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return "schema-version".equals(lower) || "defaults".equals(lower) || "consumables".equals(lower);
    }

    private boolean containsAny(ConfigurationSection section, String... paths) {
        for (String path : paths) {
            if (section.contains(path)) {
                return true;
            }
        }
        return false;
    }

    private String readString(ConfigurationSection section, String fallback, String... paths) {
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            Object value = section.get(path);
            if (value == null) {
                continue;
            }
            return String.valueOf(value);
        }
        return fallback;
    }

    private int readInt(ConfigurationSection section, int fallback, String... paths) {
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            Object value = section.get(path);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt(((String) value).trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private long readLong(ConfigurationSection section, long fallback, String... paths) {
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            Object value = section.get(path);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong(((String) value).trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private double readDouble(ConfigurationSection section, double fallback, String... paths) {
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            Object value = section.get(path);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble(((String) value).trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private Double parseDoubleObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean readBoolean(ConfigurationSection section, boolean fallback, String... paths) {
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            Object value = section.get(path);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof String) {
                String raw = ((String) value).trim().toLowerCase(Locale.ROOT);
                if ("true".equals(raw) || "yes".equals(raw) || "on".equals(raw)) {
                    return true;
                }
                if ("false".equals(raw) || "no".equals(raw) || "off".equals(raw)) {
                    return false;
                }
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
        }
        return fallback;
    }

    private List<String> readStringList(ConfigurationSection section, String... paths) {
        for (String path : paths) {
            if (!section.contains(path)) {
                continue;
            }
            if (section.isList(path)) {
                List<?> list = section.getList(path);
                if (list == null) {
                    continue;
                }
                List<String> out = new ArrayList<>(list.size());
                for (Object value : list) {
                    out.add(String.valueOf(value));
                }
                return out;
            }
            if (section.isString(path)) {
                return Collections.singletonList(section.getString(path, ""));
            }
        }
        return Collections.emptyList();
    }

    private ConfigurationSection firstSection(ConfigurationSection root, String... paths) {
        if (root == null) {
            return null;
        }

        for (String path : paths) {
            ConfigurationSection section = root.getConfigurationSection(path);
            if (section != null) {
                return section;
            }
        }
        return null;
    }

    private Set<Material> parseMaterialSet(ConfigurationSection section, String... paths) {
        List<String> raw = readStringList(section, paths);
        if (raw.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Material> out = new HashSet<>();
        for (String line : raw) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String normalized = line.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if ("ANY".equals(normalized) || "*".equals(normalized)) {
                return Collections.emptySet();
            }
            Material material = Material.matchMaterial(normalized);
            if (material == null || !material.isItem()) {
                plugin.warn("Unknown material in use-on-item target list: " + line);
                continue;
            }
            out.add(material);
        }
        return out.isEmpty() ? Collections.emptySet() : out;
    }

    private String normalizeId(String id) {
        return id == null
                ? ""
                : id.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private List<String> colorList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>(input.size());
        for (String line : input) {
            out.add(color(line));
        }
        return out;
    }
}

