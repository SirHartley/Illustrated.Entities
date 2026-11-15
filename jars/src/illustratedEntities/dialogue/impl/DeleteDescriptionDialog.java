package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.helper.TextHandler;

public class DeleteDescriptionDialog extends BaseCustomDialogDelegate {

    public static final float WIDTH = 400f;
    public static final float HEIGHT = 50f;

    private final InteractionDialogAPI dialogue;

    public DeleteDescriptionDialog(InteractionDialogAPI dialogue) {
        this.dialogue = dialogue;
    }

    public void show() {
        dialogue.showCustomDialog(WIDTH, HEIGHT, this);
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        TooltipMakerAPI text = panel.createUIElement(WIDTH, HEIGHT, false);
        text.addPara("Are you sure? You are about to delete the description of this planet. This cannot be undone.", 0f);
        panel.addUIElement(text).inTL(0f, 0f);
    }

    @Override
    public boolean hasCancelButton() {
        return true;
    }

    @Override
    public String getConfirmText() {
        return "Delete";
    }

    @Override
    public String getCancelText() {
        return "Cancel";
    }

    @Override
    public void customDialogConfirm() {
        if (TextHandler.deleteDescription(dialogue.getInteractionTarget())) {
            Global.getSector().getMemoryWithoutUpdate().set(TextChangerPanel.CLEAR, true, 0f);
            dialogue.getTextPanel().addPara("Description deleted.", Misc.getHighlightColor());
        } else dialogue.getTextPanel().addPara("There is no description to delete!", Misc.getHighlightColor());

        new TextChangerPanel().showPanel(dialogue);
    }
}
