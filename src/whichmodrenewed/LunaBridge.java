package whichmodrenewed;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

/** The only class that names LunaLib. Loaded only when LunaLib is enabled (see {@link Settings}). */
final class LunaBridge implements LunaSettingsListener {

    private LunaBridge() {
    }

    static void listen() {
        LunaSettings.addSettingsListener(new LunaBridge());
    }

    static Boolean bool(String key) {
        return LunaSettings.getBoolean(Settings.MOD_ID, key);
    }

    static String string(String key) {
        return LunaSettings.getString(Settings.MOD_ID, key);
    }

    @Override
    public void settingsChanged(String modId) {
        if (Settings.MOD_ID.equals(modId)) {
            Settings.load();
            SourceLabels.clearCaches();
        }
    }
}
