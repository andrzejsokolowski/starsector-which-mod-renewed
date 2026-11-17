package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import whichmodrenewed.Settings;
import whichmodrenewed.SourceLabels;

/**
 * Tracks which game tooltip is being filled. The game's tooltips fill their content in createImpl and size
 * the window right after it, so a line added at the end of createImpl lands at the bottom of the tooltip
 * and the window grows to fit it. Labels found while a tooltip fills wait in {@link SourceLabels} until
 * then. The class keeps its name across hotfixes.
 *
 * The codex link footer ("Press F2 to open Codex") never names the mod in the game itself. Random
 * Assortment of Things (WhichModPatch) adds " - mod name" to it, after makeExpandable and after
 * toggleExpandedLabelAndBoolean, and only when the codex entry has a source mod and RAT's switch for that
 * kind is on. The line is skipped only when the footer names the mod:
 * - Codex link only: the game makes the footer inside createImpl (setCodexEntryId, makeExpandable), so RAT
 *   has already run and the footer text is read as it is.
 * - Codex link plus another key (F1 more info): the game writes the footer after createImpl
 *   (beforeShown, toggleExpandedLabelAndBoolean), so RAT's own rule is checked instead.
 */
@Patch(target = @ClassMatch(subtypeName = "com.fs.starfarer.ui.impl.StandardTooltipV2Expandable"))
public class TooltipBuildPatch {

    private static final String RAT = "assortment_of_things";

    /** RAT's codex entry prefixes and the switch that turns its footer on for each. */
    private static final String[][] RAT_SWITCHES = {
            {"codex_hull_", "rat_whichModShips"},
            {"codex_weapon_", "rat_whichModWeapons"},
            {"codex_fighter_", "rat_whichModFighters"},
            {"codex_hullmod_", "rat_whichModHullmods"},
            {"codex_item_", "rat_whichModCargo"},
            {"codex_commodity_", "rat_whichModCargo"},
            {"codex_artifacts_", "rat_whichModCargo"},
            {"codex_industry_", "rat_whichModIndustries"},
            {"codex_condition_", "rat_whichModConditions"},
            {"codex_skill_", "rat_whichModSkills"},
            {"codex_ability_", "rat_whichModSkills"},
            {"codex_sic_aptitude_", "rat_whichModSkills"},
    };

    private static Boolean ratEnabled;

    @Before(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void beforeCreate(BeforeContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.beginBuild(context.getSelf());
    }

    @After(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void afterCreate(AfterContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.endBuild(context.getSelf(), footerNamesMod(context));
    }

    @Except(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void onError(ExceptContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.abortBuild(context.getSelf());
    }

    /** Whether the tooltip's codex footer shows the mod name. Any doubt counts as no: the line then shows. */
    private static boolean footerNamesMod(AfterContext context) {
        try {
            if (!Settings.onlyWithoutCodex() || !isRatEnabled()) return false;
            Object self = context.getSelf();
            String entryId = self instanceof TooltipMakerAPI ? ((TooltipMakerAPI) self).getCodexEntryId() : null;
            Object member = get(context, "codexFleetMember");
            if (entryId == null && member == null) return false;

            if (get(context, "expandHotkeyStr2") == null) {
                // Codex link only: the footer is already written, RAT included.
                Object label = get(context, "expandLabel");
                if (label instanceof LabelAPI) {
                    String text = ((LabelAPI) label).getText();
                    return text != null && text.contains(" - ");
                }
            }
            return ratWouldName(entryId, member);
        } catch (Throwable t) {
            return false;
        }
    }

    /** RAT's own rule (WhichModPatch.addWhichModText). */
    private static boolean ratWouldName(String entryId, Object member) {
        CodexEntryPlugin entry = entryId != null ? CodexDataV2.getEntry(entryId) : null;
        if (entry != null) {
            String key = null;
            for (String[] row : RAT_SWITCHES) {
                if (entryId.startsWith(row[0])) {
                    key = row[1];
                    break;
                }
            }
            if (key == null || !Boolean.TRUE.equals(Settings.otherModBool(RAT, key))) return false;
            ModSpecAPI mod = entry.getSourceMod();
            return mod != null && mod.getName() != null;
        }
        if (!(member instanceof FleetMemberAPI)) return false;
        if (!Boolean.TRUE.equals(Settings.otherModBool(RAT, "rat_whichModShips"))) return false;
        ShipHullSpecAPI hull = ((FleetMemberAPI) member).getHullSpec();
        if (hull == null) return false;
        ModSpecAPI mod = hull.getSourceMod();
        if (mod == null && hull.getBaseHullId() != null) {
            ShipHullSpecAPI base = Global.getSettings().getHullSpec(hull.getBaseHullId());
            if (base != null) mod = base.getSourceMod();
        }
        return mod != null && mod.getName() != null;
    }

    private static boolean isRatEnabled() {
        if (ratEnabled == null) ratEnabled = Global.getSettings().getModManager().isModEnabled(RAT);
        return ratEnabled;
    }

    private static Object get(AfterContext context, String field) {
        try {
            return context.hasField(field) ? context.getField(field).get() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
