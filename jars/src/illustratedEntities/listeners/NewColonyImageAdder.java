package illustratedEntities.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.memory.ImageDataMemory;
import illustratedEntities.plugins.ModPlugin;

public class NewColonyImageAdder implements PlayerColonizationListener {

    public static void register(){
        Global.getSector().getListenerManager().addListener(new NewColonyImageAdder(), true);
    }

    @Override
    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        int i = ImageHandler.applyFittingRandomImageToEntity(planet);

        ModPlugin.log.info("applying image to newly established colony: " + i);
        if(i > 0) ImageDataMemory.getInstance().get(i).load();
    }

    @Override
    public void reportPlayerAbandonedColony(MarketAPI colony) {
        ImageHandler.removeImageFrom(colony.getPrimaryEntity());
    }
}
