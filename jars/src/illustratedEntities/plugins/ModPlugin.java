package illustratedEntities.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.loading.specs.PlanetSpec;
import illustratedEntities.dev.MarketInfoPrinter;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.Settings;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.listeners.*;
import illustratedEntities.memory.ImageDataMemory;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ModPlugin extends BaseModPlugin {
    public static Logger log = Global.getLogger(ModPlugin.class);
    public static final String MOD_ID = "illustrated_entities";
    public static final String HAS_INTERACTION_IMAGE = "$illustrated_entity_image";
    public static final String ORBITAL_HAS_INTERACTION_IMAGE = "$illustrated_entity_image_orbital";

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        DecivImageRemover.register();
        NewColonyImageAdder.register();
        ImageOnJumpLoader.register();
        PlayerOpenColonyListener.register();
        PostDialogueVisualPanelCleaner.register();
        OrbitalStationSynchListener.register();
        UnusedImageUnloader.register();
        ImageDataMemory.getInstance().forceRefresh();

        ImageHandler.cleanseLeftoverImagesFromRemovedMarkets();
        ImageHandler.reapplyAllImages();
        TextHandler.applyAllEntries();

        if(Settings.getBoolean(Settings.ENABLE_IMAGES)){
            ImageHandler.addImagesToPlayerMarkets();

            if(Settings.getBoolean(Settings.ENABLE_PRESET_CORE_WORLDS)) ImageHandler.addPresetImagesToMarkets();
            if(Settings.getBoolean(Settings.ENABLE_RANDOM_IMAGES)) ImageHandler.addRandomImagesToMarketsWithoutImage(Settings.getBoolean(Settings.SKIP_CORE));
        } else {
            for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) if (m.getPrimaryEntity() != null) ImageHandler.removeImageFrom(m.getPrimaryEntity());
        }

        ImageHandler.loadImagesForCurrentLocation();

        devActions();
    }

    private void devActions(){
        if (Global.getSettings().isDevMode()) {
            MarketInfoPrinter.register();

            for (LocationAPI loc : Global.getSector().getAllLocations()){
                for (SectorEntityToken t : loc.getAllEntities()){
                    if (t.getMarket() != null && !t.getMarket().isPlanetConditionMarketOnly()) ModPlugin.log.info(t.getMarket().getId() + ", market:  ," + t.getMarket().getName() + ", entity id ," + t.getId() + ", entity name "
                            + t.getName() +", faction ,"+ t.getMarket().getFactionId());
                }
            }

            /*String[] glowTextures = {
                    //"graphics/illustrated_entities/lights/night_lights_dense.png",
                    //"graphics/illustrated_entities/lights/night_lights_moderate.png",
                    //"graphics/illustrated_entities/lights/night_lights_sparse.png",
                    //"graphics/illustrated_entities/lights/night_lights_ultra_dense.png",
                    //"graphics/illustrated_entities/lights/night_lights_very_sparse.png",
                    "graphics/illustrated_entities/lights/night_lights_test.png"
            };

            List<PlanetAPI> planets = Global.getSector().getPlayerFleet().getContainingLocation().getPlanets();

            for (String s : glowTextures) {
                try {
                    Global.getSettings().loadTexture(s);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            for (int i = 0; i < planets.size(); i++) {
                PlanetAPI planet = planets.get(i);

                int textureIndex = i % glowTextures.length;
                planet.getSpec().setGlowTexture(glowTextures[textureIndex]);
                planet.getSpec().setUseReverseLightForGlow(!planet.getStarSystem().isNebula());
                planet.getSpec().setAtmosphereThickness(0.5f);
                planet.getSpec().setGlowColor(new Color(255,255,255,255));
                planet.setName(planet.getName() + " - glow " + textureIndex);

                planet.applySpecChanges();

            }
*/
            //ImageDataMemory.getInstance().loadInitialData();
        }
    }
}
