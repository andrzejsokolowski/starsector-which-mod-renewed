package wmrselftest;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import whichmodrenewed.SourceLabels;

/**
 * Load-test only, never shipped. At application load it runs the game's own tooltip code for a few
 * vanilla and modded things and records the label Which Mod - Renewed added to each, into
 * saves/common/wmr_selftest (JSON). Kinds that need a running campaign (industries, conditions,
 * abilities, terrain, fleet ships) are not covered here.
 */
public class SelfTestPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(SelfTestPlugin.class);
    private final JSONArray results = new JSONArray();

    @Override
    public void onApplicationLoad() {
        try {
            SourceLabels.index();
            SettingsAPI s = Global.getSettings();

            List<String> items = new ArrayList<>();
            items.add("pristine_nanoforge");
            items.add("synchrotron");
            for (SpecialItemSpecAPI spec : s.getAllSpecialItemSpecs()) {
                if (spec.getSourceMod() != null && items.size() < 12) items.add(spec.getId());
            }
            for (String id : items) specialItem(id, null);
            ShipHullSpecAPI moddedHull = firstModded(s.getAllShipHullSpecs());
            WeaponSpecAPI moddedWeapon = firstModded(s.getAllWeaponSpecs());
            HullModSpecAPI moddedHullMod = firstModded(s.getAllHullModSpecs());
            specialItem("ship_bp", "onslaught");
            if (moddedHull != null) specialItem("ship_bp", moddedHull.getHullId());
            if (moddedWeapon != null) specialItem("weapon_bp", moddedWeapon.getWeaponId());
            if (moddedHullMod != null) specialItem("modspec", moddedHullMod.getId());
            specialItem("industry_bp", "orbitalworks");

            hullMod("heavyarmor");
            hullMod("augmentedengines");
            for (HullModSpecAPI spec : s.getAllHullModSpecs()) {
                if (spec.getSourceMod() != null && countKind("hullmod") < 6) hullMod(spec.getId());
            }

            shipSystem("fastmissileracks");
            ShipSystemSpecAPI moddedSystem = firstModded(s.getAllShipSystemSpecs());
            if (moddedSystem != null) shipSystem(moddedSystem.getId());

            weapon("lightmg");
            if (moddedWeapon != null) weapon(moddedWeapon.getWeaponId());

            JSONObject out = new JSONObject();
            out.put("results", results);
            JSONObject counts = new JSONObject();
            for (String kind : new String[] {"items", "hullMods", "ships", "weapons", "shipSystems", "commodities",
                    "industries", "conditions", "skills", "abilities", "terrain", "starSystems"}) {
                counts.put(kind, SourceLabels.countOf(kind));
                if (SourceLabels.isFailed(kind)) counts.put(kind + " FAILED", true);
            }
            out.put("counts", counts);
            s.writeTextFileToCommon("wmr_selftest", out.toString(2));
            log.info("WMR self test done: " + out.toString());
        } catch (Throwable t) {
            log.error("WMR self test crashed", t);
            try {
                Global.getSettings().writeTextFileToCommon("wmr_selftest", "CRASH " + t);
            } catch (Throwable ignored) {
            }
        }
    }

    private int countKind(String kind) {
        int n = 0;
        for (int i = 0; i < results.length(); i++) {
            if (kind.equals(results.optJSONObject(i).optString("kind"))) n++;
        }
        return n;
    }

    private static <T extends com.fs.starfarer.api.loading.WithSourceMod> T firstModded(List<T> specs) {
        for (T spec : specs) if (spec.getSourceMod() != null) return spec;
        return null;
    }

    private TooltipMakerAPI tooltip() {
        CustomPanelAPI panel = Global.getSettings().createCustom(500f, 800f, null);
        return panel.createUIElement(500f, 800f, false);
    }

    private void record(String kind, String id, String expect, String countKind, int before, Throwable error) {
        try {
            JSONObject r = new JSONObject();
            r.put("kind", kind);
            r.put("id", id);
            r.put("expected", expect);
            int after = SourceLabels.countOf(countKind);
            r.put("labelsAdded", after - before);
            r.put("label", after > before ? SourceLabels.lastLabel(countKind) : JSONObject.NULL);
            r.put("ok", after > before && expect.equals(SourceLabels.lastLabel(countKind)));
            if (error != null) r.put("error", error.toString());
            results.put(r);
        } catch (Throwable t) {
            log.error("record failed", t);
        }
    }

    private void specialItem(String id, String data) {
        int before = SourceLabels.countOf("items");
        Throwable error = null;
        String expect = "?";
        try {
            CargoStackAPI stack = Global.getFactory().createCargoStack(CargoItemType.SPECIAL,
                    new SpecialItemData(id, data), null);
            SpecialItemPlugin plugin = stack.getPlugin();
            expect = SourceLabels.text(SourceLabels.specialItemSource(plugin.getSpec(), stack));
            plugin.createTooltip(tooltip(), false, null, null);
        } catch (Throwable t) {
            error = t;
        }
        record("special item", data == null ? id : id + ":" + data, expect, "items", before, error);
    }

    private void hullMod(String id) {
        int before = SourceLabels.countOf("hullMods");
        Throwable error = null;
        String expect = "?";
        try {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            expect = SourceLabels.text(spec.getSourceMod());
            spec.getEffect().addPostDescriptionSection(tooltip(), HullSize.CRUISER, null, 500f, false);
        } catch (Throwable t) {
            error = t;
        }
        record("hullmod", id, expect, "hullMods", before, error);
    }

    /** StandardTooltipV2.createShipSystemTooltip, the game's own ship system tooltip (reads a Description). */
    private void shipSystem(String id) {
        String kindFor = "shipSystems";
        int before = SourceLabels.countOf(kindFor);
        Throwable error = null;
        String expect = "?";
        try {
            ShipSystemSpecAPI spec = Global.getSettings().getShipSystemSpec(id);
            expect = SourceLabels.text(spec.getSourceMod());
            Class<?> tooltips = Class.forName("com.fs.starfarer.ui.impl.StandardTooltipV2");
            MethodHandle create = MethodHandles.publicLookup().findStatic(tooltips, "createShipSystemTooltip",
                    MethodType.methodType(tooltips, spec.getClass()));
            create.invoke(spec);
        } catch (Throwable t) {
            error = t;
        }
        record("ship system", id, expect, kindFor, before, error);
    }

    /** StandardTooltipV2.createWeaponTooltip, as the refit weapon picker and cargo use it. */
    private void weapon(String id) {
        String kindFor = "weapons";
        int before = SourceLabels.countOf(kindFor);
        Throwable error = null;
        String expect = "?";
        try {
            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
            expect = SourceLabels.text(spec.getSourceMod());
            Class<?> tooltips = Class.forName("com.fs.starfarer.ui.impl.StandardTooltipV2");
            Class<?> base = Class.forName("com.fs.starfarer.loading.specs.BaseWeaponSpec");
            Class<?> stats = Class.forName("com.fs.starfarer.campaign.CharacterStats");
            Class<?> stack = Class.forName("com.fs.starfarer.campaign.ui.trade.CargoItemStack");
            Class<?> grid = Class.forName("com.fs.starfarer.campaign.ui.trade.CargoDataGridView$o");
            Class<?> e = Class.forName("com.fs.starfarer.campaign.ui.trade.E");
            MethodHandle create = MethodHandles.publicLookup().findStatic(tooltips, "createWeaponTooltip",
                    MethodType.methodType(tooltips, base, stats, stack, grid, e));
            Object stats0 = Global.getFactory().createPerson().getStats();
            Object tooltip = create.invoke(spec, stats0, null, null, null);
            // The content is built when the tooltip is first shown; build it now the same way.
            MethodHandle build = MethodHandles.publicLookup().findVirtual(tooltip.getClass(), "create",
                    MethodType.methodType(void.class, boolean.class));
            build.invoke(tooltip, false);
        } catch (Throwable t) {
            error = t;
        }
        record("weapon", id, expect, kindFor, before, error);
    }
}
