package whichmodrenewed.patches;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;
import whichmodrenewed.Settings;
import whichmodrenewed.SourceLabels;
import whichmodrenewed.StarSystems;

/**
 * The star system tooltip on the sector map (StarSystemTooltipFactory, its one tooltip class $1). It reads
 * the system's name while it fills, which hands the system's mod to the tooltip's bottom line.
 * Check the inner class number after a game update.
 */
@Patch(target = @ClassMatch(typeName = "com.fs.starfarer.ui.impl.StarSystemTooltipFactory$1"))
public class StarSystemTooltipPatch {

    static final String KIND = Settings.STAR_SYSTEMS;

    @RedirectMethodCall(call = @MethodMatch(methodName = "getName", parameterCount = 0),
            owner = @ClassMatch(type = StarSystemAPI.class))
    public static void name(MethodCallContext context) {
        Object name = context.call();
        context.setResult(name);
        if (SourceLabels.isFailed(KIND) || !Settings.shows(KIND)) return;
        try {
            Object system = context.getCallReceiver();
            if (!(system instanceof StarSystemAPI)) return;
            String mod = StarSystems.nameFor((StarSystemAPI) system);
            if (mod != null) SourceLabels.defer(KIND, null, mod, true);
        } catch (Throwable t) {
            SourceLabels.fail(KIND, t);
        }
    }
}
