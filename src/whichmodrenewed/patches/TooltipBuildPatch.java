package whichmodrenewed.patches;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import whichmodrenewed.SourceLabels;

/**
 * Tracks which game tooltip is being filled. The game's tooltips fill their content in createImpl and size
 * the window right after it, so a line added at the end of createImpl lands at the bottom of the tooltip
 * and the window grows to fit it. Labels found while a tooltip fills wait in {@link SourceLabels} until
 * then. The class keeps its name across hotfixes.
 */
@Patch(target = @ClassMatch(subtypeName = "com.fs.starfarer.ui.impl.StandardTooltipV2Expandable"))
public class TooltipBuildPatch {

    @Before(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void beforeCreate(BeforeContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.beginBuild(context.getSelf());
    }

    @After(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void afterCreate(AfterContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.endBuild(context.getSelf());
    }

    @Except(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void onError(ExceptContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.abortBuild(context.getSelf());
    }
}
