package illustratedEntities.helper;

import lunalib.backend.ui.components.LunaUITextFieldWithSlider;
import lunalib.backend.ui.components.base.LunaUIButton;
import lunalib.backend.ui.settings.LunaSettingsData;
import lunalib.backend.ui.settings.LunaSettingsUISettingsPanel;

import java.util.Map;

final class LunaSettingsBridge {

    // luna keeps both pending values and mounted controls after save
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void sync(Map<Setting, Object> values) {
        var ui = LunaSettingsUISettingsPanel.Companion;
        for (var pending : ui.getChangedSettings()) {
            if (!Settings.MOD_ID.equals(pending.getModID())) continue;
            Object value = find(values, pending.getFieldID());
            if (value != null) pending.setData(value);
        }
        for (var element : ui.getAddedElements()) {
            if (!(element.getKey() instanceof LunaSettingsData data)
                    || !Settings.MOD_ID.equals(data.getModID())) continue;
            Object value = find(values, data.getFieldID());
            if (value == null) continue;
            if (element instanceof LunaUIButton button && value instanceof Boolean flag) {
                button.setValue(flag);
            } else if (element instanceof LunaUITextFieldWithSlider slider && value instanceof Number) {
                slider.setValue((Number) value);
                if (slider.getTextField() != null) {
                    slider.getTextField().setValue(value);
                    if (slider.getTextField().getParagraph() != null) {
                        slider.getTextField().getParagraph().setText(value.toString());
                    }
                }
                if (slider.getValueSlider() != null) {
                    slider.getValueSlider().setValue((Number) value);
                    slider.getValueSlider().setSliderPositionByValue((Number) value);
                }
            }
        }
    }

    private static Object find(Map<Setting, Object> values, String id) {
        Setting setting = Setting.byId(id);
        return setting != null ? values.get(setting) : null;
    }

    private LunaSettingsBridge() {
    }
}
