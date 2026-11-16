package whichmodrenewed;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

/**
 * Industry and structure tooltips (colony screen and the build list), through the game's own listener
 * for additions to industry tooltips. A listener and not a patch: Theory of Toolbox swaps in its own
 * BaseIndustry, and PatchLib cannot patch that class while it does.
 */
public class IndustryLabels implements IndustryOptionProvider {

    static final String KIND = "industry";

    @Override
    public List<IndustryOptionData> getIndustryOptions(Industry ind) {
        return null;
    }

    @Override
    public void createTooltip(IndustryOptionData opt, TooltipMakerAPI tooltip, float width) {
    }

    @Override
    public void optionSelected(IndustryOptionData opt, DialogCreatorUI ui) {
    }

    @Override
    public void addToIndustryTooltip(Industry ind, IndustryTooltipMode mode, TooltipMakerAPI tooltip, float width,
            boolean expanded) {
        add(ind, tooltip);
    }

    @Override
    public void addToIndustryTooltipIfIndustryNotPresent(Industry ind, IndustryTooltipMode mode,
            TooltipMakerAPI tooltip, float width, boolean expanded) {
        add(ind, tooltip);
    }

    private static void add(Industry ind, TooltipMakerAPI tooltip) {
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || ind == null) return;
        try {
            if (ind.getSpec() == null) return;
            SourceLabels.addLine(KIND, tooltip, ind.getSpec().getSourceMod(), 10f);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
        }
    }
}
