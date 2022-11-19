package illustratedEntities.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.memory.ImageDataMemory;
import illustratedEntities.plugins.ModPlugin;

public class ImageOnJumpLoader extends BaseCampaignEventListener {

    public ImageOnJumpLoader() {
        super(false);
    }

    public static void register(){
        Global.getSector().addTransientListener(new ImageOnJumpLoader());
    }

    @Override
    public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpPointAPI.JumpDestination to) {
        super.reportFleetJumped(fleet, from, to);

        if (fleet.isPlayerFleet()){
            if (to.getDestination() != null && to.getDestination().getContainingLocation() != null) {
                LocationAPI dest = to.getDestination().getContainingLocation();

                for (MarketAPI m : Misc.getMarketsInLocation(dest)) ImageHandler.applyPlanetImageToOrbitalStations(m);
                loadImages(dest);
            }
        }
    }

    private void loadImages(LocationAPI system){
        for (SectorEntityToken t : system.getEntitiesWithTag(ModPlugin.HAS_INTERACTION_IMAGE)){
            int id = ImageHandler.getImageId(t);
            ImageDataMemory.getInstance().get(id).load();
        }
    }

}
