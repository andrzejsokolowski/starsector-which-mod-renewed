package whichmodrenewed.patches;

import com.fs.starfarer.api.Global;

import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;
import whichmodrenewed.Settings;
import whichmodrenewed.SourceLabels;

/**
 * Skill tooltips (character screen, officers): the "Mod: X" line at the bottom of the tooltip, or after
 * the skill's description when the tooltip is not one of the game's expandable ones.
 */
@Patch(target = @ClassMatch(packageName = "com.fs.starfarer.ui.impl"))
public class SkillDescriptionPatch {

    static final String KIND = Settings.SKILLS;

    @RedirectMethodCall(call = @MethodMatch(methodName = "getDescription", parameterCount = 0),
            owner = @ClassMatch(typeName = "com.fs.starfarer.loading.SkillSpec"))
    public static void description(MethodCallContext context) {
        Object original = context.call();
        if (Global.CODEX_TOOLTIP_MODE || SourceLabels.isFailed(KIND) || !Settings.shows(KIND)
                || !(original instanceof String)) {
            context.setResult(original);
            return;
        }
        try {
            String name = SourceLabels.name(SourceLabels.sourceOf(context.getCallReceiver()));
            if (SourceLabels.defer(KIND, null, name, false)) {
                context.setResult(original);
                return;
            }
            String label = SourceLabels.PREFIX + name;
            String result = SourceLabels.appendLabel((String) original, label);
            if (result != original) SourceLabels.count(KIND, label);
            context.setResult(result);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
            context.setResult(original);
        }
    }
}
