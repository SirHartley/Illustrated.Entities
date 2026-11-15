package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.memory.TextDataMemory;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.List;

// runs on 2.0.c and leaves a save for UpgradeChecks. does what the 2.0.c editor and selector did
public final class SeedChecks {
    private static final String[] PLANET_LINES = {"first line", "second line", "", "third line", "", "", ""};
    private static final String[] COLONY_LINES = {"docked line", "", "", "", "", "", ""};

    private SeedChecks() {
    }

    public static List<Step> seed() {
        List<Step> steps = new ArrayList<>();
        steps.add(SeedChecks::text);
        steps.add(SeedChecks::images);
        steps.add(c -> c.reload());
        steps.add(SeedChecks::record);
        steps.add(c -> c.reload());
        steps.add(c -> c.note("seed save written"));
        return steps;
    }

    private static void text(Ctx c) {
        SectorEntityToken t = Global.getSector().getEntityById("jangala");
        TextDataMemory memory = TextDataMemory.getInstance();
        TextDataEntry data = new TextDataEntry(memory.getNexFreetNum(), t.getId());
        for (int i = 0; i < PLANET_LINES.length; i++) {
            data.setString(1, i + 1, PLANET_LINES[i]);
            data.setString(2, i + 1, COLONY_LINES[i]);
        }
        data.apply();
        memory.set(data.descriptionNum, data);
        c.put("seedTextNum", data.descriptionNum);
    }

    private static void images(Ctx c) {
        PlanetAPI colony = Ui.newColony(c, "seedColony");
        PlanetAPI picked = Ui.newColony(c, "seedPicked");
        c.expect(ImageHandler.hasImage(colony) && ImageHandler.hasImage(picked), "seed colonies get an image");

        ImageDataEntry pick = unused();
        ImageHandler.removeImageFrom(picked);
        ImageHandler.setImage(picked, pick, false);
        c.put("seedPickImage", pick.id);

        SectorEntityToken preset = null;
        for (ImageDataEntry entry : Ui.presets()) {
            SectorEntityToken t = Global.getSector().getEntityById(entry.planetId);
            if (ImageHandler.hasImage(t)) preset = t;
        }
        c.expect(preset != null, "preset planet with an image found");
        ImageHandler.removeImageFrom(preset);
        ImageHandler.setImage(preset, unused(), false);
        c.put("seedPreset", preset.getId());
    }

    // read after a reload, the state 2.0.d will find
    private static void record(Ctx c) {
        TextDataEntry data = TextDataMemory.getInstance().get((Integer) c.get("seedTextNum"));
        c.expect(data != null, "seed description saved");
        c.put("seedText1", data.getDescription().getText1());
        c.put("seedText3", data.getDescription().getText3());

        ArrayList<String> images = new ArrayList<>();
        for (SectorEntityToken t : Global.getSector().getEntitiesWithTag(ModPlugin.HAS_INTERACTION_IMAGE)) {
            images.add(t.getId() + ":" + ImageHandler.getImageId(t));
        }
        c.expect(!images.isEmpty(), "seed images recorded (" + images.size() + ")");
        c.put("seedImages", images);
    }

    private static ImageDataEntry unused() {
        ImageDataEntry found = null;
        for (ImageDataEntry entry : Ui.memory().getDataMap().values()) {
            if (entry.planetId.isEmpty() && !entry.isUsed()) found = entry;
        }
        return found;
    }
}
