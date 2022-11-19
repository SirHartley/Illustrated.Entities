package illustratedEntities.dev;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.plugins.ModPlugin;

public class MarketInfoPrinter implements ColonyInteractionListener {

    public static void register(){
        Global.getSector().getListenerManager().addListener(new MarketInfoPrinter(), true);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        ModPlugin.log.info("market has image " + ImageHandler.getImageId(market.getPrimaryEntity()));

        //ModPlugin.log.info("Available images " + new ImagePicker(market.getPrimaryEntity(), false, true).numChoices());

        //for (ImageDataEntry e : ImageDataMemory.getInstance().getDataMap().values()) e.print();
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {

    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {

    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {

    }
}
