package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.ImagePicker;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.helper.Settings;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.memory.TextDataMemory;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.List;

// each step runs once. c.days, c.until and c.reload hold the next step back. keep ids in c.put, it survives a reload
public final class IllustratedChecks {
    private static final String PLANET_TEXT = "live check planet\n\nsecond paragraph";
    private static final String COLONY_TEXT = "live check colony";

    private IllustratedChecks() {
    }

    public static List<Step> images() {
        List<Step> steps = new ArrayList<>();
        steps.add(IllustratedChecks::imageDataMatchesCsv);
        steps.add(IllustratedChecks::presetsApplied);
        steps.add(IllustratedChecks::presetTexturesLoad);
        steps.add(IllustratedChecks::pickerCoversSector);
        steps.add(IllustratedChecks::jumpToPreset);
        steps.add(c -> openDialog(c, entity(c, "preset")));
        steps.add(c -> c.shot("preset-dialog"));
        steps.add(IllustratedChecks::closeDialog);
        steps.add(IllustratedChecks::pickImage);
        steps.add(IllustratedChecks::pickKeptAfterReload);
        return steps;
    }

    public static List<Step> colony() {
        List<Step> steps = new ArrayList<>();
        steps.add(IllustratedChecks::newColonies);
        steps.add(IllustratedChecks::colonyImageAfterReload);
        steps.add(c -> {
            image(entity(c, "colony")).unload();
            openDialog(c, entity(c, "colony"));
        });
        steps.add(IllustratedChecks::colonyDialog);
        steps.add(c -> c.shot("colony-dialog"));
        steps.add(c -> {
            closeDialog(c);
            c.days(1f);
        });
        steps.add(IllustratedChecks::abandon);
        steps.add(IllustratedChecks::decivilize);
        return steps;
    }

    public static List<Step> text() {
        List<Step> steps = new ArrayList<>();
        steps.add(IllustratedChecks::editDescription);
        steps.add(IllustratedChecks::descriptionAfterReload);
        steps.add(IllustratedChecks::deleteDescription);
        return steps;
    }

    private static void imageDataMatchesCsv(Ctx c) throws Exception {
        int rows = Global.getSettings().getMergedSpreadsheetDataForMod("id", Settings.CSV_PATH, ModPlugin.MOD_ID).length();
        c.expect(rows > 1000, "image csv loaded (" + rows + " rows)");
        c.expect(memory().getDataMap().size() == rows, "image data has one entry per csv row");
    }

    private static void presetsApplied(Ctx c) {
        int placed = 0, vanilla = 0, wrong = 0;
        for (ImageDataEntry entry : presets()) {
            SectorEntityToken t = Global.getSector().getEntityById(entry.planetId);
            if (ImageHandler.hasImage(t)) {
                placed++;
                InteractionDialogImageVisual visual = t.getCustomInteractionDialogImageVisual();
                ImageDataEntry shown = image(t);
                if (!entry.planetId.equals(shown.planetId) || visual == null || !shown.getImagePath().equals(visual.getSpriteName())) wrong++;
            } else if (t.getCustomInteractionDialogImageVisual() != null) vanilla++;
            else wrong++;
        }
        c.note("presets placed " + placed + ", vanilla illustration kept " + vanilla);
        c.expect(placed > 0, "presets placed on core worlds");
        c.expect(wrong == 0, "every preset planet shows its preset or vanilla image (" + wrong + " wrong)");
    }

    private static void presetTexturesLoad(Ctx c) {
        int checked = 0, missing = 0;
        for (ImageDataEntry entry : presets()) {
            checked++;
            entry.load();
            if (Global.getSettings().getSprite(entry.getImagePath()).getWidth() <= 0) missing++;
            entry.unload();
        }
        c.expect(checked > 0 && missing == 0, "preset textures load and unload (" + checked + " images, " + missing + " missing)");
    }

    // same mode as applyFittingRandomImageToEntity with default settings, every planet once
    private static void pickerCoversSector(Ctx c) {
        int planets = 0, empty = 0, unknown = 0;
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (PlanetAPI planet : location.getPlanets()) {
                if (planet.isStar()) continue;
                planets++;
                ImagePicker picker = new ImagePicker(planet, true, ImageTags.MatchMode.BROAD, true);
                if (picker.isEmpty()) {
                    empty++;
                    c.note("no image for " + planet.getId());
                } else if (memory().get(picker.pick()) == null) unknown++;
            }
        }
        c.expect(planets > 100, "picker ran on the sector (" + planets + " planets)");
        c.expect(empty == 0, "picker finds an image for every planet (" + empty + " empty)");
        c.expect(unknown == 0, "picker only returns known images (" + unknown + " unknown)");
    }

    private static void jumpToPreset(Ctx c) {
        SectorAPI sector = Global.getSector();
        SectorEntityToken target = null;
        for (ImageDataEntry entry : presets()) {
            SectorEntityToken t = sector.getEntityById(entry.planetId);
            if (ImageHandler.hasImage(t) && t.getContainingLocation() != sector.getCurrentLocation()) target = t;
        }
        c.expect(target != null, "preset planet in another system found");
        if (target == null) return;

        c.put("preset", target.getId());
        ImageDataEntry entry = image(target);
        LocationAPI location = target.getContainingLocation();
        c.expect(!Boolean.TRUE.equals(entry.isLoaded), "preset texture not loaded before the jump");
        sector.doHyperspaceTransition(sector.getPlayerFleet(), sector.getPlayerFleet(), new JumpPointAPI.JumpDestination(target, "live check"));
        c.until(() -> sector.getCurrentLocation() == location && Boolean.TRUE.equals(entry.isLoaded), 30f, "jump loads the images of the new system");
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

    // what the Apply button in ImageSelectorPanel does
    private static void pickImage(Ctx c) {
        SectorEntityToken t = entity(c, "preset");
        ImageDataEntry picked = null;
        for (ImageDataEntry entry : memory().getDataMap().values()) {
            if (entry.planetId.isEmpty() && !entry.isUsed()) picked = entry;
        }
        ImageHandler.removeImageFrom(t);
        ImageHandler.setImage(t, picked, false);
        t.getMemoryWithoutUpdate().set(ModPlugin.PLAYER_PICK, true);
        c.put("pick", picked.id);
        c.reload();
    }

    private static void pickKeptAfterReload(Ctx c) {
        SectorEntityToken t = entity(c, "preset");
        c.expect(ImageHandler.getImageId(t) == (Integer) c.get("pick"), "picked image kept over the preset after reload");
        c.expect(path(t).equals(image(t).getImagePath()), "picked image still shown after reload");
    }

    private static void newColonies(Ctx c) {
        List<PlanetAPI> planets = Colony.planets(2);
        PlanetAPI first = planets.get(0);
        PlanetAPI second = planets.get(1);
        MarketAPI market = Colony.make(first, 5);
        Colony.make(second, 5);
        c.put("colony", first.getId());
        c.put("deciv", second.getId());
        ListenerUtil.reportPlayerColonizedPlanet(first);
        ListenerUtil.reportPlayerColonizedPlanet(second);

        c.expect(market.isPlayerOwned() && !market.isPlanetConditionMarketOnly(), "test colony is a real player market");
        c.expect(ImageHandler.hasImage(first) && ImageHandler.hasImage(second), "new colonies get an image");
        if (!ImageHandler.hasImage(first)) return;

        ImageDataEntry entry = image(first);
        c.put("colonyImage", entry.id);
        c.expect(entry.usedEntityIds.contains(first.getId()), "image marked used by the colony");
        c.expect(path(first).equals(entry.getImagePath()), "colony shows the picked image");
        c.expect(Boolean.TRUE.equals(entry.isLoaded) && Global.getSettings().getSprite(entry.getImagePath()).getWidth() > 0, "colony image texture loaded");

        Colony.jump(first.getContainingLocation(), first.getLocation().x, first.getLocation().y + 300f);
        c.reload();
    }

    private static void colonyImageAfterReload(Ctx c) {
        SectorEntityToken t = entity(c, "colony");
        c.expect(ImageHandler.getImageId(t) == (Integer) c.get("colonyImage"), "colony keeps its image after reload");
        c.expect(path(t).equals(image(t).getImagePath()), "colony still shows its image after reload");
        c.expect(Boolean.TRUE.equals(image(t).isLoaded), "images of the current location load after reload");
    }

    private static void colonyDialog(Ctx c) {
        SectorEntityToken t = entity(c, "colony");
        c.expect(Boolean.TRUE.equals(image(t).isLoaded), "opening the market loads the image");
        c.expect(VisualCustomPanel.getPanel() != null, "player colony shows the image panel");
    }

    private static void abandon(Ctx c) {
        c.expect(VisualCustomPanel.getPanel() == null, "image panel cleared after the dialog");

        SectorEntityToken t = entity(c, "colony");
        ListenerUtil.reportPlayerAbandonedColony(t.getMarket());
        c.expect(!ImageHandler.hasImage(t), "abandoned colony loses its image tag");
        c.expect(t.getCustomInteractionDialogImageVisual() == null, "abandoned colony has no visual left");
    }

    private static void decivilize(Ctx c) {
        SectorEntityToken t = entity(c, "deciv");
        c.expect(ImageHandler.hasImage(t), "colony has an image before decivilizing");
        DecivTracker.decivilize(t.getMarket(), false, false);
        c.expect(t.getMarket().isPlanetConditionMarketOnly(), "colony decivilized");
        c.expect(!ImageHandler.hasImage(t), "decivilized colony loses its image");
    }

    // what the Apply button in TextChangerPanel does
    private static void editDescription(Ctx c) {
        SectorEntityToken t = Global.getSector().getEntityById("jangala");
        c.expect(t != null && t.getMarket() != null && !t.getMarket().isPlanetConditionMarketOnly(), "jangala is a real market");
        c.expect(t.getCustomDescriptionId() != null, "jangala has a vanilla description id");
        c.put("textOriginal", t.getCustomDescriptionId());

        TextDataMemory memory = TextDataMemory.getInstance();
        TextDataEntry data = new TextDataEntry(memory.getNexFreetNum(), t.getId());
        data.setText(1, PLANET_TEXT);
        data.setText(2, COLONY_TEXT);
        c.expect(data.isValid(), "edited description is valid");
        data.apply();
        memory.set(data.descriptionNum, data);

        c.expect(data.getDescriptionID().equals(t.getCustomDescriptionId()), "planet uses the edited description");
        c.expect(PLANET_TEXT.equals(data.getDescription().getText1()), "planet text written");
        c.expect(COLONY_TEXT.equals(data.getDescription().getText3()), "colony text written");
        c.expect(TextHandler.getDataForEntity(t) == data, "entity finds its edited description");

        // descriptions are not saved, onGameLoad has to write them back
        data.getDescription().setText1("stale");
        c.put("textNum", data.descriptionNum);
        c.reload();
    }

    private static void descriptionAfterReload(Ctx c) {
        SectorEntityToken t = Global.getSector().getEntityById("jangala");
        TextDataEntry data = TextDataMemory.getInstance().get((Integer) c.get("textNum"));
        c.expect(data != null && PLANET_TEXT.equals(data.getText(1)) && COLONY_TEXT.equals(data.getText(2)), "edited text saved");
        if (data == null) return;
        c.expect(data.getDescriptionID().equals(t.getCustomDescriptionId()), "planet keeps the edited description after reload");
        c.expect(PLANET_TEXT.equals(data.getDescription().getText1()), "description text written back on load");
    }

    private static void deleteDescription(Ctx c) {
        SectorEntityToken t = Global.getSector().getEntityById("jangala");
        int num = (Integer) c.get("textNum");
        c.expect(TextHandler.deleteDescription(t), "edited description deleted");
        c.expect(TextDataMemory.getInstance().get(num) == null, "description slot freed");
        c.expect(!t.getMemoryWithoutUpdate().contains(TextDataEntry.ID_MEM_KEY), "text memory key removed");
        c.expect(c.get("textOriginal").equals(t.getCustomDescriptionId()), "vanilla description back after delete (got " + t.getCustomDescriptionId() + ")");
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
}
