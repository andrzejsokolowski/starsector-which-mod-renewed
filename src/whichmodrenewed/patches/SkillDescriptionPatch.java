package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;

import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;
import whichmodrenewed.SourceLabels;

/**
 * Skill tooltips (character screen, officers): the skill's description, as the game's tooltip code reads
 * it, ends with the "Mod: X" line. A skill with no description shows just the line.
 */
@Patch(target = @ClassMatch(packageName = "com.fs.starfarer.ui.impl"))
public class SkillDescriptionPatch {

    static final String KIND = "skill";

    @RedirectMethodCall(call = @MethodMatch(methodName = "getDescription", parameterCount = 0),
            owner = @ClassMatch(typeName = "com.fs.starfarer.loading.SkillSpec"))
    public static void description(MethodCallContext context) {
        Object original = context.call();
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !(original instanceof String)) {
            context.setResult(original);
            return;
        }
        try {
            String label = SourceLabels.text(SourceLabels.sourceOf(context.getCallReceiver()));
            String result = SourceLabels.appendLabel((String) original, label);
            if (result != original) SourceLabels.count(KIND, label);
            context.setResult(result);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
            context.setResult(original);
        }
    }
}
