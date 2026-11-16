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
  and `com.fs.starfarer.campaign.ui.marketinfo` get "Mod: X" appended. Covers ships, fighters, weapons,
  ship systems and commodities. The id comes from `Description.getUID()` (id + "_" + type).
- `SkillDescriptionPatch`: same for `SkillSpec.getDescription` in `ui.impl`.
- After-hooks on interfaces, most derived call only: `SpecialItemPlugin.createTooltip` (blueprint data
  resolved to the unlocked thing), `HullModEffect.addPostDescriptionSection` (not for modspec items),
  `MarketConditionPlugin.createTooltip` (field `condition`), `AbilityPlugin.createTooltip`,
  `CampaignTerrainPlugin.createTooltip`.
- Industries: `IndustryLabels` is an `IndustryOptionProvider` listener (transient, added on game load).
  Not a BaseIndustry patch: Theory of Toolbox replaces BaseIndustry and PatchLib cannot patch it then.
- Every hook skips the codex (`Global.CODEX_TOOLTIP_MODE`) and turns itself off after its first error.
- `tools/selftest` is a load-test mod (never shipped): it runs the game's tooltip code at application
  load and writes `saves/common/wmr_selftest`. Not run yet: Andrzej tests in game himself.
