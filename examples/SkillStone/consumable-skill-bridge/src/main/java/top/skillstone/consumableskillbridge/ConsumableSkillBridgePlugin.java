package top.skillstone.consumableskillbridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.skillstone.consumableskillbridge.command.BridgeCommand;
import top.skillstone.consumableskillbridge.engine.CommandEngine;
import top.skillstone.consumableskillbridge.engine.ConsumeEngine;
import top.skillstone.consumableskillbridge.engine.EffectEngine;
import top.skillstone.consumableskillbridge.engine.RequirementEngine;
import top.skillstone.consumableskillbridge.engine.SkillEngine;
import top.skillstone.consumableskillbridge.engine.SoundEngine;
import top.skillstone.consumableskillbridge.factory.ItemFactory;
import top.skillstone.consumableskillbridge.integration.ItemVisualBridge;
import top.skillstone.consumableskillbridge.integration.NoopItemVisualBridge;
import top.skillstone.consumableskillbridge.integration.NoopResourceRestoreBridge;
import top.skillstone.consumableskillbridge.integration.ResourceRestoreBridge;
import top.skillstone.consumableskillbridge.integration.craftengine.CraftEngineItemVisualBridge;
import top.skillstone.consumableskillbridge.integration.mmocore.MMOCoreResourceRestoreBridge;
import top.skillstone.consumableskillbridge.listener.ConsumableUseListener;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.registry.ItemDefinitionRegistry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public class ConsumableSkillBridgePlugin extends JavaPlugin {
    private boolean bridgeEnabled;
    private boolean debug;
    private boolean disableConsumableBlockClicks;
    private boolean mmocoreIntegrationEnabled;
    private boolean commandWhitelistEnabled;
    private Set<String> commandWhitelist = Collections.emptySet();
    private ItemDefinitionRegistry definitionRegistry;
    private ItemFactory itemFactory;
    private ConsumeEngine consumeEngine;
    private ItemVisualBridge itemVisualBridge = NoopItemVisualBridge.INSTANCE;
    private ResourceRestoreBridge resourceRestoreBridge = NoopResourceRestoreBridge.INSTANCE;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        definitionRegistry = new ItemDefinitionRegistry(this);
        itemVisualBridge = createItemVisualBridge();
        itemFactory = new ItemFactory(this, definitionRegistry);

        RequirementEngine requirementEngine = new RequirementEngine(this);
        SkillEngine skillEngine = new SkillEngine(this);
        EffectEngine effectEngine = new EffectEngine(this);
        CommandEngine commandEngine = new CommandEngine(this);
        SoundEngine soundEngine = new SoundEngine();
        consumeEngine = new ConsumeEngine(this, itemFactory, requirementEngine, skillEngine, effectEngine, commandEngine, soundEngine);

        reloadBridgeConfig();

        getServer().getPluginManager().registerEvents(new ConsumableUseListener(this, itemFactory, consumeEngine), this);

        BridgeCommand command = new BridgeCommand(this);
        if (getCommand("csbridge") != null) {
            getCommand("csbridge").setExecutor(command);
            getCommand("csbridge").setTabCompleter(command);
        } else {
            getLogger().warning("Command 'csbridge' is missing in plugin.yml.");
        }
    }

    public void reloadBridgeConfig() {
        reloadConfig();

        bridgeEnabled = getConfig().getBoolean("settings.enabled", true);
        debug = getConfig().getBoolean("settings.debug", false);
        disableConsumableBlockClicks = getConfig().getBoolean(
                "settings.consumables.disable-clicks-on-blocks",
                getConfig().getBoolean("consumables.disable_clicks_on_blocks", false)
        );
        mmocoreIntegrationEnabled = getConfig().getBoolean("settings.mmocore.enabled", true);
        commandWhitelistEnabled = getConfig().getBoolean("settings.command-whitelist.enabled", true);
        commandWhitelist = loadCommandWhitelist(getConfig().getStringList("settings.command-whitelist.labels"));
        itemVisualBridge = createItemVisualBridge();
        if (definitionRegistry != null) {
            definitionRegistry.reload(getConfig().getConfigurationSection("mappings"));
        }
        resourceRestoreBridge = createResourceRestoreBridge();

        if (commandWhitelistEnabled && commandWhitelist.isEmpty()) {
            warn("Command whitelist is enabled but empty; all consumable commands will be blocked.");
        }

        warnIfResourceRestoreConfiguredWithoutProvider();

        getLogger().info("Loaded " + getDefinitionCount() + " consumable definition(s).");
    }

    public boolean isBridgeEnabled() {
        return bridgeEnabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isDisableConsumableBlockClicks() {
        return disableConsumableBlockClicks;
    }

    public boolean isMmocoreIntegrationEnabled() {
        return mmocoreIntegrationEnabled;
    }

    public ResourceRestoreBridge getResourceRestoreBridge() {
        return resourceRestoreBridge;
    }

    public ItemVisualBridge getItemVisualBridge() {
        return itemVisualBridge;
    }

    public String getResourceRestoreProviderName() {
        return resourceRestoreBridge.getProviderName();
    }

    public boolean isResourceRestoreAvailable() {
        return resourceRestoreBridge.isAvailable();
    }

    public String getItemVisualProviderName() {
        return itemVisualBridge.getProviderName();
    }

    public boolean isItemVisualAvailable() {
        return itemVisualBridge.isAvailable();
    }

    public boolean isCommandAllowed(String commandLine) {
        if (!commandWhitelistEnabled) {
            return true;
        }

        String label = normalizeCommandLabel(commandLine);
        if (label.isEmpty()) {
            return false;
        }
        if (commandWhitelist.contains(label)) {
            return true;
        }

        int index = label.indexOf(':');
        if (index > -1 && index < label.length() - 1) {
            String plain = label.substring(index + 1);
            return commandWhitelist.contains(plain);
        }

        return false;
    }

    public boolean isCommandWhitelistEnabled() {
        return commandWhitelistEnabled;
    }

    public int getCommandWhitelistSize() {
        return commandWhitelist.size();
    }

    public ItemDefinitionRegistry getDefinitionRegistry() {
        return definitionRegistry;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public int getDefinitionCount() {
        return definitionRegistry == null ? 0 : definitionRegistry.size();
    }

    public void debug(String message) {
        if (debug) {
            getLogger().log(Level.INFO, "[DEBUG] {0}", message);
        }
    }

    public void warn(String message) {
        getLogger().warning(message);
    }

    private ResourceRestoreBridge createResourceRestoreBridge() {
        if (!mmocoreIntegrationEnabled) {
            return NoopResourceRestoreBridge.INSTANCE;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            return NoopResourceRestoreBridge.INSTANCE;
        }

        try {
            ResourceRestoreBridge bridge = new MMOCoreResourceRestoreBridge();
            getLogger().info("Resource restore provider: " + bridge.getProviderName());
            return bridge;
        } catch (NoClassDefFoundError ex) {
            warn("MMOCore integration requested but API classes are unavailable: " + ex.getMessage());
            return NoopResourceRestoreBridge.INSTANCE;
        } catch (Throwable ex) {
            warn("Failed to initialize MMOCore integration: " + ex.getMessage());
            return NoopResourceRestoreBridge.INSTANCE;
        }
    }

    private ItemVisualBridge createItemVisualBridge() {
        if (!Bukkit.getPluginManager().isPluginEnabled("CraftEngine")) {
            return NoopItemVisualBridge.INSTANCE;
        }

        try {
            ItemVisualBridge bridge = new CraftEngineItemVisualBridge(this);
            getLogger().info("Item visual provider: " + bridge.getProviderName());
            return bridge;
        } catch (NoClassDefFoundError ex) {
            warn("CraftEngine detected but API classes are unavailable: " + ex.getMessage());
            return NoopItemVisualBridge.INSTANCE;
        } catch (Throwable ex) {
            warn("Failed to initialize CraftEngine item visual bridge: " + ex.getMessage());
            return NoopItemVisualBridge.INSTANCE;
        }
    }

    private void warnIfResourceRestoreConfiguredWithoutProvider() {
        if (resourceRestoreBridge.isAvailable() || definitionRegistry == null) {
            return;
        }

        for (ConsumableDefinition definition : definitionRegistry.getDefinitions()) {
            if (definition.getRestoreMana() > 0D || definition.getRestoreStamina() > 0D) {
                warn("Consumable '" + definition.getId() + "' uses restore-mana/restore-stamina but no resource provider is active. "
                        + "Install+enable MMOCore and set settings.mmocore.enabled=true.");
                return;
            }
        }
    }

    private Set<String> loadCommandWhitelist(List<String> raw) {
        Set<String> normalized = new HashSet<>();
        for (String line : raw) {
            String label = normalizeCommandLabel(line);
            if (!label.isEmpty()) {
                normalized.add(label);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private String normalizeCommandLabel(String line) {
        if (line == null) {
            return "";
        }

        String normalized = line.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int space = normalized.indexOf(' ');
        if (space >= 0) {
            normalized = normalized.substring(0, space);
        }

        return normalized.trim();
    }
}
