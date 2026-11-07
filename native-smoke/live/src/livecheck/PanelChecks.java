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
        steps.add(c -> c.shot("selector"));
        steps.add(c -> Ui.press(c, "showFilter"));
        steps.add(c -> c.shot("selector-filter"));
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
