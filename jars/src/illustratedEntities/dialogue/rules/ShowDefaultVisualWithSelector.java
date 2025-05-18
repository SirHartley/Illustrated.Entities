package illustratedEntities.dialogue.rules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.ShowDefaultVisual;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.impl.ImageDisplayPanel;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.Settings;

import java.util.List;
import java.util.Map;

public class ShowDefaultVisualWithSelector extends ShowDefaultVisual {

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        SectorEntityToken target = dialog.getInteractionTarget();
        boolean hasImage = target.getCustomInteractionDialogImageVisual() != null;
        boolean hasMarket = target.getMarket() != null;
        boolean isPlayerOwned = target.getMarket().isPlayerOwned();
        boolean allowsNPCPicking = !isPlayerOwned && (Settings.getBoolean(Settings.BUTTON_ALWAYS_SHOW) || Global.getSettings().isDevMode());

        if(hasImage && hasMarket
                && (allowsNPCPicking || isPlayerOwned)){

            VisualCustomPanel.createPanel(dialog, true);
            new ImageDisplayPanel().showPanel(dialog);

        } else if (target.getCustomInteractionDialogImageVisual() != null) {
            dialog.getVisualPanel().showImageVisual(target.getCustomInteractionDialogImageVisual());

        } else {
            if (target.getMarket() != null) {
                target = target.getMarket().getPlanetEntity();
            }
            if (target instanceof PlanetAPI) {
                if (!Global.getSettings().getBoolean("3dPlanetBGInInteractionDialog")) {
                    dialog.getVisualPanel().showPlanetInfo((PlanetAPI) target);
                }

            } else if (target instanceof CampaignFleetAPI) {
                CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                CampaignFleetAPI otherFleet = (CampaignFleetAPI) target;
                showFleetInfo(dialog, playerFleet, otherFleet);
            }
        }

        return true;
    }
}
