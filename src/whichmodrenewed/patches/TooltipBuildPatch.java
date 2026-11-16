package whichmodrenewed.patches;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

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
 *
 * The codex link ("F2 codex - mod") is set inside createImpl too: the game keeps a codex entry id only
 * when the entry exists, and a codex fleet member or temporary entry also make the link. Checking them
 * at the end is three field reads per tooltip.
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
        SourceLabels.endBuild(context.getSelf(), hasCodex(context));
    }

    @Except(target = @MethodMatch(methodName = "createImpl", parameterCount = 1))
    public static void onError(ExceptContext context) {
        if (!context.isMostDerivedCall()) return;
        SourceLabels.abortBuild(context.getSelf());
    }

    private static boolean hasCodex(AfterContext context) {
        try {
            Object self = context.getSelf();
            if (self instanceof TooltipMakerAPI && ((TooltipMakerAPI) self).getCodexEntryId() != null) return true;
            return isSet(context, "tempCodexEntry") || isSet(context, "codexFleetMember");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isSet(AfterContext context, String field) {
        try {
            return context.hasField(field) && context.getField(field).get() != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
