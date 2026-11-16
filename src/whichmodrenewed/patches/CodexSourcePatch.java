package whichmodrenewed.patches;

import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import whichmodrenewed.Settings;

/**
 * Mods on the Treat as Vanilla list (zzz BlockTags, Animated Colony Items by default) only change vanilla
 * things, so their codex entries read as vanilla too: no mod name in the codex or in the tooltip's
 * "F2 codex" link.
 */
@Patch(target = @ClassMatch(subtype = CodexEntryPlugin.class))
public class CodexSourcePatch {

    @After(target = @MethodMatch(methodName = "getSourceMod", parameterCount = 0))
    public static void afterGetSourceMod(AfterContext context) {
        try {
            Object mod = context.getReturnValue();
            if (mod instanceof ModSpecAPI && Settings.isVanillaMod(((ModSpecAPI) mod).getId())) {
                context.setReturnValue(null);
            }
        } catch (Throwable ignored) {
        }
    }
}
