package illustratedEntities.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;
import illustratedEntities.helper.ImageHandler;

public class DecivImageRemover implements ColonyDecivListener {

    public static void register(){
        Global.getSector().getListenerManager().addListener(new DecivImageRemover(), true);
    }

    @Override
    public void reportColonyAboutToBeDecivilized(MarketAPI market, boolean fullyDestroyed) {

    }

    @Override
    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        SectorEntityToken primaryEntity = market.getPrimaryEntity();

        if (ImageHandler.hasImage(primaryEntity)) ImageHandler.removeImageFrom(primaryEntity);
    }
}
