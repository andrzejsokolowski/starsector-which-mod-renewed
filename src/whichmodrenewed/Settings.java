package whichmodrenewed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;

/**
 * The player's choices. With LunaLib they come from its settings screen (read once, then again when the
 * player changes them), without it the defaults below apply. Nothing here names LunaLib's classes:
 * {@link LunaBridge} does, and it is only loaded when LunaLib is enabled.
 */
public final class Settings {

    public static final String MOD_ID = "which_mod_renewed";

    /** Where the line can show. Each kind has a LunaLib switch "wmr_show_" + key. */
    public static final String SHIPS = "ships";
    public static final String WEAPONS = "weapons";
    public static final String SHIP_SYSTEMS = "shipSystems";
    public static final String COMMODITIES = "commodities";
    public static final String OTHER_DESCRIPTIONS = "otherDescriptions";
    public static final String HULL_MODS = "hullMods";
    public static final String ITEMS = "items";
    public static final String INDUSTRIES = "industries";
    public static final String CONDITIONS = "conditions";
    public static final String SKILLS = "skills";
    public static final String ABILITIES = "abilities";
    public static final String TERRAIN = "terrain";
    public static final String STAR_SYSTEMS = "starSystems";

    /** Mods that only change vanilla things. Their changes read as Vanilla, in the codex too. */
    public static final String DEFAULT_VANILLA_MODS = "yunruHullModsLib, animated_items";

    private static final Logger log = Global.getLogger(Settings.class);

    private static final Map<String, Boolean> shown = new HashMap<>();
    private static volatile boolean onlyWithoutCodex = true;
    private static volatile Set<String> vanillaMods = parse(DEFAULT_VANILLA_MODS);
    private static boolean lunaChecked = false;
    private static boolean luna = false;

    private Settings() {
    }

    /** Reads the settings. Called at application load and by LunaLib when the player changes them. */
    public static synchronized void load() {
        if (!lunaChecked) {
            lunaChecked = true;
            luna = Global.getSettings().getModManager().isModEnabled("lunalib");
            if (luna) {
                try {
                    LunaBridge.listen();
                } catch (Throwable t) {
                    luna = false;
                    log.error("Which Mod - Renewed: could not read LunaLib settings, using the defaults", t);
                }
            }
        }
        if (!luna) return;
        try {
            shown.clear();
            for (String kind : new String[] {SHIPS, WEAPONS, SHIP_SYSTEMS, COMMODITIES, OTHER_DESCRIPTIONS,
                    HULL_MODS, ITEMS, INDUSTRIES, CONDITIONS, SKILLS, ABILITIES, TERRAIN, STAR_SYSTEMS}) {
                Boolean value = LunaBridge.bool("wmr_show_" + kind);
                if (value != null) shown.put(kind, value);
            }
            Boolean codex = LunaBridge.bool("wmr_onlyWithoutCodex");
            onlyWithoutCodex = codex == null || codex;
            String list = LunaBridge.string("wmr_vanillaMods");
            vanillaMods = parse(list == null ? DEFAULT_VANILLA_MODS : list);
        } catch (Throwable t) {
            log.error("Which Mod - Renewed: could not read LunaLib settings, using the defaults", t);
        }
    }

    private static Set<String> parse(String list) {
        Set<String> ids = new HashSet<>();
        for (String id : list.split("[,;\\s]+")) {
            if (!id.trim().isEmpty()) ids.add(id.trim().toLowerCase());
        }
        return ids;
    }

    /** Whether the line shows for this kind of thing. All on by default. */
    public static boolean shows(String kind) {
        Boolean value = shown.get(kind);
        return value == null || value;
    }

    /** Skip the line in tooltips that show the codex link, which already names the mod. */
    public static boolean onlyWithoutCodex() {
        return onlyWithoutCodex;
    }

    public static boolean isVanillaMod(String modId) {
        return modId != null && vanillaMods.contains(modId.toLowerCase());
    }
}
