# Scope Freeze (No MMOItems)

## Goal
Build a standalone consumable system in this plugin and remove MMOItems runtime dependency.

## In Scope
- Replicate `CONSUMABLE` abilities only.
- Right-click and vanilla-eating triggers.
- Skill casting (MythicLib handlers).
- Consume decision flow.
- `max-consume` use count handling.
- Item cooldown handling.
- Potion/effect application.
- Command execution.
- Consume sounds.
- Basic requirement checks (level/class/permission/custom-flag hooks as defined in this plugin scope).

## Out of Scope
- Equipment stat system.
- Crafting/reforging/upgrading systems.
- Gem/socket system.
- Skin system.
- Template random modifiers/roll engine.
- Full MMOItems template compatibility.

## Non-Goals
- Binary compatibility with MMOItems APIs.
- Preserving MMOItems internal data model.

## Acceptance Criteria
- Plugin starts and runs without MMOItems installed.
- No `net.Indyuce.*` imports in source.
- No MMOItems dependency in `pom.xml` and `plugin.yml`.
- Consumable flow is complete for in-scope features:
  - trigger -> requirement check -> consume decision -> effects/skill/commands/sounds -> cooldown
- Existing in-scope features are covered by integration tests/manual checklist.

## Constraints
- No reflection-based MMOItems bridge.
- No runtime fallback to MMOItems APIs.
- New plugin owns consumable config schema and runtime data.

## Migration Principle
- Provide one-way migration from old config/items to new schema.
- Migration tool is optional/offline and must not introduce runtime MMOItems dependency.
