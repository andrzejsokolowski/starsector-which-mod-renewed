package whichmodrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;

/**
 * Reads the settings and indexes every spec once all mods have loaded, and adds the industry tooltip
 * listener to each campaign. The other labels are PatchLib patches in {@code whichmodrenewed.patches},
 * found by PatchLib on its own.
 */
public class WhichModRenewedPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        Settings.load();
        SourceLabels.index();
        StarSystems.index();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        StarSystems.clearCache();
        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        if (!listeners.hasListenerOfClass(IndustryLabels.class)) {
            // Transient: the listener itself is not saved.
            listeners.addListener(new IndustryLabels(), true);
        }
    }
}
