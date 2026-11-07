package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.Setting;
import illustratedEntities.helper.Settings;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.plugins.ModPlugin;
import lunalib.backend.ui.settings.LunaSettingsLoader;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

// settings apply on load, so each change is followed by a reload. run this group last, the settings stay changed until the end
public final class SettingChecks {
    private SettingChecks() {
    }

    public static List<Step> settings() {
        List<Step> steps = new ArrayList<>();
        steps.add(SettingChecks::invalidValueFallsBack);
        steps.add(SettingChecks::replaceVanilla);
        steps.add(SettingChecks::imagesOff);
        steps.add(SettingChecks::randomSkipsCore);
        steps.add(SettingChecks::randomEverywhere);
        steps.add(SettingChecks::buttonAlwaysShown);
        steps.add(c -> {
            c.expect(VisualCustomPanel.getPanel() != null, "always show button adds the panel to an npc market");
            set(Setting.BUTTON_ALWAYS_SHOW, false);
            IllustratedChecks.closeDialog(c);
            c.frames(30);
        });
        steps.add(SettingChecks::textOff);
        steps.add(c -> {
            c.expect(PanelChecks.hasButton("change_interaction_visual"), "image button stays with text edits off");
            c.expect(!PanelChecks.hasButton("desc"), "desc button hidden with text edits off");
            set(Setting.ENABLE_TEXT, true);
            IllustratedChecks.closeDialog(c);
        });
        return steps;
    }

    private static void set(Setting setting, Object value) throws JSONException {
        LunaSettingsLoader.getSettings().get(Settings.MOD_ID).put(setting.id, value);
        Settings.reload();
    }

    private static void invalidValueFallsBack(Ctx c) throws Exception {
        SectorEntityToken vanilla = vanillaPreset();
        c.expect(vanilla != null, "preset planet with a vanilla illustration found");
        c.put("vanilla", vanilla.getId());
        c.put("vanillaPath", IllustratedChecks.path(vanilla));

        set(Setting.ENABLE_IMAGES, "nonsense");
        c.expect(Setting.ENABLE_IMAGES.enabled(), "invalid setting value falls back to the default");
        c.expect(Boolean.TRUE.equals(LunaSettingsLoader.getSettings().get(Settings.MOD_ID).opt(Setting.ENABLE_IMAGES.id)), "invalid setting value replaced in the settings data");

        set(Setting.PRESET_REPLACE_VANILLA, true);
        c.reload();
    }

    private static void replaceVanilla(Ctx c) throws Exception {
        SectorEntityToken t = IllustratedChecks.entity(c, "vanilla");
        c.expect(ImageHandler.hasImage(t), "preset replaces the vanilla illustration with the setting on");
        c.expect(t.getMemoryWithoutUpdate().contains(ModPlugin.VANILLA_VISUAL), "vanilla illustration kept in memory");

        set(Setting.ENABLE_IMAGES, false);
        c.reload();
    }

    private static void imagesOff(Ctx c) throws Exception {
        SectorEntityToken t = IllustratedChecks.entity(c, "vanilla");
        c.expect(Global.getSector().getEntitiesWithTag(ModPlugin.HAS_INTERACTION_IMAGE).isEmpty(), "no planet keeps a mod image with images off");
        c.expect(Global.getSector().getEntitiesWithTag(ModPlugin.ORBITAL_HAS_INTERACTION_IMAGE).isEmpty(), "no station keeps a mod image with images off");
        c.expect(c.get("vanillaPath").equals(IllustratedChecks.path(t)), "vanilla illustration back with images off");

        PlanetAPI planet = PanelChecks.newColony(c, "settingColony");
        c.expect(!ImageHandler.hasImage(planet), "new colony gets no image with images off");

        set(Setting.ENABLE_IMAGES, true);
        set(Setting.PRESET_REPLACE_VANILLA, false);
        set(Setting.ENABLE_RANDOM_IMAGES, true);
        set(Setting.SKIP_CORE, true);
        c.reload();
    }

    private static void randomSkipsCore(Ctx c) throws Exception {
        int core = 0, other = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getPrimaryEntity().getCustomInteractionDialogImageVisual() != null) continue;
            if (market.getContainingLocation().hasTag(Tags.THEME_CORE)) core++;
            else other++;
        }
        c.expect(core > 0, "skip core leaves core worlds without a random image (" + core + " left)");
        c.expect(other == 0, "random images reach every market outside the core (" + other + " missing)");
        c.expect(ImageHandler.hasImage(IllustratedChecks.entity(c, "settingColony")), "player colony gets an image back on load");

        set(Setting.SKIP_CORE, false);
        c.reload();
    }

    private static void randomEverywhere(Ctx c) throws Exception {
        int missing = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getPrimaryEntity().getCustomInteractionDialogImageVisual() == null) missing++;
        }
        c.expect(missing == 0, "random images reach every market (" + missing + " missing)");
        c.expect(!ImageHandler.hasImage(IllustratedChecks.entity(c, "vanilla")), "random images leave vanilla illustrations alone");
        set(Setting.ENABLE_RANDOM_IMAGES, false);
    }

    private static void buttonAlwaysShown(Ctx c) throws Exception {
        set(Setting.BUTTON_ALWAYS_SHOW, true);
        IllustratedChecks.openDialog(c, IllustratedChecks.entity(c, "vanilla"));
    }

    private static void textOff(Ctx c) throws Exception {
        set(Setting.ENABLE_TEXT, false);
        IllustratedChecks.openDialog(c, IllustratedChecks.entity(c, "settingColony"));
    }

    private static SectorEntityToken vanillaPreset() {
        for (ImageDataEntry entry : IllustratedChecks.presets()) {
            SectorEntityToken t = Global.getSector().getEntityById(entry.planetId);
            if (!ImageHandler.hasImage(t) && t.getCustomInteractionDialogImageVisual() != null) return t;
        }
        return null;
    }
}
