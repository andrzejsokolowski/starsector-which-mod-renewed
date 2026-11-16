package whichmodrenewed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.loading.WithSourceMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Finds which mod something comes from (the same {@link WithSourceMod#getSourceMod()} the codex reads)
 * and writes the "Mod: X" line. Things the game ships itself have no source mod and read "Vanilla".
 */
public final class SourceLabels {

    public static final String VANILLA = "Vanilla";
    public static final String PREFIX = "Mod: ";
    /** The icon the codex shows next to an entry's mod. Part of the game's own files. */
    public static final String ICON = "graphics/ui/icons/codex_mod_info.png";

    private static final Logger log = Global.getLogger(SourceLabels.class);

    private static final Map<Object, HullModSpecAPI> hullModsByEffect = new IdentityHashMap<>();
    private static final Map<String, WithSourceMod> hulls = new HashMap<>();
    private static final Map<String, WithSourceMod> weapons = new HashMap<>();
    private static final Map<String, WithSourceMod> wings = new HashMap<>();
    private static final Map<String, WithSourceMod> systems = new HashMap<>();
    private static final Map<String, WithSourceMod> commodities = new HashMap<>();
    private static final Map<String, WithSourceMod> hullMods = new HashMap<>();
    private static final Map<String, WithSourceMod> industries = new HashMap<>();
    /** Description UID to its label, or "" for descriptions that get none. */
    private static final Map<String, String> descriptionLabels = new HashMap<>();
    private static boolean indexed = false;
    private static boolean iconReady = false;

    /** Kinds of hook that hit an error, so each logs once and then stands down. */
    private static final Map<String, Boolean> failed = new HashMap<>();
    /** How many labels each kind of hook added this session (logged by the self test). */
    private static final Map<String, Integer> counts = new HashMap<>();
    private static final Map<String, String> lastLabels = new HashMap<>();

    private SourceLabels() {
    }

    /** The mod's display name, or "Vanilla". Percent signs are dropped, some tooltips format their text. */
    public static String name(ModSpecAPI mod) {
        if (mod == null) return VANILLA;
        String name = mod.getName();
        if (name == null || name.trim().isEmpty()) name = mod.getId();
        if (name == null || name.trim().isEmpty()) return VANILLA;
        return name.replace("%", "").trim();
    }

    public static String text(ModSpecAPI mod) {
        return PREFIX + name(mod);
    }

    public static ModSpecAPI sourceOf(Object thing) {
        return thing instanceof WithSourceMod ? ((WithSourceMod) thing).getSourceMod() : null;
    }

    /** Fills the id lookups once, after every mod has loaded its data. Safe to call again. */
    public static synchronized void index() {
        if (indexed) return;
        SettingsAPI settings = Global.getSettings();
        try {
            for (ShipHullSpecAPI spec : settings.getAllShipHullSpecs()) hulls.put(spec.getHullId(), spec);
            for (WeaponSpecAPI spec : settings.getAllWeaponSpecs()) weapons.put(spec.getWeaponId(), spec);
            for (FighterWingSpecAPI spec : settings.getAllFighterWingSpecs()) wings.put(spec.getId(), spec);
            for (ShipSystemSpecAPI spec : settings.getAllShipSystemSpecs()) systems.put(spec.getId(), spec);
            for (CommoditySpecAPI spec : settings.getAllCommoditySpecs()) commodities.put(spec.getId(), spec);
            for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) industries.put(spec.getId(), spec);
            for (HullModSpecAPI spec : settings.getAllHullModSpecs()) {
                hullMods.put(spec.getId(), spec);
                Object effect = spec.getEffect();
                if (effect != null) hullModsByEffect.put(effect, spec);
            }
            indexed = true;
            log.info("Which Mod - Renewed: indexed " + hulls.size() + " hulls, " + weapons.size() + " weapons, "
                    + wings.size() + " wings, " + systems.size() + " ship systems, " + commodities.size()
                    + " commodities, " + industries.size() + " industries, " + hullMods.size() + " hull mods");
        } catch (Throwable t) {
            log.error("Which Mod - Renewed: could not index the game's specs", t);
        }
    }

    private static void prepareIcon() {
        if (iconReady) return;
        iconReady = true;
        try {
            Global.getSettings().loadTexture(ICON);
        } catch (Exception e) {
            log.warn("Which Mod - Renewed: could not load the codex mod icon " + ICON, e);
        }
    }

    /** Adds the codex-style line (mod icon, "Mod: name") at the bottom of a tooltip. */
    public static void addLine(String kind, TooltipMakerAPI tooltip, ModSpecAPI mod, float pad) {
        if (tooltip == null || isFailed(kind)) return;
        String name = name(mod);
        if (defer(kind, tooltip, name, true)) return;
        draw(kind, tooltip, name, pad);
    }

    private static void draw(String kind, TooltipMakerAPI tooltip, String name, float pad) {
        prepareIcon();
        TooltipMakerAPI row = tooltip.beginImageWithText(ICON, 18f);
        row.addPara(PREFIX + "%s", 0f, Misc.getGrayColor(), Misc.getHighlightColor(), name);
        tooltip.addImageWithText(pad);
        count(kind, PREFIX + name);
    }

    /** A game tooltip being filled, and the mod name waiting to go at its bottom. */
    private static final class Build {
        final Object tooltip;
        String kind;
        String name;
        boolean strong;

        Build(Object tooltip) {
            this.tooltip = tooltip;
        }
    }

    private static final ArrayDeque<Build> builds = new ArrayDeque<>();

    /** The game started filling a tooltip (StandardTooltipV2Expandable.createImpl). */
    public static void beginBuild(Object tooltip) {
        if (tooltip instanceof TooltipMakerAPI) builds.push(new Build(tooltip));
    }

    /** The tooltip is filled and about to be sized: the waiting line goes at its bottom now. */
    public static void endBuild(Object tooltip) {
        Build build = pop(tooltip);
        if (build == null || build.name == null || isFailed(build.kind)) return;
        try {
            draw(build.kind, (TooltipMakerAPI) tooltip, build.name, 10f);
        } catch (Throwable t) {
            fail(build.kind, t);
        }
    }

    public static void abortBuild(Object tooltip) {
        pop(tooltip);
    }

    private static Build pop(Object tooltip) {
        while (!builds.isEmpty()) {
            Build build = builds.pop();
            if (build.tooltip == tooltip) return build;
        }
        return null;
    }

    /**
     * Hands a mod name to the tooltip being filled, if there is one (and it is this tooltip, when one is
     * given). Lines from the item, hull mod, industry and other plugin hooks ("strong") win over names
     * found in descriptions, and the first of each kind wins, so a tooltip shows one line.
     */
    public static boolean defer(String kind, Object tooltip, String name, boolean strong) {
        Build build = builds.peek();
        if (build == null) return false;
        if (tooltip != null && build.tooltip != tooltip) return false;
        if (build.name == null || (strong && !build.strong)) {
            build.kind = kind;
            build.name = name;
            build.strong = strong;
        }
        return true;
    }

    public static HullModSpecAPI hullModFor(Object effect) {
        index();
        return effect == null ? null : hullModsByEffect.get(effect);
    }

    /**
     * The mod behind a special item. Blueprints and modspecs name the thing they unlock in the stack data,
     * so a vanilla blueprint of a modded ship reads as that ship's mod.
     */
    public static ModSpecAPI specialItemSource(SpecialItemSpecAPI spec, CargoStackAPI stack) {
        index();
        if (stack != null) {
            SpecialItemData data = stack.getSpecialDataIfSpecial();
            String itemId = data != null ? data.getId() : null;
            String target = data != null ? data.getData() : null;
            if (itemId != null && target != null && !target.isEmpty()) {
                WithSourceMod unlocked = null;
                switch (itemId) {
                    case "ship_bp": unlocked = hulls.get(target); break;
                    case "weapon_bp": unlocked = weapons.get(target); break;
                    case "fighter_bp": unlocked = wings.get(target); break;
                    case "modspec": unlocked = hullMods.get(target); break;
                    case "industry_bp": unlocked = industries.get(target); break;
                    default: break;
                }
                if (unlocked != null) return unlocked.getSourceMod();
            }
        }
        return spec != null ? spec.getSourceMod() : null;
    }

    /**
     * The label for a description read by the game's tooltip code, or null for descriptions that are
     * not about a thing (interface help text, gallery pictures, asteroids).
     */
    public static String labelFor(Description description) {
        if (description == null) return null;
        String uid = description.getUID();
        if (uid == null) return null;
        String cached = descriptionLabels.get(uid);
        if (cached == null) {
            String label = computeLabel(description, uid);
            cached = label == null ? "" : label;
            descriptionLabels.put(uid, cached);
        }
        return cached.isEmpty() ? null : cached;
    }

    private static String computeLabel(Description description, String uid) {
        index();
        if (uid.endsWith("_ACTION_TOOLTIP") || uid.endsWith("_GALLERY") || uid.endsWith("_ASTEROID")) return null;
        WithSourceMod thing = null;
        if (uid.endsWith("_SHIP_SYSTEM")) thing = systems.get(cut(uid, "_SHIP_SYSTEM"));
        else if (uid.endsWith("_SHIP")) thing = hulls.get(cut(uid, "_SHIP"));
        else if (uid.endsWith("_WEAPON")) thing = weapons.get(cut(uid, "_WEAPON"));
        else if (uid.endsWith("_RESOURCE")) thing = commodities.get(cut(uid, "_RESOURCE"));
        ModSpecAPI mod = thing != null ? thing.getSourceMod() : description.getSourceMod();
        return text(mod);
    }

    private static String cut(String uid, String suffix) {
        return uid.substring(0, uid.length() - suffix.length());
    }

    /** A text with the label on its own line after it. An empty text becomes just the label. */
    public static String appendLabel(String text, String label) {
        if (label == null || text == null) return text;
        if (text.contains(label)) return text;
        if (text.trim().isEmpty()) return label;
        return text + "\n\n" + label;
    }

    public static List<String> appendLabel(List<String> paras, String label) {
        if (label == null || paras == null) return paras;
        if (paras.contains(label)) return paras;
        List<String> result = new ArrayList<>(paras);
        result.add(label);
        return result;
    }

    public static boolean isFailed(String kind) {
        return failed.containsKey(kind);
    }

    /** Logs a hook's first error and turns that hook off for the rest of the session. */
    public static void fail(String kind, Throwable t) {
        if (failed.put(kind, Boolean.TRUE) == null) {
            log.error("Which Mod - Renewed: the " + kind + " label hit an error and is off until the game restarts", t);
        }
    }

    public static void count(String kind, String label) {
        Integer n = counts.get(kind);
        counts.put(kind, n == null ? 1 : n + 1);
        lastLabels.put(kind, label);
    }

    public static int countOf(String kind) {
        Integer n = counts.get(kind);
        return n == null ? 0 : n;
    }

    public static String lastLabel(String kind) {
        return lastLabels.get(kind);
    }
}
