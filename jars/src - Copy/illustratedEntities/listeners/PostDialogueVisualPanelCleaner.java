package illustratedEntities.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostDialogueVisualPanelCleaner extends BaseCampaignEventListener {

    //this is a dirty hack

    public PostDialogueVisualPanelCleaner() {
        super(false);
    }

    public static void register(){
        Global.getSector().addTransientListener(new PostDialogueVisualPanelCleaner());
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        super.reportShownInteractionDialog(dialog);
        Global.getSector().addScript(new VisualPanelRemover());
    }

    public static class VisualPanelRemover implements EveryFrameScript{

        boolean isDone = false;

        @Override
        public boolean isDone() {
            return isDone;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            if (isDone) return;

            VisualCustomPanel.clearPanel();

            isDone = true;
        }
    }
}
