package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// also compiled against the 2.0.c jar, so only use what both builds have
public final class Ui {
    private static final int SETTLE_FRAMES = 30;

    private Ui() {
    }

    static ImageDataMemory memory() {
        return ImageDataMemory.getInstance();
    }

    static List<ImageDataEntry> presets() {
        List<ImageDataEntry> presets = new ArrayList<>();
        for (ImageDataEntry entry : memory().getDataMap().values()) {
            if (entry.planetId.isEmpty()) continue;
            SectorEntityToken t = Global.getSector().getEntityById(entry.planetId);
            if (t != null && t.getMarket() != null && !t.getMarket().isPlanetConditionMarketOnly()) presets.add(entry);
        }
        return presets;
    }

    static SectorEntityToken entity(Ctx c, String key) {
        return Global.getSector().getEntityById((String) c.get(key));
    }

    static ImageDataEntry image(SectorEntityToken t) {
        return memory().get(ImageHandler.getImageId(t));
    }

    static String path(SectorEntityToken t) {
        return t.getCustomInteractionDialogImageVisual().getSpriteName();
    }

    static void openDialog(Ctx c, SectorEntityToken t) {
        c.keepDialog = true;
        c.expect(Global.getSector().getCampaignUI().showInteractionDialog(t), "market dialog opens on " + t.getId());
        c.frames(90);
    }

    static void closeDialog(Ctx c) {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = ui.getCurrentInteractionDialog();
        c.expect(dialog != null, "market dialog stayed open");
        c.keepDialog = false;
        if (dialog != null) dialog.dismiss();
    }

    static PlanetAPI newColony(Ctx c, String key) {
        PlanetAPI planet = Colony.planets(1).get(0);
        Colony.make(planet, 5);
        ListenerUtil.reportPlayerColonizedPlanet(planet);
        c.put(key, planet.getId());
        return planet;
    }

    static boolean hasButton(String id) {
        return button(id) != null;
    }

    // setChecked is the same state change a click makes, ClickChecks does the real clicks
    static void press(Ctx c, String id) {
        InteractionDialogCustomPanelPlugin.ButtonEntry entry = button(id);
        c.expect(entry != null, "button " + id + " shown");
        if (entry != null) entry.button.setChecked(!entry.button.isChecked());
        c.frames(SETTLE_FRAMES);
    }

    static InteractionDialogCustomPanelPlugin.ButtonEntry button(String id) {
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : buttons()) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<InteractionDialogCustomPanelPlugin.ButtonEntry> buttons() {
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
