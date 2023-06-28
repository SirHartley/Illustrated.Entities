package illustratedEntities.helper;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;

import static illustratedEntities.plugins.ModPlugin.HAS_INTERACTION_IMAGE;

public class ImageHandler {

    public static boolean hasImage(SectorEntityToken t){
        return t.hasTag(HAS_INTERACTION_IMAGE) || (Entities.STATION_BUILT_FROM_INDUSTRY.equals(t.getCustomEntityType()) && t.hasTag(ModPlugin.ORBITAL_HAS_INTERACTION_IMAGE));
    }

    public static int getImageId(SectorEntityToken t){
        return t.getMemoryWithoutUpdate().getInt(HAS_INTERACTION_IMAGE);
    }

    public static void setImage(SectorEntityToken t, ImageDataEntry entry, boolean orbitalStation){
        if(orbitalStation) entry.addToOrbitalStation(t);
        else entry.addToEntity(t);
    }

    public static void removeImageFrom(SectorEntityToken t){
        ModPlugin.log.info("removing image from " + t.getName());

        if (!hasImage(t)) return;

        int num = getImageId(t);
        ImageDataMemory.getInstance().get(num).removeFromEntity(t);
    }

    public static int applyFittingRandomImageToEntity(SectorEntityToken t){
        if(t.hasTag(HAS_INTERACTION_IMAGE) || (!Settings.OVERWRITE_VANILLA && t.getCustomInteractionDialogImageVisual() != null)) return 0;

        ImagePicker picker = new ImagePicker(t, Settings.AVOID_DUPLICATES, true);
        if (picker.isEmpty()) {
            ModPlugin.log.warn("Unable to pick interaction image for " + t.getName() + ", id " + t.getId());
            return 0;
        }

        int choice = picker.pick();

        ImageDataMemory.getInstance().get(choice).addToEntity(t);

        ModPlugin.log.info("Applying image " + choice + " to " + t.getName() + " (" + (t.getMarket() != null ? t.getMarket().getName() : "No Market") + ")");
        return choice;
    }

    public static void loadImagesForCurrentLocation(){
        LocationAPI loc = Global.getSector().getCurrentLocation();

        if (loc != null) {
            for (SectorEntityToken t : loc.getEntitiesWithTag(ModPlugin.HAS_INTERACTION_IMAGE)){
                int id = ImageHandler.getImageId(t);
                ImageDataMemory.getInstance().get(id).load();
            }

            for (MarketAPI m : Misc.getMarketsInLocation(loc)) ImageHandler.applyPlanetImageToOrbitalStations(m);
        }
    }

    public static void applyPlanetImageToOrbitalStations(MarketAPI m){
        SectorEntityToken primary = m.getPrimaryEntity();

        if (Misc.hasOrbitalStation(m) && hasImage(primary)){
            for (SectorEntityToken t : m.getConnectedEntities()){
                if (t == primary) continue;

                if (t.getCustomEntityType().equals(Entities.STATION_BUILT_FROM_INDUSTRY) && (t.getCustomInteractionDialogImageVisual() == null || hasImage(t))) {
                    int id = getImageId(primary);
                    setImage(t, ImageDataMemory.getInstance().get(id), true);
                }
            }
        }
    }

    public static void addRandomImagesToMarketsWithoutImage(){
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()){
            SectorEntityToken entity = m.getPrimaryEntity();

            if (!entity.hasTag(HAS_INTERACTION_IMAGE) && (Settings.OVERWRITE_VANILLA || entity.getCustomInteractionDialogImageVisual() == null)) {
                int i = applyFittingRandomImageToEntity(entity);
                ModPlugin.log.info("applying image to " + entity.getName() + ": " + i);
            }
        }
    }

    public static void addPresetImagesToMarkets(){
        ImageDataMemory memory = ImageDataMemory.getInstance();

        for (ImageDataEntry data : new ArrayList<>(memory.getDataMap().values())){
            //ModPlugin.log.info("Entry " + data.id + " target -" + data.planetId + "- entity exists: " + (Global.getSector().getEntityById(data.planetId) != null));

            if (!data.planetId.isEmpty()) {
                SectorEntityToken t = Global.getSector().getEntityById(data.planetId);

                if (t != null
                        && t.getMarket() != null
                        && !t.getMarket().isPlanetConditionMarketOnly()) {

                    //the default pirate station visual will always get overridden
                    if (Settings.PRESET_OVERWRITE || t.getCustomInteractionDialogImageVisual() == null || t.getCustomInteractionDialogImageVisual().getSpriteName().contains("pirate_station")) ImageHandler.setImage(t, data, false);
                }
            }
        }
    }

    public static void addImagesToPlayerMarkets(){
        for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()){
            if (!m.isPlayerOwned() || hasImage(m.getPrimaryEntity())) continue;

            SectorEntityToken t = m.getPrimaryEntity();

            if (t != null){
                applyFittingRandomImageToEntity(t);
            }
        }
    }

    public static void reapplyAllImages(){
        for (ImageDataEntry entry : ImageDataMemory.getInstance().getDataMap().values()){
            entry.applyAllPlanets();
        }
    }

    public static void cleanseLeftoverImagesFromRemovedMarkets(){
        for (SectorEntityToken t : Global.getSector().getEntitiesWithTag(HAS_INTERACTION_IMAGE)){
            if (t.getMarket() == null || t.getMarket().isPlanetConditionMarketOnly() || !t.isAlive()) ImageHandler.removeImageFrom(t);
        }
    }
}
