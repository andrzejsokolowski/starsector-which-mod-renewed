package whichmodrenewed.patches;

import java.util.ArrayDeque;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.context.HookContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import whichmodrenewed.Settings;
import whichmodrenewed.SourceLabels;

/**
 * The game's plain fleet member tooltips: the ship tooltip of the recovery and deployment screens
 * (StandardTooltipV2.createFleetMemberTooltipPreDeploy). They are not expandable tooltips, so
 * {@link TooltipBuildPatch} never sees them, and they show no description, so the description redirect
 * adds nothing either.
 *
 * Such a tooltip clears and refills its content panel in beforeShown, sets the panel's size, then calls
 * updateSize (the window takes the panel's size) and the base beforeShown (which writes the codex footer;
 * Random Assortment of Things adds the mod name to it there). So:
 * - Before updateSize, inside beforeShown: the line goes at the bottom of the content panel and the panel
 *   grows by its height. The "Requires N story points to recover" label sits at the panel's bottom edge,
 *   so the line goes just above it when it is there.
 * - After beforeShown: if the footer now names the mod, the line comes out again and the window shrinks
 *   back (only with Only without a codex link on).
 * A ship line found in a description during the same beforeShown means the tooltip already has one.
 */
@Patch(target = @ClassMatch(subtypeName = "com.fs.starfarer.ui.impl.StandardTooltipV2"))
public class FleetMemberTooltipPatch {

    static final String HOOK = "fleetMemberTooltip";

    /** A plain tooltip running beforeShown, and the line added to it, if any. */
    static final class Shown {
        final Object tooltip;
        final int shipLinesBefore;
        boolean tried;
        UIPanelAPI panel;
        UIComponentAPI line;
        float width;
        float height;

        Shown(Object tooltip) {
            this.tooltip = tooltip;
            this.shipLinesBefore = SourceLabels.countOf(Settings.SHIPS);
        }
    }

    private static final ArrayDeque<Shown> shown = new ArrayDeque<>();

    @Before(target = @MethodMatch(methodName = "beforeShown", parameterCount = 0))
    public static void beforeShown(BeforeContext context) {
        if (!context.isMostDerivedCall()) return;
        Object self = context.getSelf();
        // Expandable tooltips (tooltip makers) get their line from TooltipBuildPatch.
        if (self instanceof TooltipMakerAPI || Global.CODEX_TOOLTIP_MODE) return;
        if (SourceLabels.isFailed(HOOK) || !Settings.shows(Settings.SHIPS)) return;
        shown.push(new Shown(self));
    }

    @After(target = @MethodMatch(methodName = "beforeShown", parameterCount = 0))
    public static void afterShown(AfterContext context) {
        if (!context.isMostDerivedCall()) return;
        Shown entry = pop(context.getSelf());
        if (entry == null || entry.line == null) return;
        try {
            if (!Settings.onlyWithoutCodex() || !footerNamesMod(context)) return;
            entry.panel.removeComponent(entry.line);
            entry.panel.getPosition().setSize(entry.width, entry.height);
            context.getMethod("updateSize").call();
        } catch (Throwable t) {
            SourceLabels.fail(HOOK, t);
        }
    }

    @Except(target = @MethodMatch(methodName = "beforeShown", parameterCount = 0))
    public static void onError(ExceptContext context) {
        if (!context.isMostDerivedCall()) return;
        pop(context.getSelf());
    }

    /** Called from {@link FleetMemberTooltipSizePatch} when a tooltip in beforeShown is about to take its content panel's size. */
    static void beforeSizing(HookContext context) {
        Shown entry = shown.peek();
        if (entry == null || entry.tooltip != context.getSelf() || entry.tried) return;
        entry.tried = true;
        try {
            addLine(context, entry);
        } catch (Throwable t) {
            SourceLabels.fail(HOOK, t);
        }
    }

    private static void addLine(HookContext context, Shown entry) {
        if (SourceLabels.countOf(Settings.SHIPS) != entry.shipLinesBefore) return;
        Object member = field(context, "codexFleetMember");
        if (!(member instanceof FleetMemberAPI) || ((FleetMemberAPI) member).isFighterWing()) return;
        Object contents = context.getMethod("getContents").call();
        if (!(contents instanceof UIPanelAPI)) return;

        UIPanelAPI panel = (UIPanelAPI) contents;
        float width = panel.getPosition().getWidth();
        float height = panel.getPosition().getHeight();
        if (width < 50f || height <= 0f) return;

        String name = SourceLabels.name(SourceLabels.shipSource((FleetMemberAPI) member));
        UIComponentAPI bottom = storyPointLabel(panel);
        CustomPanelAPI line = SourceLabels.linePanel(Settings.SHIPS, name, width - 10f);
        float lineHeight = line.getPosition().getHeight();

        panel.getPosition().setSize(width, height + lineHeight + 8f);
        PositionAPI pos = panel.addComponent(line);
        if (bottom != null) pos.aboveLeft(bottom, 4f);
        else pos.inBL(5f, 3f);

        entry.panel = panel;
        entry.line = line;
        entry.width = width;
        entry.height = height;
    }

    /** The label the game puts at the panel's bottom edge for ships that need story points to recover. */
    private static UIComponentAPI storyPointLabel(UIPanelAPI panel) {
        for (UIComponentAPI child : panel.getChildrenCopy2()) {
            if (!(child instanceof LabelAPI)) continue;
            String text = ((LabelAPI) child).getText();
            if (text != null && text.startsWith("Requires") && text.contains("story point")) return child;
        }
        return null;
    }

    /** The codex footer as written by the game and other mods. The game's own footer names no mod. */
    private static boolean footerNamesMod(HookContext context) {
        Object label = field(context, "expandLabel");
        if (!(label instanceof LabelAPI)) return false;
        String text = ((LabelAPI) label).getText();
        return text != null && text.contains(" - ");
    }

    private static Object field(HookContext context, String name) {
        try {
            return context.hasField(name) ? context.getField(name).get() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Shown pop(Object tooltip) {
        while (!shown.isEmpty()) {
            Shown entry = shown.pop();
            if (entry.tooltip == tooltip) return entry;
        }
        return null;
    }
}
