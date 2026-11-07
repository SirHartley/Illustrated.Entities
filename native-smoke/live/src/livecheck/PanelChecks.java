package livecheck;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.ImagePicker;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PanelChecks {
    private PanelChecks() {
    }

    public static List<Step> panels() {
        List<Step> steps = new ArrayList<>();
        steps.add(PanelChecks::openColony);
        steps.add(c -> Ui.press(c, "change_interaction_visual"));
        steps.add(PanelChecks::selectorOrder);
        steps.add(PanelChecks::loadsVisibleOnly);
        steps.add(c -> c.shot("selector"));
        steps.add(c -> {
            Perf.holdImageLoads = true;
            Ui.press(c, "showFilter");
        });
        steps.add(c -> c.shot("selector-loading"));
        steps.add(c -> {
            Perf.holdImageLoads = false;
            c.frames(60);
        });
        steps.add(c -> c.shot("selector-filter"));
        steps.add(PanelChecks::previewLoads);
        steps.add(PanelChecks::applyImage);
        steps.add(c -> Ui.press(c, "desc"));
        steps.add(c -> c.shot("editor"));
        steps.add(c -> Ui.press(c, "confirm"));
        steps.add(c -> {
            c.expect(TextHandler.getDataForEntity(Ui.entity(c, "panelColony")) == null, "apply with empty text saves nothing");
            Ui.press(c, "return");
        });
        steps.add(c -> {
            c.expect(Ui.hasButton("desc") && Ui.hasButton("change_interaction_visual"), "return shows the image panel");
            Ui.closeDialog(c);
        });
        return steps;
    }

    private static void openColony(Ctx c) {
        PlanetAPI planet = Ui.newColony(c, "panelColony");
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        Ui.openDialog(c, planet);
    }

    private static void selectorOrder(Ctx c) {
        Map<Integer, Integer> scores = ImagePicker.generateChoices(ImagePicker.getTags(Ui.entity(c, "panelColony")), null, true, ImageTags.MatchMode.BROAD);
        int shown = 0, wrong = 0, last = Integer.MAX_VALUE;
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : Ui.buttons()) {
            if (!entry.id.endsWith("_set")) continue;
            shown++;
            Integer score = scores.get(Integer.parseInt(entry.id.substring(0, entry.id.indexOf('_'))));
            if (score == null || score > last) wrong++;
            else last = score;
        }
        c.expect(shown > 0 && wrong == 0, "selector lists the best matches first (" + shown + " images, " + wrong + " out of order)");
    }

    // the scroller only renders rows in view, so rows below the fold stay unloaded
    private static void loadsVisibleOnly(Ctx c) {
        List<Integer> ids = listed();
        int loaded = 0, top = 0;
        for (int i = 0; i < ids.size(); i++) {
            boolean in = Boolean.TRUE.equals(Ui.memory().get(ids.get(i)).isLoaded);
            if (in) loaded++;
            if (in && i < 4) top++;
        }
        c.expect(top == Math.min(4, ids.size()), "the first two rows load on their own (" + top + " of 4)");
        c.expect(ids.size() <= 16 || loaded < ids.size(), "selector loads only the images in view (" + loaded + " of " + ids.size() + ")");
    }

    private static void previewLoads(Ctx c) {
        int id = -1;
        for (int listed : listed()) {
            if (!Boolean.TRUE.equals(Ui.memory().get(listed).isLoaded)) id = listed;
        }
        c.expect(id >= 0, "an image below the fold is not loaded yet");
        if (id < 0) return;
        int preview = id;
        Ui.press(c, preview + "_preview");
        c.hold(() -> Boolean.TRUE.equals(Ui.memory().get(preview).isLoaded), 10f, "preview loads its image");
    }

    private static List<Integer> listed() {
        List<Integer> ids = new ArrayList<>();
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : Ui.buttons()) {
            if (entry.id.endsWith("_set")) ids.add(Integer.parseInt(entry.id.substring(0, entry.id.indexOf('_'))));
        }
        return ids;
    }

    private static void applyImage(Ctx c) {
        SectorEntityToken t = Ui.entity(c, "panelColony");
        int current = ImageHandler.getImageId(t);
        String pick = null;
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : Ui.buttons()) {
            if (entry.id.endsWith("_set") && !entry.id.equals(current + "_set")) pick = entry.id;
        }
        c.expect(pick != null, "selector lists other images");
        if (pick == null) return;

        int picked = Integer.parseInt(pick.substring(0, pick.indexOf('_')));
        Ui.press(c, pick);
        c.hold(() -> ImageHandler.getImageId(t) == picked && t.getMemoryWithoutUpdate().getBoolean(ModPlugin.PLAYER_PICK), 10f, "selector apply sets the picked image");
    }
}
