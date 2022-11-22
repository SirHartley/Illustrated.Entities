package illustratedEntities.helper;

import com.fs.starfarer.api.Global;

public class Settings {
    public static final String CSV_PATH = Global.getSettings().getString("illent_csv_path");
    public static final String DEFAULT_IMAGE_PATH = Global.getSettings().getString("illent_default_image_path");
    public static final boolean AVOID_DUPLICATES = Global.getSettings().getBoolean("illent_avoid_duplicates_if_possible");
    public static final boolean RESPECT_FACTION = Global.getSettings().getBoolean("illent_respect_faction_when_picking");
    public static final boolean APPLY_RANDOM = Global.getSettings().getBoolean("illent_apply_images_to_random_colonies");
    public static final boolean APPLY_PRESET = Global.getSettings().getBoolean("illent_apply_pre_set_images");
    public static final boolean ALLOW_NPC_PICKING = Global.getSettings().getBoolean("illent_allow_change_on_NPC_market");
    public static final boolean OVERWRITE_VANILLA =  Global.getSettings().getBoolean("illent_overwrite_vanilla_illustrations");
    public static final boolean DEFAULT_ENABLE_DUPES = Global.getSettings().getBoolean("illent_dupe_selection_default_on");
    public static final boolean ENABLE_TEXT_CHANGER = Global.getSettings().getBoolean("illent_allow_text_edits");
    public static final int LINE_AMT = Global.getSettings().getInt("illent_text_line_num");
}
