package illustratedEntities.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.memory.ImageDataMemory;

public class PlayerOpenColonyListener extends BaseCampaignEventListener {
    public static void register(){
        Global.getSector().addTransientListener(new PlayerOpenColonyListener(false));
    }

    public PlayerOpenColonyListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        super.reportPlayerOpenedMarket(market);
        SectorEntityToken primary = market.getPrimaryEntity();

        if(ImageHandler.hasImage(primary)) ImageDataMemory.getInstance().get(ImageHandler.getImageId(primary)).load();
    }
}
