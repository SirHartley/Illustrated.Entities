package illustratedEntities.helper;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;

public class Settings  {
    public static final String MOD_ID = "illustrated_entities";

    public static final String ENABLE_IMAGES  = "illustrated_entities_images_enable";
    public static final String ENABLE_PRESET_CORE_WORLDS  = "illustrated_entities_images_core_worlds";
    public static final String ENABLE_RANDOM_IMAGES  = "illustrated_entities_images_random_colonies";
    public static final String SKIP_CORE  = "illustrated_entities_images_random_skip_core";
    public static final String BUTTON_ALWAYS_SHOW  = "illustrated_entities_images_always_show_button";
    public static final String ALWAYS_BROAD_MATCH  = "illustrated_entities_images_broadmatch";

    public static final String ENABLE_TEXT  = "illustrated_entities_text_enable";
    public static final String TEXT_LINE_NUM  = "illustrated_entities_text_num_lines"; //int

    public static final String CSV_PATH = Global.getSettings().getString("illent_csv_path");
    public static final String DEFAULT_IMAGE_PATH = Global.getSettings().getString("illent_default_image_path");

    public static int getInt(String id){
        return LunaSettings.getInt(MOD_ID, id);
    }

    public static boolean getBoolean(String id){
        return LunaSettings.getBoolean(MOD_ID, id);
    }
}
