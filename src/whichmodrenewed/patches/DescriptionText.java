package whichmodrenewed.patches;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.Description;

import patchlib.api.context.MethodCallContext;
import whichmodrenewed.SourceLabels;

/**
 * Shared body of the description redirects: ships, fighters, weapons, ship systems, commodities and the
 * rest read their text from a {@link Description}, and the game's tooltip code gets that text with the
 * "Mod: X" line: at the bottom of the tooltip when it is one of the game's expandable tooltips, else
 * after the text. Only calls inside the tooltip classes are redirected, so the codex, intel and
 * other mods' screens see the descriptions unchanged.
 */
final class DescriptionText {

    static final String KIND = "description";

    private DescriptionText() {
    }

    static void text(MethodCallContext context) {
        Object original = context.call();
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !(original instanceof String)) {
            context.setResult(original);
            return;
        }
        try {
            String label = SourceLabels.labelFor((Description) context.getCallReceiver());
            if (deferred(label)) {
                context.setResult(original);
                return;
            }
            String result = SourceLabels.appendLabel((String) original, label);
            if (result != original) SourceLabels.count(KIND, label);
            context.setResult(result);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
            context.setResult(original);
        }
    }

    /**
     * Inside a game tooltip being filled, the name waits for the tooltip's bottom and the text stays as
     * it is. Elsewhere (ship system tooltips, built in one go) the line goes into the text.
     */
    static boolean deferred(String label) {
        return label != null && SourceLabels.defer(KIND, null, label.substring(SourceLabels.PREFIX.length()), false);
    }

    @SuppressWarnings("unchecked")
    static void paras(MethodCallContext context) {
        Object original = context.call();
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !(original instanceof List)) {
            context.setResult(original);
            return;
        }
        try {
            String label = SourceLabels.labelFor((Description) context.getCallReceiver());
            if (deferred(label)) {
                context.setResult(original);
                return;
            }
            List<String> result = SourceLabels.appendLabel((List<String>) original, label);
            if (result != original) SourceLabels.count(KIND, label);
            context.setResult(result);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
            context.setResult(original);
        }
    }
}
