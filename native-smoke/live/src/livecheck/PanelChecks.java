package livecheck;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.plugins.ModPlugin;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// presses the real panel buttons with setChecked, the same state change a click makes. mouse and keyboard are in clicks.sh
public final class PanelChecks {
    private static final int SETTLE_FRAMES = 30;

    private PanelChecks() {
    }

    public static List<Step> panels() {
        List<Step> steps = new ArrayList<>();
        steps.add(PanelChecks::openColony);
        steps.add(c -> press(c, "change_interaction_visual"));
        steps.add(c -> c.shot("selector"));
        steps.add(c -> press(c, "showFilter"));
        steps.add(c -> c.shot("selector-filter"));
        steps.add(PanelChecks::applyImage);
        steps.add(c -> press(c, "desc"));
        steps.add(c -> c.shot("editor"));
        steps.add(c -> press(c, "confirm"));
        steps.add(c -> {
            c.expect(TextHandler.getDataForEntity(IllustratedChecks.entity(c, "panelColony")) == null, "apply with empty text saves nothing");
            press(c, "return");
        });
        steps.add(c -> {
            c.expect(hasButton("desc") && hasButton("change_interaction_visual"), "return shows the image panel");
            IllustratedChecks.closeDialog(c);
        });
        return steps;
    }

    static boolean hasButton(String id) {
        return button(id) != null;
    }

    static void press(Ctx c, String id) {
        InteractionDialogCustomPanelPlugin.ButtonEntry entry = button(id);
        c.expect(entry != null, "button " + id + " shown");
        if (entry != null) entry.button.setChecked(!entry.button.isChecked());
        c.frames(SETTLE_FRAMES);
    }

    static PlanetAPI newColony(Ctx c, String key) {
        PlanetAPI planet = Colony.planets(1).get(0);
        Colony.make(planet, 5);
        ListenerUtil.reportPlayerColonizedPlanet(planet);
        c.put(key, planet.getId());
        return planet;
    }

    private static void openColony(Ctx c) {
        PlanetAPI planet = newColony(c, "panelColony");
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        IllustratedChecks.openDialog(c, planet);
    }

    private static void applyImage(Ctx c) {
        SectorEntityToken t = IllustratedChecks.entity(c, "panelColony");
        int current = ImageHandler.getImageId(t);
        String pick = null;
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : buttons()) {
            if (entry.id.endsWith("_set") && !entry.id.equals(current + "_set")) pick = entry.id;
        }
        c.expect(pick != null, "selector lists other images");
        if (pick == null) return;

        int picked = Integer.parseInt(pick.substring(0, pick.indexOf('_')));
        press(c, pick);
        c.hold(() -> ImageHandler.getImageId(t) == picked && t.getMemoryWithoutUpdate().getBoolean(ModPlugin.PLAYER_PICK), 10f, "selector apply sets the picked image");
    }

    private static InteractionDialogCustomPanelPlugin.ButtonEntry button(String id) {
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : buttons()) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<InteractionDialogCustomPanelPlugin.ButtonEntry> buttons() {
        InteractionDialogCustomPanelPlugin plugin = VisualCustomPanel.getPlugin();
        if (plugin == null) return Collections.emptyList();
        try {
            return (List<InteractionDialogCustomPanelPlugin.ButtonEntry>) MethodHandles
                    .privateLookupIn(InteractionDialogCustomPanelPlugin.class, MethodHandles.lookup())
                    .findVarHandle(InteractionDialogCustomPanelPlugin.class, "buttons", List.class)
                    .get(plugin);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No buttons field", e);
        }
    }
}
