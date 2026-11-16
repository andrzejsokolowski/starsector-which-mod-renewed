package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import whichmodrenewed.Settings;
import whichmodrenewed.SourceLabels;

/**
 * Hull mod tooltips in the refit screen. The game asks every hull mod for its section under the
 * description, and the line goes at the end of it. Modspec items in cargo (isForModSpec) get their line
 * from {@link SpecialItemTooltipPatch} instead, so it shows once.
 */
@Patch(target = @ClassMatch(subtype = HullModEffect.class))
public class HullModTooltipPatch {

    static final String KIND = Settings.HULL_MODS;

    @After(target = @MethodMatch(methodName = "addPostDescriptionSection", parameterCount = 5))
    public static void afterPostDescription(AfterContext context) {
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !context.isMostDerivedCall()) return;
        try {
            if (Boolean.TRUE.equals(context.getArg(4))) return;
            if (!(context.getArg(0) instanceof TooltipMakerAPI)) return;
            HullModSpecAPI spec = SourceLabels.hullModFor(context.getSelf());
            if (spec == null) return;
            SourceLabels.addLine(KIND, (TooltipMakerAPI) context.getArg(0), spec.getSourceMod(), 10f);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
        }
    }
}
