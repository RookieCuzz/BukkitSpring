# Test And Acceptance Checklist

## 1. Preconditions
- Server: Paper/Spigot 1.21.x
- Plugins: `MythicLib` + `ConsumableSkillBridge`
- Must remove: `MMOItems` jar from `plugins/`
- Config:
  - `settings.command-whitelist` enabled in `config.yml`
  - At least 2 consumables in `items/consumable/*.yml`:
    - one `RIGHT_CLICK`
    - one `EAT`
- Quick checks:
  - `/csbridge doctor`
  - `/csbridge inspect <player> main`

## 2. Regression Cases

### R1 Cooldown
- Steps:
  1. Give a consumable with `cooldown: 5`.
  2. Use twice within 1 second.
- Expected:
  - First use succeeds.
  - Second use is blocked with cooldown message.
  - Use succeeds again after 5 seconds.

### R2 Consume Logic
- Steps:
  1. Set `max-consume: 3`, `consume: DEFAULT`.
  2. Use the same item 3 times.
- Expected:
  - First 2 uses decrease `uses_left`.
  - Third use consumes one stack amount (or clears hand when amount is 1).

### R3 Stack Split
- Steps:
  1. Give a stack (for example amount 16) with `max-consume: 3`.
  2. Use once.
- Expected:
  - Active unit updates `uses_left`.
  - Remaining stack units keep original state.
  - No dupe and no unexpected item loss.

### R4 Skill Parameter Chain
- Steps:
  1. Pick a MythicLib skill that has configured parameters (damage/range/delay).
  2. Trigger it through consumable.
- Expected:
  - Skill behaves according to MythicLib-side parameters.
  - No context errors caused by MMOItems removal.

### R5 Failure Rollback
- Steps:
  1. Create a consumable with invalid `skill.id`.
  2. Use item.
- Expected:
  - Cast fails and player sees failure message.
  - No consume count change.
  - No stack amount change.
  - No new cooldown write.

### R6 Command Whitelist
- Steps:
  1. Configure one allowed command (for example `tell`).
  2. Configure one blocked command (for example `op`).
  3. Use item.
- Expected:
  - Allowed command executes.
  - Blocked command is denied and warning is logged.

### R7 Optional MMOCore Resource Restore
- Steps:
  1. Install and enable `MMOCore` (optional case).
  2. Configure consumable with `restore-mana` and `restore-stamina`.
  3. Use item, then run `/csbridge doctor`.
- Expected:
  - Mana/Stamina increases according to config.
  - Doctor shows `MMOCore plugin: LOADED` and resource restore bridge available.
  - If MMOCore is not installed, item still works but mana/stamina restore is skipped with warning.

### R8 Resource Requirement/Cost
- Steps:
  1. Configure `requirements.min-mana` / `requirements.min-stamina` and `mana-cost` / `stamina-cost`.
  2. Try to use consumable when resource is lower than requirement/cost.
  3. Raise resource and use again.
- Expected:
  - Use is blocked with clear reason when resource is insufficient.
  - On success, mana/stamina is deducted by configured cost.
  - If MMOCore is disabled or missing and resource constraints are configured, use is blocked.

### R9 Vanilla Eating Restore Offset
- Steps:
  1. Configure an edible consumable with `vanilla-eating: true`, `restore-food`, `restore-saturation`.
  2. Consume once and record food/saturation delta.
- Expected:
  - Plugin restore is offset by vanilla food/saturation restored by the edible item.
  - No duplicated restore caused by vanilla + plugin stacking.

## 3. Stress Tests

### S1 Concurrent Right Click
- Scenario: 5-20 players spam right click for 3 minutes.
- Observe:
  - TPS and main thread lag
  - Console exceptions
  - Cooldown/consume bypass

### S2 High Frequency Trigger
- Scenario: One player at 10-20 CPS for 2 minutes.
- Observe:
  - `uses_left` never negative/out-of-range
  - Cooldown cannot be bypassed
  - No dupe/loss issue

### S3 Reload Consistency
- Scenario:
  1. Use item to mid state (`uses_left=2/3`).
  2. Run `/csbridge reload`, keep using.
  3. Restart server, keep using.
- Observe:
  - `uses_left` and `cooldown_until` stay consistent
  - Behavior stays identical before/after reload/restart

## 4. Acceptance Standard
- MMOItems is removed, and consumable chain is fully usable:
  - right-click/eat trigger
  - skill cast
  - consume and stack split
  - cooldown
  - effects, commands, sounds
- Regression R1-R6 all pass.
- Optional integration: R7 pass when MMOCore is installed.
- Optional integration: R8 pass when MMOCore is installed.
- Behavior parity: R9 pass for edible consumables.
- Stress S1-S3 has no blocking issue (no dupe exploit, no fatal error, no state corruption).
- Final decision: consumable flow is 100% usable without MMOItems.

## 5. Result Template
- Versions:
  - Paper:
  - MythicLib:
  - ConsumableSkillBridge:
- Regression:
  - R1:
  - R2:
  - R3:
  - R4:
  - R5:
  - R6:
- Stress:
  - S1:
  - S2:
  - S3:
- Final conclusion:
