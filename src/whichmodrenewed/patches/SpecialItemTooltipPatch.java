package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import whichmodrenewed.SourceLabels;

/**
 * Every special item's tooltip, from every mod, including items with their own tooltip code that never
 * show the item's description. Runs once per tooltip: only the most derived createTooltip adds the line.
 */
@Patch(target = @ClassMatch(subtype = SpecialItemPlugin.class))
public class SpecialItemTooltipPatch {

    static final String KIND = "special item";

    @After(target = @MethodMatch(methodName = "createTooltip", parameterCount = 4))
    public static void afterCreateTooltip(AfterContext context) {
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !context.isMostDerivedCall()) return;
        try {
            SpecialItemPlugin plugin = context.getInferredSelf();
            if (plugin == null || !(context.getArg(0) instanceof TooltipMakerAPI)) return;
            SourceLabels.addLine(KIND, (TooltipMakerAPI) context.getArg(0),
                    SourceLabels.specialItemSource(plugin.getSpec(), stackOf(context)), 10f);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
        }
    }

    /** The cargo stack the item sits in, kept by BaseSpecialItemPlugin. Null for items that keep none. */
    private static CargoStackAPI stackOf(AfterContext context) {
        try {
            if (!context.hasField("stack")) return null;
            Object stack = context.getField("stack").get();
            return stack instanceof CargoStackAPI ? (CargoStackAPI) stack : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
