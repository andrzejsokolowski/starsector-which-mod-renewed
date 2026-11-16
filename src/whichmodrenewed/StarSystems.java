package whichmodrenewed;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.util.Misc;

/**
 * Which mod made a star system. The game does not record it, so:
 * <ol>
 * <li>Procedurally generated systems get nothing (Andrzej's rule).</li>
 * <li>The 24 hand-made vanilla systems read Vanilla, by name.</li>
 * <li>New games: a system created while a mod's plugin runs (onNewGame and the like, onGameLoad) is
 * recorded in its memory under {@link #MEMORY_KEY} as that mod's id.</li>
 * <li>Older saves: the mod most of its markets' factions come from (the mod that wrote the faction's
 * description), or the mod of its modded star and planet types.</li>
 * <li>Anything else gets nothing.</li>
 * </ol>
 */
public final class StarSystems {

    public static final String MEMORY_KEY = "$wmr_sourceMod";

    private static final Logger log = Global.getLogger(StarSystems.class);

    /** The hand-made systems in starsector-core/data/scripts/world (0.98.5a). */
    private static final Set<String> VANILLA = new HashSet<>(Arrays.asList("al gebbar", "arcadia", "askonia",
            "aztlan", "canaan", "corvus", "duzahk", "eos exodus", "galatia", "hybrasil", "isirah", "kumari kandam",
            "magec", "mayasura", "naraka", "penelope's star", "samarra", "thule", "tia", "tyle", "valhalla",
            "westernesse", "yma", "zagan"));

    private static final Map<String, ModSpecAPI> modsByPluginClass = new HashMap<>();
    private static final Map<String, ModSpecAPI> modsById = new HashMap<>();
    private static final ArrayDeque<ModSpecAPI> running = new ArrayDeque<>();
    /** System id to mod name, "" for none. Cleared on game load and when settings change. */
    private static final Map<String, String> cache = new HashMap<>();
    private static final Map<String, ModSpecAPI> factionMods = new HashMap<>();
    private static final ModSpecAPI NO_MOD = null;

    private StarSystems() {
    }

    public static synchronized void index() {
        if (!modsByPluginClass.isEmpty()) return;
        for (ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            modsById.put(mod.getId(), mod);
            String plugin = mod.getModPluginClassName();
            if (plugin != null && !plugin.isEmpty()) modsByPluginClass.put(plugin, mod);
        }
    }

    public static void clearCache() {
        cache.clear();
        factionMods.clear();
    }

    /** A mod's plugin started one of its new-game or game-load steps. */
    public static void pluginStarted(Object plugin) {
        index();
        running.push(plugin == null ? null : modsByPluginClass.get(plugin.getClass().getName()));
    }

    public static void pluginEnded() {
        if (!running.isEmpty()) running.pop();
    }

    /** A star system was just created: remember the mod whose plugin is running, if any. */
    public static void systemCreated(StarSystemAPI system) {
        ModSpecAPI mod = running.peek();
        if (system == null || mod == null) return;
        system.getMemoryWithoutUpdate().set(MEMORY_KEY, mod.getId());
    }

    /** The mod name to show for a star system, or null to show nothing. */
    public static String nameFor(StarSystemAPI system) {
        if (system == null) return null;
        String id = system.getId();
        String cached = cache.get(id);
        if (cached == null) {
            cached = compute(system);
            cache.put(id, cached == null ? "" : cached);
        }
        return cached.isEmpty() ? null : cached;
    }

    private static String compute(StarSystemAPI system) {
        try {
            if (system.isProcgen()) return null;
            String base = system.getBaseName();
            if (base != null && VANILLA.contains(base.toLowerCase())) return SourceLabels.VANILLA;
            index();
            Object recorded = system.getMemoryWithoutUpdate().get(MEMORY_KEY);
            if (recorded instanceof String) {
                ModSpecAPI mod = modsById.get(recorded);
                return mod != null ? SourceLabels.name(mod) : null;
            }
            ModSpecAPI guess = guess(system);
            return guess != null ? SourceLabels.name(guess) : null;
        } catch (Throwable t) {
            log.warn("Which Mod - Renewed: could not tell which mod made " + system.getId(), t);
            return null;
        }
    }

    /** Older saves: the mod most of the system's markets' factions or modded planet types come from. */
    private static ModSpecAPI guess(StarSystemAPI system) {
        Map<ModSpecAPI, Integer> votes = new HashMap<>();
        for (MarketAPI market : Misc.getMarketsInLocation(system)) {
            if (market.getFaction() != null) vote(votes, factionMod(market.getFaction().getId()), 2);
        }
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.getSpec() != null) vote(votes, planet.getSpec().getSourceMod(), 1);
        }
        ModSpecAPI best = null;
        int bestVotes = 0;
        for (Map.Entry<ModSpecAPI, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > bestVotes) {
                best = entry.getKey();
                bestVotes = entry.getValue();
            }
        }
        return best;
    }

    private static void vote(Map<ModSpecAPI, Integer> votes, ModSpecAPI mod, int weight) {
        if (mod == null || Settings.isVanillaMod(mod.getId())) return;
        Integer n = votes.get(mod);
        votes.put(mod, n == null ? weight : n + weight);
    }

    private static ModSpecAPI factionMod(String factionId) {
        if (factionMods.containsKey(factionId)) return factionMods.get(factionId);
        ModSpecAPI mod = NO_MOD;
        try {
            Description description = Global.getSettings().getDescription(factionId, Description.Type.FACTION);
            if (description != null) mod = description.getSourceMod();
        } catch (Throwable ignored) {
        }
        factionMods.put(factionId, mod);
        return mod;
    }
}
