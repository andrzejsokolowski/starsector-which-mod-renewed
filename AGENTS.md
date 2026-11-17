# Which Mod - Renewed

These instructions apply to this repository, including when it is opened without the parent mod workspace.
The workspace `AGENTS.md` one folder up still governs releases, packaging, Git identity and communication.

## Notion

Tasks, history and test lists: the **Which Mod - Renewed board** in Andrzej's Notion, under
**Starsector mods** (`collection://41a20ca7-5d20-8174-92f6-000b4d694d0d`). Ticket ids start with WMR.
Read the board with `notion-query-data-sources` mode `view` on `view://41a20ca7-5d20-8198-9010-000c7450e9cc`.
WMR-1 holds Andrzej's original request word for word.

## Build

`./gradlew releaseZip` writes `WhichModRenewed.zip` here (staging folder `WhichModRenewed`).

## How it hangs together

- `SourceLabels` finds the mod with `WithSourceMod.getSourceMod()` (what the codex reads); null = "Vanilla".
  It indexes every spec at application load and draws the line with the codex icon
  `graphics/ui/icons/codex_mod_info.png` from the game's own files.
- Description redirects (`TooltipDescriptionPatch`, `MarketInfoDescriptionPatch`): calls to
  `Description.getText1/getText1FirstPara/getText1Paras` inside the packages `com.fs.starfarer.ui.impl`
  and `com.fs.starfarer.campaign.ui.marketinfo` find the mod. Covers ships, fighters, weapons,
  ship systems and commodities. The id comes from `Description.getUID()` (id + "_" + type).
- Bottom of the tooltip (Andrzej's ask, WMR-1 Changes): `TooltipBuildPatch` tracks
  `StandardTooltipV2Expandable.createImpl` (the game fills a tooltip there, then sizes it). While one is
  being filled, every hook hands its mod name to `SourceLabels.defer` and the line is drawn at the end of
  createImpl. Plugin hooks win over description names, first wins, one line per tooltip. Outside a
  build (ship system tooltips are built in one go) the line goes after the description text instead.
- `SkillDescriptionPatch`: same for `SkillSpec.getDescription` in `ui.impl`.
- After-hooks on interfaces, most derived call only: `SpecialItemPlugin.createTooltip` (blueprint data
  resolved to the unlocked thing), `HullModEffect.addPostDescriptionSection` (not for modspec items),
  `MarketConditionPlugin.createTooltip` (field `condition`), `AbilityPlugin.createTooltip`,
  `CampaignTerrainPlugin.createTooltip`.
- Industries: `IndustryLabels` is an `IndustryOptionProvider` listener (transient, added on game load).
  Not a BaseIndustry patch: Theory of Toolbox replaces BaseIndustry and PatchLib cannot patch it then.
- Every hook skips the codex (`Global.CODEX_TOOLTIP_MODE`) and turns itself off after its first error.
- Codex link (1.1.0, fixed in 2.0.1): the game's "Press F2 to open Codex" footer never names the mod; the
  " - mod" part is Random Assortment of Things' `WhichModPatch` (After `makeExpandable` and
  `toggleExpandedLabelAndBoolean`, only when the entry has a source mod and RAT's per-kind LunaLib switch is
  on). `TooltipBuildPatch` skips the line only when the footer names the mod: codex-only tooltips get their
  footer inside createImpl, so it reads `expandLabel` text (" - "); tooltips with a second key
  (`expandHotkeyStr2`, F1 more info) get it after createImpl, so it checks RAT's rule and switches.
  No RAT = the line always shows. LunaLib `wmr_onlyWithoutCodex` (default on).
- Treat as Vanilla (`wmr_vanillaMods`, default `yunruHullModsLib, animated_items`): `SourceLabels.name`
  and `CodexSourcePatch` (After `CodexEntryPlugin.getSourceMod`, returns null) so the codex agrees.
- Settings: `Settings` holds the values (defaults without LunaLib), `LunaBridge` is the only class that
  names LunaLib (build check). One switch per view kind, `wmr_show_<kind>`.
- Star systems (`StarSystems`): procgen = nothing; the 24 vanilla names = Vanilla; `ModPluginScopePatch`
  tracks which mod plugin runs (new-game steps, onGameLoad) and `StarSystemCreatePatch` stores its id in
  the system memory `$wmr_sourceMod`; older saves guess from market factions (FACTION description source
  mod) and modded planet types. Shown by a redirect of `StarSystemAPI.getName` in
  `StarSystemTooltipFactory$1` (check the inner class number after game updates).
- `tools/selftest` is a load-test mod (never shipped): it runs the game's tooltip code at application
  load and writes `saves/common/wmr_selftest`. Not run yet: Andrzej tests in game.
