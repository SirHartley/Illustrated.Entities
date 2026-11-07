package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.Setting;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.memory.TextDataMemory;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.List;

// loads the save SeedChecks wrote on 2.0.c. the checks hold 2.0.d to what the changelog Note lines promise
public final class UpgradeChecks {
    private static final String NEW_TEXT = "edited after the update";

    private UpgradeChecks() {
    }

    public static List<Step> upgrade() {
        List<Step> steps = new ArrayList<>();
        steps.add(UpgradeChecks::imagesKept);
        steps.add(UpgradeChecks::oldTextKept);
        steps.add(c -> Ui.openDialog(c, Ui.entity(c, "seedPicked")));
        steps.add(c -> {
            c.expect(Ui.hasButton("change_interaction_visual"), "old colony shows the image panel");
            Ui.closeDialog(c);
        });
        steps.add(UpgradeChecks::pickAgain);
        steps.add(c -> c.expect(ImageHandler.getImageId(Ui.entity(c, "seedPreset")) == (Integer) c.get("upgradePick"), "image picked after the update stays over the preset"));
        steps.add(UpgradeChecks::editOldText);
        steps.add(UpgradeChecks::editedTextAfterReload);
        steps.add(UpgradeChecks::deleteOldText);
        steps.add(c -> Ui.openDialog(c, Global.getSector().getEntityById("jangala")));
        steps.add(Ui::closeDialog);
        steps.add(UpgradeChecks::imagesOff);
        steps.add(UpgradeChecks::imagesOffResult);
        steps.add(c -> c.expect(ImageHandler.hasImage(Ui.entity(c, "seedPicked")), "old colony gets an image back with images on"));
        steps.add(UpgradeChecks::abandon);
        return steps;
    }

    @SuppressWarnings("unchecked")
    private static List<String> seedImages(Ctx c) {
        return (List<String>) c.get("seedImages");
    }

    private static void imagesKept(Ctx c) {
        String preset = (String) c.get("seedPreset");
        int kept = 0, changed = 0;
        for (String record : seedImages(c)) {
            String id = record.substring(0, record.indexOf(':'));
            SectorEntityToken t = Global.getSector().getEntityById(id);
            if (t != null && ImageHandler.hasImage(t) && record.equals(id + ":" + ImageHandler.getImageId(t))) kept++;
            else {
                changed++;
                c.note("image changed on " + id + ": " + record + " now " + (t == null ? "gone" : ImageHandler.getImageId(t)));
            }
        }
        c.expect(kept > 0 && changed == 0, "every image from the old save kept (" + kept + " kept, " + changed + " changed)");

        SectorEntityToken picked = Ui.entity(c, "seedPicked");
        c.expect(ImageHandler.getImageId(picked) == (Integer) c.get("seedPickImage"), "pick on a colony without a preset kept");
        c.expect(Ui.path(picked).equals(Ui.image(picked).getImagePath()), "old colony shows its image");

        SectorEntityToken t = Global.getSector().getEntityById(preset);
        c.expect(preset.equals(Ui.image(t).planetId), "pick on a preset planet is back to the preset, as the changelog says");
    }

    private static void oldTextKept(Ctx c) {
        SectorEntityToken t = Global.getSector().getEntityById("jangala");
        TextDataEntry data = TextDataMemory.getInstance().get((Integer) c.get("seedTextNum"));
        c.expect(data != null, "old description found");
        if (data == null) return;
        c.expect(data.getDescriptionID().equals(t.getCustomDescriptionId()), "old description still on the planet");
        c.expect(c.get("seedText1").equals(data.getText(1)) && c.get("seedText3").equals(data.getText(2)), "old lines join to the text 2.0.c showed (" + data.getText(1) + ")");
        c.expect(c.get("seedText1").equals(data.getDescription().getText1()), "old text written back on load");
        c.expect(!t.getMemoryWithoutUpdate().contains(ModPlugin.VANILLA_DESCRIPTION), "no vanilla id known for an old description");
    }

    private static void pickAgain(Ctx c) {
        SectorEntityToken t = Ui.entity(c, "seedPreset");
        ImageDataEntry pick = null;
        for (ImageDataEntry entry : Ui.memory().getDataMap().values()) {
            if (entry.planetId.isEmpty() && !entry.isUsed()) pick = entry;
        }
        ImageHandler.removeImageFrom(t);
        ImageHandler.setImage(t, pick, false);
        t.getMemoryWithoutUpdate().set(ModPlugin.PLAYER_PICK, true);
        c.put("upgradePick", pick.id);
        c.reload();
    }

    private static void editOldText(Ctx c) {
        TextDataEntry data = TextDataMemory.getInstance().get((Integer) c.get("seedTextNum"));
        data.setText(1, NEW_TEXT);
        data.apply();
        c.reload();
    }

    private static void editedTextAfterReload(Ctx c) {
        TextDataEntry data = TextDataMemory.getInstance().get((Integer) c.get("seedTextNum"));
        c.expect(NEW_TEXT.equals(data.getText(1)), "old description edited on 2.0.d stays after reload");
        c.expect(data.stringMap1.size() == 1, "edit drops the old lines");
        c.expect(c.get("seedText3").equals(data.getText(2)), "untouched old text stays");
    }

    private static void deleteOldText(Ctx c) {
        SectorEntityToken t = Global.getSector().getEntityById("jangala");
        c.expect(TextHandler.deleteDescription(t), "old description deleted");
        c.expect(t.getCustomDescriptionId() == null, "old description gets no vanilla text back, as the changelog says (got " + t.getCustomDescriptionId() + ")");
    }

    private static void imagesOff(Ctx c) throws Exception {
        SettingChecks.set(Setting.ENABLE_IMAGES, false);
        c.reload();
    }

    private static void imagesOffResult(Ctx c) throws Exception {
        int left = 0, lost = 0;
        for (String record : seedImages(c)) {
            SectorEntityToken t = Global.getSector().getEntityById(record.substring(0, record.indexOf(':')));
            if (t == null) continue;
            if (ImageHandler.hasImage(t)) left++;
            if (t.getCustomInteractionDialogImageVisual() == null) lost++;
        }
        c.note("old save with images off: " + lost + " planets have no illustration, the changelog says a new game gets them back");
        c.expect(left == 0, "images off clears the old save (" + left + " left)");
        SettingChecks.set(Setting.ENABLE_IMAGES, true);
        c.reload();
    }

    private static void abandon(Ctx c) {
        SectorEntityToken t = Ui.entity(c, "seedColony");
        ListenerUtil.reportPlayerAbandonedColony(t.getMarket());
        c.expect(!ImageHandler.hasImage(t), "old colony loses its image when abandoned");
    }
}
