package illustratedEntities.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import illustratedEntities.helper.Settings;
import illustratedEntities.loading.Importer;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static illustratedEntities.plugins.ModPlugin.HAS_INTERACTION_IMAGE;

public class ImageDataEntry {
    public int id;
    public List<String> requiredTags;
    public List<String> requiredExcludedTags;
    public List<String> optionalTags;
    public List<String> optionalExcludedTags;
    public List<String> usedEntityIds;
    public String planetId;

    public String faction;
    public int weight;
    public transient Boolean isLoaded;

    public ImageDataEntry(int id, int weight, List<String> requiredTags, List<String> requiredExcludedTags, List<String> optionalTags, List<String> optionalExcludedTags, String faction, String planetId) {
        this.id = id;
        this.weight = weight;
        this.requiredTags = requiredTags;
        this.requiredExcludedTags = requiredExcludedTags;
        this.optionalTags = optionalTags;
        this.optionalExcludedTags = optionalExcludedTags;

        this.usedEntityIds = new ArrayList<>();
        this.faction = faction;
        this.planetId = planetId;
    }

    public String getImagePath(){
        return Settings.DEFAULT_IMAGE_PATH + "/" + id + ".png";
    }

    public boolean isUsed(){
        return !usedEntityIds.isEmpty();
    }

    public void addToEntity(SectorEntityToken t){
        String entityID = t.getId();

        String path = getImagePath();
        InteractionDialogImageVisual visual = new InteractionDialogImageVisual(path, 480, 300);

        t.setCustomInteractionDialogImageVisual(visual);
        t.getMemoryWithoutUpdate().set(HAS_INTERACTION_IMAGE, id);
        t.addTag(HAS_INTERACTION_IMAGE);

        if (!usedEntityIds.contains(entityID)) usedEntityIds.add(entityID);

        ImageDataMemory.getInstance().set(this.id, this);
    }

    public void removeFromEntity(SectorEntityToken t){
        usedEntityIds.remove(t.getId());
        t.setCustomInteractionDialogImageVisual(null);
        t.getMemoryWithoutUpdate().unset(HAS_INTERACTION_IMAGE);
        t.removeTag(HAS_INTERACTION_IMAGE);

        ImageDataMemory.getInstance().set(this.id, this);
    }

    public void applyAllPlanets(){
        SectorAPI sector = Global.getSector();
        for (String s : usedEntityIds){
            SectorEntityToken t = sector.getEntityById(s);

            if (t != null && t.getMarket() != null && !t.getMarket().isPlanetConditionMarketOnly()) addToEntity(t);
        }
    }

    public void load(){
        if(isLoaded == null) Importer.loadImage(getImagePath());
        isLoaded = true;
    }

    public void print(){
        String[] r = requiredTags.toArray(new String[0]);
        String[] re = requiredExcludedTags.toArray(new String[0]);
        String[] o = optionalTags.toArray(new String[0]);
        String[] oe = optionalExcludedTags.toArray(new String[0]);

        ModPlugin.log.info("data " + id);
        ModPlugin.log.info("required " + Arrays.toString(r));
        ModPlugin.log.info("required excluded " + Arrays.toString(re));
        ModPlugin.log.info("optional " + Arrays.toString(o));
        ModPlugin.log.info("optional excluded " + Arrays.toString(oe));
    }
}
