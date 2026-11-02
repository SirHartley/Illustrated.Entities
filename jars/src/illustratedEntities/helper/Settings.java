package illustratedEntities.helper;

import com.fs.starfarer.api.Global;
import illustratedEntities.plugins.ModPlugin;
import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.json.JSONException;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class Settings implements LunaSettingsListener {
    public static final String MOD_ID = "illustrated_entities";

    // tasc copies this many TextDataEntry lines, the editor only fills line 1
    public static final int LINE_AMT = 7;
    public static final String TEXT_LINE_NUM = "illustrated_entities_text_num_lines";

    public static final String CSV_PATH = Global.getSettings().getString("illent_csv_path");
    public static final String DEFAULT_IMAGE_PATH = Global.getSettings().getString("illent_default_image_path");

    private static Map<Setting, Object> current = defaults();

    public static void install() {
        if (!LunaSettings.hasSettingsListenerOfClass(Settings.class)) {
            LunaSettings.addSettingsListener(new Settings());
        }
        reload();
    }

    public static Object get(Setting setting) {
        return current.getOrDefault(setting, setting.fallback);
    }

    public static Map<Setting, Object> defaults() {
        Map<Setting, Object> result = new EnumMap<>(Setting.class);
        for (Setting setting : Setting.values()) result.put(setting, setting.fallback);
        return result;
    }

    @Override
    public void settingsChanged(String modId) {
        if (MOD_ID.equals(modId)) reload();
    }

    public static void reload() {
        // luna loads lazily, a get on our own id loads it without reloading other mods
        LunaSettings.getBoolean(MOD_ID, Setting.ENABLE_IMAGES.id);
        var data = LunaSettingsLoader.getSettings().get(MOD_ID);
        if (data == null) return;
        Map<Setting, Object> result = new EnumMap<>(Setting.class);
        for (Setting key : Setting.values()) result.put(key, key.sanitize(data.opt(key.id)));
        current = result;
        try {
            boolean changed = false;
            for (Setting key : Setting.values()) {
                Object value = current.get(key);
                if (value.equals(data.opt(key.id))) continue;
                data.put(key.id, value);
                changed = true;
            }
            if (changed) data.save();
        } catch (IOException | JSONException ex) {
            ModPlugin.log.error("Could not save Illustrated.Entities settings", ex);
        }
        LunaSettingsBridge.sync(current);
    }

    // public api for other mods, tasc calls getInt
    public static int getInt(String id) {
        if (TEXT_LINE_NUM.equals(id)) return LINE_AMT;
        Setting setting = Setting.byId(id);
        return setting != null ? setting.value() : 0;
    }

    public static boolean getBoolean(String id) {
        Setting setting = Setting.byId(id);
        return setting != null && setting.enabled();
    }

    private Settings() {
    }
}
