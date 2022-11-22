package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.helper.TextHandler;

import java.awt.*;

public class ConfirmDialoguePanel {

    protected static final float PANEL_WIDTH_1 = 580;
    protected static final float BUTTON_HEIGHT = 30;
    protected static final float SELECT_BUTTON_WIDTH = 95f;
    protected static final float TEXT_FIELD_PANEL_HEIGHT = 300f;

    public static final String TEXTFIELD_KEY = "$Illent_textField_";
    public static final String CLEAR = "$Illent_clearTextfields";

    public void showPanel(InteractionDialogAPI dialogue) {
        VisualCustomPanel.createPanel(dialogue, true);
        showTextChangePanel(dialogue);
    }

    private void showTextChangePanel(final InteractionDialogAPI dialogue) {
        float opad = 10f;
        float spad = 3f;

        final CustomPanelAPI panel = VisualCustomPanel.getPanel();
        final TooltipMakerAPI panelTooltip = VisualCustomPanel.getTooltip();

        Color baseColor = Misc.getButtonTextColor();
        Color bgColour = Misc.getDarkPlayerColor();
        Color brightColor = Misc.getBrightPlayerColor();

        TooltipMakerAPI lastUsedAnchor;
        TooltipMakerAPI anchor;
        String buttonId;
        ButtonAPI button;
        InteractionDialogCustomPanelPlugin.ButtonEntry entry;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        panelTooltip.addSectionHeading("Confirm Deletion", Alignment.MID, opad);

        CustomPanelAPI selectionPanel = panel.createCustomPanel(PANEL_WIDTH_1, TEXT_FIELD_PANEL_HEIGHT, new NoFrameCustomPanelPlugin());

        TooltipMakerAPI desc = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH * 3, BUTTON_HEIGHT, false);
        desc.addPara("Are you sure? You are about to delete the description of this planet. This cannot be undone.", opad);
        selectionPanel.addUIElement(desc).inTL(spad, opad);
        lastUsedAnchor = desc;

        buttonId = "delete";
        baseColor = Color.BLACK;
        bgColour = new Color(160, 30, 20, 255);
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Delete", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                boolean success = TextHandler.deleteDescription(dialogue.getInteractionTarget());

                if(success){
                    Global.getSector().getMemoryWithoutUpdate().set(CLEAR, true, 0f);
                    dialogue.getTextPanel().addPara("Description deleted.", Misc.getHighlightColor());
                } else  dialogue.getTextPanel().addPara("There is no description to delete!", Misc.getHighlightColor());

                new TextChangerPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).belowLeft(lastUsedAnchor, opad);
        lastUsedAnchor = anchor;

        buttonId = "return";
        baseColor = Misc.getButtonTextColor();
        bgColour = Misc.getDarkPlayerColor();
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Return", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                //reset to what it was when the panel was first opened
                Global.getSector().getMemoryWithoutUpdate().set(CLEAR, true, 0f);
                new TextChangerPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
        lastUsedAnchor = anchor;

        panelTooltip.addCustom(selectionPanel, 0f); //add panel
        VisualCustomPanel.addTooltipToPanel();
    }
}
