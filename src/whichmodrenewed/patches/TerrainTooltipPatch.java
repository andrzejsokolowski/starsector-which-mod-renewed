package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import whichmodrenewed.Settings;
import whichmodrenewed.SourceLabels;

/** Terrain tooltips on the campaign map (nebulas, storms, fields and modded terrain). */
@Patch(target = @ClassMatch(subtype = CampaignTerrainPlugin.class))
public class TerrainTooltipPatch {

    static final String KIND = Settings.TERRAIN;

    @After(target = @MethodMatch(methodName = "createTooltip", parameterCount = 2))
    public static void afterCreateTooltip(AfterContext context) {
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !context.isMostDerivedCall()) return;
        try {
            CampaignTerrainPlugin plugin = context.getInferredSelf();
            if (plugin == null || plugin.getSpec() == null || !(context.getArg(0) instanceof TooltipMakerAPI)) return;
            SourceLabels.addLine(KIND, (TooltipMakerAPI) context.getArg(0), plugin.getSpec().getSourceMod(), 10f);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
        }
    }
}
