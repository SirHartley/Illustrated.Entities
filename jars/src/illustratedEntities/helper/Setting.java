package illustratedEntities.helper;

public enum Setting {

    ENABLE_IMAGES("illustrated_entities_images_enable", true),
    ENABLE_PRESET_CORE_WORLDS("illustrated_entities_images_core_worlds", true),
    PRESET_REPLACE_VANILLA("illustrated_entities_images_core_worlds_replace_vanilla", false),
    ENABLE_RANDOM_IMAGES("illustrated_entities_images_random_colonies", false),
    SKIP_CORE("illustrated_entities_images_random_skip_core", false),
    BUTTON_ALWAYS_SHOW("illustrated_entities_images_always_show_button", false),
    ALWAYS_BROAD_MATCH("illustrated_entities_images_broadmatch", true),

    ENABLE_TEXT("illustrated_entities_text_enable", true);

    public final String id;
    public final Object fallback;
    public final int min;
    public final int max;

    Setting(String id, int fallback, int min, int max) {
        this.id = id;
        this.fallback = fallback;
        this.min = min;
        this.max = max;
    }

    Setting(String id, boolean fallback) {
        this.id = id;
        this.fallback = fallback;
        this.min = 0;
        this.max = 1;
    }

    public Object sanitize(Object value) {
        if (fallback instanceof Integer) {
            if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) return fallback;
            return (int) Math.max(min, Math.min(max, Math.round(number.doubleValue())));
        }
        return value instanceof Boolean ? value : fallback;
    }

    public int value() {
        return (Integer) Settings.get(this);
    }

    public boolean enabled() {
        return (Boolean) Settings.get(this);
    }

    public static Setting byId(String id) {
        for (Setting setting : values()) if (setting.id.equals(id)) return setting;
        return null;
    }
}
