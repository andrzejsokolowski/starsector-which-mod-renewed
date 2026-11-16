package whichmodrenewed.patches;

import com.fs.starfarer.api.loading.Description;

import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;

/**
 * The game's tooltip classes (cargo, weapons, fighters, ships, ship systems, hull mods): every read of a
 * description's main text gets the "Mod: X" line. The package keeps its name across hotfixes.
 */
@Patch(target = @ClassMatch(packageName = "com.fs.starfarer.ui.impl"))
public class TooltipDescriptionPatch {

    @RedirectMethodCall(call = @MethodMatch(methodName = "getText1", parameterCount = 0),
            owner = @ClassMatch(type = Description.class))
    public static void text1(MethodCallContext context) {
        DescriptionText.text(context);
    }

    @RedirectMethodCall(call = @MethodMatch(methodName = "getText1FirstPara", parameterCount = 0),
            owner = @ClassMatch(type = Description.class))
    public static void text1FirstPara(MethodCallContext context) {
        DescriptionText.text(context);
    }

    @RedirectMethodCall(call = @MethodMatch(methodName = "getText1Paras", parameterCount = 0),
            owner = @ClassMatch(type = Description.class))
    public static void text1Paras(MethodCallContext context) {
        DescriptionText.paras(context);
    }
}
