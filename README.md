# CPVPEventPlus

Production Crystal PvP event plugin for Paper 1.20.6+.

## Build
Requires Maven + internet access (pulls Paper API + PlaceholderAPI from their repos):
```
mvn clean package
```
Jar output: `target/CPVPEventPlus-1.0.0.jar`

## Install
1. Drop jar into `plugins/`.
2. Start server once to generate `plugins/CPVPEventPlus/` config files.
3. Edit `config.yml`, `messages.yml`, `scoreboard.yml`, `kits.yml` as needed.
4. Put RevFight arena world folders under `plugins/CPVPEventPlus/RevFights-Worlds/<arena>`.
5. Put regen templates under `plugins/CPVPEventPlus/regen-templates/<worldname>`.
6. Optional: install PlaceholderAPI for `%cpvp_*%` placeholders elsewhere (works without it too).

## Commands
See `plugin.yml` / COMMANDS.md for full list + usage + permissions.

## Permissions
See PERMISSIONS.md.

## Architecture
Modular managers under `net.cpvpevent.plugin.*` (event, border, drop, pvp, rekit,
revive, party, revfight, chat, kits, regen, scoreboard, gui, commands, listeners,
config, util). Single centralized scheduler ticks drive event stages, border
animation/damage, and scoreboard updates instead of per-player tasks. All tasks
are cancelled in `onDisable`.

## Known limitations / next steps
- Compiled by static review only (this build environment has no internet/Maven,
  so no `mvn package` was actually run). Do a `mvn clean package` locally first;
  fix any straggler API-version mismatches Paper may flag for your exact build.
- GUI wizard re-renders items in-place rather than re-titling the inventory per
  step (Bukkit limitation pre-1.20 title API); functional but title stays generic.
- RevFights/regen template folders must be created by staff before use.
