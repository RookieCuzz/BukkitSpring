# Execution Plan (Based on Scope Freeze)

## Phase 1 - Dependency Removal Baseline
- Remove MMOItems dependency from `pom.xml`.
- Remove MMOItems `depend` entry from `plugin.yml`.
- Delete MMOItems listener/import command paths.
- Keep plugin compiling with Spigot/Paper + MythicLib only.
- Exit criteria: project builds and enables without MMOItems.

## Phase 2 - Core Consumable Model
- Add internal data model:
  - consumable definition
  - trigger type
  - consume policy
  - use-count policy (`max-consume`)
  - cooldown and requirement sections
- Add config loader and validation.
- Exit criteria: definitions load with strict validation and clear error logs.

## Phase 3 - Runtime Pipeline
- Implement trigger handling (right-click + vanilla consume event).
- Implement requirement checks and consume decision.
- Implement skill cast pipeline (MythicLib).
- Implement effects, commands, sounds, cooldown.
- Exit criteria: end-to-end consumable lifecycle works without MMOItems.

## Phase 4 - Item State and Persistence
- Store item id / remaining uses / cooldown key in PDC.
- Handle stack split behavior for partial consume.
- Ensure reload-safe behavior.
- Exit criteria: use counts and cooldown behavior are stable across relog/restart.

## Phase 5 - Migration Tooling
- Add offline/command migration for old consumable config to new schema.
- Add optional inventory rewrite utility for existing player items.
- Exit criteria: migration can be executed without MMOItems runtime coupling.

## Phase 6 - Verification and Release
- Build regression checklist for in-scope features.
- Add smoke/integration tests where feasible.
- Write rollout guide (gray release -> cutover -> rollback).
- Exit criteria: production-ready release package + operation docs.

## Immediate Next Sprint
- Execute Phase 1 fully.
- Deliver first standalone runtime skeleton from Phase 2.
