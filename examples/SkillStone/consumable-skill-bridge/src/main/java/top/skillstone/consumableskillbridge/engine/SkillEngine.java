package top.skillstone.consumableskillbridge.engine;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.skill.ModifiableSkill;
import io.lumine.mythic.lib.skill.SimpleSkill;
import io.lumine.mythic.lib.skill.SkillMetadata;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import io.lumine.mythic.lib.skill.result.SkillResult;
import org.bukkit.entity.Player;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;

import java.util.Map;

public class SkillEngine {
    private final ConsumableSkillBridgePlugin plugin;

    public SkillEngine(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean cast(Player player, ConsumableDefinition definition) {
        String skillId = definition.getSkillId();
        if (skillId == null || skillId.trim().isEmpty()) {
            return true;
        }

        SkillHandler<?> handler = MythicLib.plugin.getSkills().getHandler(skillId);
        if (handler == null) {
            plugin.warn("Unknown skill handler '" + skillId + "' for consumable '" + definition.getId() + "'.");
            return false;
        }

        MMOPlayerData playerData = MMOPlayerData.get(player);
        if (playerData == null) {
            plugin.warn("Failed to fetch MythicLib player data for " + player.getName() + ".");
            return false;
        }

        SkillResult result = createCastSkill(handler, definition).cast(SkillMetadata.of(playerData));
        return result == null || result.isSuccessful();
    }

    private io.lumine.mythic.lib.skill.Skill createCastSkill(SkillHandler<?> handler, ConsumableDefinition definition) {
        Map<String, Double> overrides = definition.getSkillParameters();
        if (overrides == null || overrides.isEmpty()) {
            return new SimpleSkill(handler);
        }

        ModifiableSkill modifiableSkill = new ModifiableSkill(handler);

        // Keep MythicLib item defaults first, then apply per-stone overrides.
        for (String parameter : handler.getParameters()) {
            modifiableSkill.registerModifier(parameter, handler.getDefaultItemParameter(parameter));
        }
        for (Map.Entry<String, Double> entry : overrides.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty() || entry.getValue() == null) {
                continue;
            }
            modifiableSkill.registerModifier(entry.getKey(), entry.getValue());
        }

        return modifiableSkill;
    }
}
