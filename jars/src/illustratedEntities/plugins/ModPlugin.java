package illustratedEntities.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import illustratedEntities.dev.MarketInfoPrinter;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.Settings;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.listeners.DecivImageRemover;
import illustratedEntities.listeners.ImageOnJumpLoader;
import illustratedEntities.listeners.NewColonyImageAdder;
import illustratedEntities.listeners.PostDialogueVisualPanelCleaner;
import org.apache.log4j.Logger;

public class ModPlugin extends BaseModPlugin {
    public static Logger log = Global.getLogger(ModPlugin.class);
    public static final String MOD_ID = "illustrated_entities";
    public static final String HAS_INTERACTION_IMAGE = "$illustrated_entity_image";

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        DecivImageRemover.register();
        NewColonyImageAdder.register();
        ImageOnJumpLoader.register();
        PostDialogueVisualPanelCleaner.register();

        ImageHandler.cleanseLeftoverImagesFromRemovedMarkets();
        ImageHandler.reapplyAllImages();
        TextHandler.applyAllEntries();

        ImageHandler.addImagesToPlayerMarkets();
        if(Settings.APPLY_PRESET) ImageHandler.addPresetImagesToMarkets();
        if(Settings.APPLY_RANDOM) ImageHandler.addRandomImagesToMarketsWithoutImage();
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
            //ImageDataMemory.getInstance().loadInitialData();
        }
    }
}
