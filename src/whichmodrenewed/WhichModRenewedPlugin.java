package whichmodrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;

/**
 * Indexes every spec once all mods have loaded, and adds the industry tooltip listener to each campaign.
 * The other labels are PatchLib patches in {@code whichmodrenewed.patches}, found by PatchLib on its own.
 */
public class WhichModRenewedPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        SourceLabels.index();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        if (!listeners.hasListenerOfClass(IndustryLabels.class)) {
            // Transient: nothing of this mod goes into the save, so it can be removed at any time.
            listeners.addListener(new IndustryLabels(), true);
        }
    }
}
