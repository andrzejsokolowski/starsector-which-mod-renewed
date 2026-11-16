package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import whichmodrenewed.SourceLabels;

/** Planet and colony condition tooltips, from every mod. The condition comes from BaseMarketConditionPlugin. */
@Patch(target = @ClassMatch(subtype = MarketConditionPlugin.class))
public class ConditionTooltipPatch {

    static final String KIND = "condition";

    @After(target = @MethodMatch(methodName = "createTooltip", parameterCount = 2))
    public static void afterCreateTooltip(AfterContext context) {
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !context.isMostDerivedCall()) return;
        try {
            if (!(context.getArg(0) instanceof TooltipMakerAPI) || !context.hasField("condition")) return;
            Object condition = context.getField("condition").get();
            if (!(condition instanceof MarketConditionAPI)) return;
            MarketConditionAPI c = (MarketConditionAPI) condition;
            if (c.getSpec() == null) return;
            SourceLabels.addLine(KIND, (TooltipMakerAPI) context.getArg(0), c.getSpec().getSourceMod(), 10f);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
        }
    }
}
