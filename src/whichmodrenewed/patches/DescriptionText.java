package whichmodrenewed.patches;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.Description;

import patchlib.api.context.MethodCallContext;
import whichmodrenewed.SourceLabels;
import whichmodrenewed.SourceLabels.Label;

/**
 * Shared body of the description redirects: ships, fighters, weapons, ship systems, commodities and the
 * rest read their text from a {@link Description}. Inside one of the game's expandable tooltips the mod
 * name waits for the tooltip's bottom and the text stays as it is. Elsewhere (ship system tooltips, built
 * in one go) the "Mod: X" line goes after the text. Only calls inside the tooltip classes are redirected,
 * so the codex, intel and other mods' screens see the descriptions unchanged.
 */
final class DescriptionText {

    /** Failures are tracked for the redirect as a whole; the view kind comes from the description. */
    static final String HOOK = "description";

    private DescriptionText() {
    }

    private static Label label(MethodCallContext context, Object original) {
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(HOOK) || original == null) return null;
        return SourceLabels.labelFor((Description) context.getCallReceiver());
    }

    static void text(MethodCallContext context) {
        Object original = context.call();
        try {
            Label label = original instanceof String ? label(context, original) : null;
            if (label == null || SourceLabels.defer(label.kind, null, label.name, false)) {
                context.setResult(original);
                return;
            }
            String line = SourceLabels.PREFIX + label.name;
            String result = SourceLabels.appendLabel((String) original, line);
            if (result != original) SourceLabels.count(label.kind, line);
            context.setResult(result);
        } catch (Throwable t) {
            SourceLabels.fail(HOOK, t);
            context.setResult(original);
        }
    }

    @SuppressWarnings("unchecked")
    static void paras(MethodCallContext context) {
        Object original = context.call();
        try {
            Label label = original instanceof List ? label(context, original) : null;
            if (label == null || SourceLabels.defer(label.kind, null, label.name, false)) {
                context.setResult(original);
                return;
            }
            String line = SourceLabels.PREFIX + label.name;
            List<String> result = SourceLabels.appendLabel((List<String>) original, line);
            if (result != original) SourceLabels.count(label.kind, line);
            context.setResult(result);
        } catch (Throwable t) {
            SourceLabels.fail(HOOK, t);
            context.setResult(original);
        }
    }
}
