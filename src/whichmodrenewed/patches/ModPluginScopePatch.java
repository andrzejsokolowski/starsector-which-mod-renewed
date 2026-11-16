package whichmodrenewed.patches;

import com.fs.starfarer.api.ModPlugin;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import whichmodrenewed.StarSystems;

/**
 * Knows which mod's plugin is running during the steps where mods build their star systems: the new-game
 * steps and onGameLoad (mods added to a running game build theirs then).
 */
@Patch(target = @ClassMatch(subtype = ModPlugin.class))
public class ModPluginScopePatch {

    @Before(target = @MethodMatch(methodName = "onNewGame", parameterCount = 0))
    public static void beforeNewGame(BeforeContext c) { start(c); }
    @After(target = @MethodMatch(methodName = "onNewGame", parameterCount = 0))
    public static void afterNewGame(AfterContext c) { end(c.isMostDerivedCall()); }
    @Except(target = @MethodMatch(methodName = "onNewGame", parameterCount = 0))
    public static void failNewGame(ExceptContext c) { end(c.isMostDerivedCall()); }

    @Before(target = @MethodMatch(methodName = "onNewGameAfterProcGen", parameterCount = 0))
    public static void beforeProcGen(BeforeContext c) { start(c); }
    @After(target = @MethodMatch(methodName = "onNewGameAfterProcGen", parameterCount = 0))
    public static void afterProcGen(AfterContext c) { end(c.isMostDerivedCall()); }
    @Except(target = @MethodMatch(methodName = "onNewGameAfterProcGen", parameterCount = 0))
    public static void failProcGen(ExceptContext c) { end(c.isMostDerivedCall()); }

    @Before(target = @MethodMatch(methodName = "onNewGameAfterEconomyLoad", parameterCount = 0))
    public static void beforeEconomy(BeforeContext c) { start(c); }
    @After(target = @MethodMatch(methodName = "onNewGameAfterEconomyLoad", parameterCount = 0))
    public static void afterEconomy(AfterContext c) { end(c.isMostDerivedCall()); }
    @Except(target = @MethodMatch(methodName = "onNewGameAfterEconomyLoad", parameterCount = 0))
    public static void failEconomy(ExceptContext c) { end(c.isMostDerivedCall()); }

    @Before(target = @MethodMatch(methodName = "onNewGameAfterTimePass", parameterCount = 0))
    public static void beforeTimePass(BeforeContext c) { start(c); }
    @After(target = @MethodMatch(methodName = "onNewGameAfterTimePass", parameterCount = 0))
    public static void afterTimePass(AfterContext c) { end(c.isMostDerivedCall()); }
    @Except(target = @MethodMatch(methodName = "onNewGameAfterTimePass", parameterCount = 0))
    public static void failTimePass(ExceptContext c) { end(c.isMostDerivedCall()); }

    @Before(target = @MethodMatch(methodName = "onGameLoad", parameterCount = 1))
    public static void beforeGameLoad(BeforeContext c) { start(c); }
    @After(target = @MethodMatch(methodName = "onGameLoad", parameterCount = 1))
    public static void afterGameLoad(AfterContext c) { end(c.isMostDerivedCall()); }
    @Except(target = @MethodMatch(methodName = "onGameLoad", parameterCount = 1))
    public static void failGameLoad(ExceptContext c) { end(c.isMostDerivedCall()); }

    private static void start(BeforeContext context) {
        if (!context.isMostDerivedCall()) return;
        try {
            StarSystems.pluginStarted(context.getSelf());
        } catch (Throwable ignored) {
        }
    }

    private static void end(boolean mostDerived) {
        if (mostDerived) StarSystems.pluginEnded();
    }
}
