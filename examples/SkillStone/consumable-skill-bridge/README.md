# ConsumableSkillBridge

Standalone consumable system based on Spigot/Paper + MythicLib.

## What it does
- Loads local consumable definitions from `items/consumable/*.yml`.
- Supports standardized item schema (`schema-version: 2`, `defaults`, `consumables`) and legacy `mappings` compatibility.
- Generates consumable items with PDC keys (`item_id`, `uses_left`, `max_consume`, `cooldown_key`, `cooldown_until`, `state_schema`).
- Handles right-click/eat trigger pipeline:
  - requirement check
  - cooldown check
  - pre/post consume events (`ConsumablePreUseEvent`, `ConsumablePostUseEvent`)
  - MythicLib skill cast
  - potion effects
  - commands
  - sounds
  - consume/use-count mutation (with stack split support)
- Supports extra consumable semantics:
  - `vanilla-eating` (consume on `PlayerItemConsumeEvent`)
  - `disable-right-click-consume` (apply effects without consuming amount)
  - `use-on-item` inventory chain (`SWAP_WITH_CURSOR`)
  - `inedible` (blocks direct consume while keeping item definition valid)
  - `restore-health`, `restore-food`, `restore-saturation`
  - `restore-mana`, `restore-stamina` (optional MMOCore integration)
  - resource checks/costs: `requirements.min-mana`, `requirements.min-stamina`, `mana-cost`, `stamina-cost`
- Optional right-click safety:
  - `settings.consumables.disable-clicks-on-blocks` (ignore consumable use on interactable blocks)
- Optional MMOCore bridge:
  - `settings.mmocore.enabled` (auto uses MMOCore when plugin is installed/enabled)
- Dynamic lore placeholders: `{uses_left}`, `{max_consume}`, `{cooldown_seconds}`.
- Enforces command whitelist (`settings.command-whitelist`) before dispatching consumable commands.

## Project Docs
- Scope freeze: `SCOPE_FREEZE.md`
- Execution plan: `PHASE_PLAN.md`

## Command
- `/csbridge reload` - reload plugin config.
- `/csbridge list` - list loaded consumable ids.
- `/csbridge give <player> <item_id> [amount]` - create and give consumable item.
- `/csbridge doctor` - runtime dependency and readiness check.
- `/csbridge inspect [player] [main|off]` - inspect consumable PDC/runtime state on hand item.

## Notes
- This baseline no longer depends on MMOItems.
- Runtime consumable state is PDC-only (no MMOItems NBT structure).
- Acceptance checklist: `TEST_ACCEPTANCE.md`
