package whichmodrenewed.patches;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import whichmodrenewed.StarSystems;

/** Every new star system: remembers which mod's plugin was running when it was made. */
@Patch(target = @ClassMatch(subtype = SectorAPI.class))
public class StarSystemCreatePatch {

    @After(target = @MethodMatch(methodName = "createStarSystem", parameterCount = 1))
    public static void afterCreate(AfterContext context) {
        try {
            Object system = context.getReturnValue();
            if (system instanceof StarSystemAPI) StarSystems.systemCreated((StarSystemAPI) system);
        } catch (Throwable ignored) {
        }
    }
}
