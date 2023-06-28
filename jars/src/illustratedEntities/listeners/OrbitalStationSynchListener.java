package illustratedEntities.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import illustratedEntities.helper.ImageHandler;

public class OrbitalStationSynchListener extends BaseCampaignEventListener {

    public static void register(){
        Global.getSector().getListenerManager().addListener(new OrbitalStationSynchListener(), true);
    }

    public OrbitalStationSynchListener() {
        super(false);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        super.reportPlayerClosedMarket(market);

        ImageHandler.applyPlanetImageToOrbitalStations(market);
    }
}
