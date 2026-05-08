package top.skillstone.consumableskillbridge.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BridgeCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "consumableskillbridge.admin";
    private static final List<String> ROOT_SUB_COMMANDS = List.of("reload", "list", "give", "doctor", "inspect");

    private final ConsumableSkillBridgePlugin plugin;

    public BridgeCommand(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/csbridge reload");
            sender.sendMessage(ChatColor.YELLOW + "/csbridge list");
            sender.sendMessage(ChatColor.YELLOW + "/csbridge give <player> <item_id> [amount]");
            sender.sendMessage(ChatColor.YELLOW + "/csbridge doctor");
            sender.sendMessage(ChatColor.YELLOW + "/csbridge inspect [player] [main|off]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            plugin.reloadBridgeConfig();
            sender.sendMessage(ChatColor.GREEN + "ConsumableSkillBridge reloaded. "
                    + ChatColor.YELLOW + "Definitions: " + plugin.getDefinitionCount());
            return true;
        }

        if ("list".equals(sub)) {
            List<String> ids = plugin.getDefinitionRegistry().getDefinitionIds();
            if (ids.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No consumable definitions loaded.");
                return true;
            }

            sender.sendMessage(ChatColor.GREEN + "Loaded definitions (" + ids.size() + "): "
                    + ChatColor.YELLOW + String.join(", ", ids));
            return true;
        }

        if ("give".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /csbridge give <player> <item_id> [amount]");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[3]));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                    return true;
                }
            }

            ItemStack created = plugin.getItemFactory().create(args[2], amount);
            if (created == null) {
                sender.sendMessage(ChatColor.RED + "Unknown item id: " + args[2]);
                return true;
            }

            Map<Integer, ItemStack> leftover = target.getInventory().addItem(created);
            for (ItemStack item : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), item);
            }

            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + args[2].toUpperCase(Locale.ROOT) + " to " + target.getName() + ".");
            return true;
        }

        if ("doctor".equals(sub)) {
            return handleDoctor(sender);
        }

        if ("inspect".equals(sub)) {
            return handleInspect(sender, args);
        }

        sender.sendMessage(ChatColor.RED + "Unknown sub-command. Use /csbridge reload|list|give|doctor|inspect");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(ROOT_SUB_COMMANDS, args[0]);
        }
        if (args.length == 2 && ("give".equalsIgnoreCase(args[0]) || "inspect".equalsIgnoreCase(args[0]))) {
            List<String> names = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            if ("inspect".equalsIgnoreCase(args[0])) {
                names = new ArrayList<>(names);
                names.add("main");
                names.add("off");
            }
            return filterStartsWith(names, args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return filterStartsWith(plugin.getDefinitionRegistry().getDefinitionIds(), args[2]);
        }
        if (args.length == 3 && "inspect".equalsIgnoreCase(args[0])) {
            return filterStartsWith(List.of("main", "off"), args[2]);
        }
        return List.of();
    }

    private boolean handleDoctor(CommandSender sender) {
        Plugin mythicLib = Bukkit.getPluginManager().getPlugin("MythicLib");
        Plugin mmoItems = Bukkit.getPluginManager().getPlugin("MMOItems");
        Plugin mmocore = Bukkit.getPluginManager().getPlugin("MMOCore");
        Plugin craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine");

        boolean mythicLibOk = mythicLib != null && mythicLib.isEnabled();
        boolean mmoItemsRemoved = mmoItems == null || !mmoItems.isEnabled();
        boolean mmocoreLoaded = mmocore != null && mmocore.isEnabled();
        boolean craftEngineLoaded = craftEngine != null && craftEngine.isEnabled();
        boolean hasDefinitions = plugin.getDefinitionCount() > 0;

        sender.sendMessage(ChatColor.GOLD + "[CSBridge Doctor]");
        sender.sendMessage(ChatColor.YELLOW + "Bridge enabled: " + ChatColor.WHITE + plugin.isBridgeEnabled());
        sender.sendMessage(ChatColor.YELLOW + "Definitions loaded: " + ChatColor.WHITE + plugin.getDefinitionCount());
        sender.sendMessage(ChatColor.YELLOW + "Command whitelist: " + ChatColor.WHITE
                + (plugin.isCommandWhitelistEnabled() ? "enabled (" + plugin.getCommandWhitelistSize() + ")" : "disabled"));
        sender.sendMessage(ChatColor.YELLOW + "MythicLib: " + ChatColor.WHITE
                + (mythicLibOk ? "OK" : "MISSING/DISABLED"));
        sender.sendMessage(ChatColor.YELLOW + "MMOItems: " + ChatColor.WHITE
                + (mmoItemsRemoved ? "NOT LOADED" : "LOADED"));
        sender.sendMessage(ChatColor.YELLOW + "MMOCore plugin: " + ChatColor.WHITE
                + (mmocoreLoaded ? "LOADED" : "NOT LOADED"));
        sender.sendMessage(ChatColor.YELLOW + "CraftEngine plugin: " + ChatColor.WHITE
                + (craftEngineLoaded ? "LOADED" : "NOT LOADED"));
        sender.sendMessage(ChatColor.YELLOW + "Resource restore bridge: " + ChatColor.WHITE
                + plugin.getResourceRestoreProviderName()
                + ChatColor.GRAY + " (enabled=" + plugin.isMmocoreIntegrationEnabled()
                + ", available=" + plugin.isResourceRestoreAvailable() + ")");
        sender.sendMessage(ChatColor.YELLOW + "Item visual bridge: " + ChatColor.WHITE
                + plugin.getItemVisualProviderName()
                + ChatColor.GRAY + " (available=" + plugin.isItemVisualAvailable() + ")");

        boolean pass = mythicLibOk && mmoItemsRemoved && hasDefinitions;
        sender.sendMessage(pass
                ? ChatColor.GREEN + "Doctor result: PASS"
                : ChatColor.RED + "Doctor result: FAIL");
        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        Player target;
        String handArg = null;

        if (args.length >= 2) {
            String arg = args[1];
            if (isHandArgument(arg) && sender instanceof Player) {
                target = (Player) sender;
                handArg = arg;
            } else {
                target = Bukkit.getPlayerExact(arg);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + arg);
                    return true;
                }
                if (args.length >= 3) {
                    handArg = args[2];
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /csbridge inspect <player> [main|off]");
                return true;
            }
            target = (Player) sender;
        }

        EquipmentSlot hand = EquipmentSlot.HAND;
        if (handArg != null) {
            String handRaw = handArg.toLowerCase(Locale.ROOT);
            if ("off".equals(handRaw) || "offhand".equals(handRaw)) {
                hand = EquipmentSlot.OFF_HAND;
            } else if (!"main".equals(handRaw) && !"hand".equals(handRaw) && !"mainhand".equals(handRaw)) {
                sender.sendMessage(ChatColor.RED + "Unknown hand: " + handArg + " (use main|off)");
                return true;
            }
        }

        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? target.getInventory().getItemInOffHand()
                : target.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.YELLOW + "Target hand is empty.");
            return true;
        }

        String itemId = plugin.getItemFactory().readItemId(item);
        int stateSchema = plugin.getItemFactory().readStateSchema(item);
        long cooldownUntil = plugin.getItemFactory().readCooldownUntil(item);
        double remainingSeconds = Math.max(0D, (cooldownUntil - System.currentTimeMillis()) / 1000D);

        sender.sendMessage(ChatColor.GOLD + "[CSBridge Inspect] " + ChatColor.YELLOW + target.getName() + " " + hand.name());
        sender.sendMessage(ChatColor.YELLOW + "Material: " + ChatColor.WHITE + item.getType().name() + " x" + item.getAmount());
        sender.sendMessage(ChatColor.YELLOW + "state_schema: " + ChatColor.WHITE + stateSchema);
        sender.sendMessage(ChatColor.YELLOW + "item_id: " + ChatColor.WHITE + (itemId == null ? "<none>" : itemId));
        sender.sendMessage(ChatColor.YELLOW + "cooldown_until: " + ChatColor.WHITE + cooldownUntil
                + ChatColor.GRAY + " (" + String.format(Locale.ROOT, "%.2fs", remainingSeconds) + ")");

        ConsumableDefinition definition = plugin.getItemFactory().resolveDefinition(item);
        if (definition == null) {
            sender.sendMessage(ChatColor.RED + "This item is not a loaded CSBridge consumable.");
            return true;
        }

        int usesLeft = plugin.getItemFactory().readUsesLeft(item, definition);
        int maxConsume = plugin.getItemFactory().readMaxConsume(item, definition);
        String cooldownKey = plugin.getItemFactory().readCooldownKey(item, definition);

        sender.sendMessage(ChatColor.YELLOW + "resolved_id: " + ChatColor.WHITE + definition.getId());
        sender.sendMessage(ChatColor.YELLOW + "craftengine_id: " + ChatColor.WHITE
                + (definition.getCraftEngineId().isEmpty() ? "<none>" : definition.getCraftEngineId()));
        sender.sendMessage(ChatColor.YELLOW + "uses_left/max: " + ChatColor.WHITE + usesLeft + "/" + maxConsume);
        sender.sendMessage(ChatColor.YELLOW + "cooldown_key: " + ChatColor.WHITE + cooldownKey);
        return true;
    }

    private boolean isHandArgument(String raw) {
        if (raw == null) {
            return false;
        }
        String handRaw = raw.toLowerCase(Locale.ROOT);
        return "main".equals(handRaw)
                || "hand".equals(handRaw)
                || "mainhand".equals(handRaw)
                || "off".equals(handRaw)
                || "offhand".equals(handRaw);
    }

    private List<String> filterStartsWith(List<String> source, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : source) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(candidate);
            }
        }
        return out;
    }
}
