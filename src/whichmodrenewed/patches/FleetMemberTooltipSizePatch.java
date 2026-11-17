package whichmodrenewed.patches;

import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

/**
 * The window of a plain game tooltip takes its content panel's size in StandardTooltipV2.updateSize. The
 * plain fleet member tooltip calls it at the end of its beforeShown, after filling the panel: the moment
 * {@link FleetMemberTooltipPatch} adds the line.
 */
@Patch(target = @ClassMatch(typeName = "com.fs.starfarer.ui.impl.StandardTooltipV2"))
public class FleetMemberTooltipSizePatch {

    @Before(target = @MethodMatch(methodName = "updateSize", parameterCount = 0))
    public static void beforeUpdateSize(BeforeContext context) {
        FleetMemberTooltipPatch.beforeSizing(context);
    }
}
