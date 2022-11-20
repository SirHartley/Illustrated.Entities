package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.memory.TextDataEntry;

import java.awt.*;

public class TextChangerPanel {

    protected static final float PANEL_WIDTH_1 = 580;
    protected static final float BUTTON_HEIGHT = 30;
    protected static final float SELECT_BUTTON_WIDTH = 95f;
    protected static final float TAG_PANEL_HEGHT = 210f;

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

        panelTooltip.addSectionHeading("Change descriptions", Alignment.MID, opad);

        CustomPanelAPI selectionPanel = panel.createCustomPanel(PANEL_WIDTH_1, 40f, new NoFrameCustomPanelPlugin());

        buttonId = "return";
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Return", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                dialogue.getTextPanel().clear();
                printDefaultText(dialogue);

                new ImageDisplayPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).inTR(spad, opad); //last in row

        buttonId = "reset";
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Reset", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                //reset to what it was when the panel was first opened
                new TextChangerPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).inTL(spad, opad);
        lastUsedAnchor = anchor;

        buttonId = "confirm";
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Apply", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                //save text field contents to planet desc
                TextDataEntry e = new TextDataEntry();
                e.descriptionNum = 1;
                e.entityID = dialogue.getInteractionTarget().getId();

                e.getDescription().setText1(t1.getText());
                e.apply();
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);

        // TODO: 20/11/2022 add all text fields as transient memory entry
        // TODO: 20/11/2022 get description entry for this planet or generate new one, fill it with current text
        // TODO: 20/11/2022 fill text fields with description text (cut if needed)
        // TODO: 20/11/2022 on confirm, set new desc id and play back to mem, apply custom id num to this planet

        for (int i = 1; i <= 10; i++){ //1 to 10
            anchor = selectionPanel.createUIElement(PANEL_WIDTH_1, BUTTON_HEIGHT, false);
            t1 = anchor.addTextField(PANEL_WIDTH_1-4f,BUTTON_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
            t1.setHandleCtrlV(true);

            selectionPanel.addUIElement(anchor).belowLeft(lastUsedAnchor, i == 0 ? opad : spad); //first in row
            lastUsedAnchor = anchor;
        }

        panelTooltip.addCustom(selectionPanel, 0f); //add panel
        VisualCustomPanel.addTooltipToPanel();
    }

    public TextFieldAPI t1 = null;

    private void printDefaultText(InteractionDialogAPI dialogue){
        Description desc = Global.getSettings().getDescription(dialogue.getInteractionTarget().getCustomDescriptionId(), Description.Type.CUSTOM);
        if (desc.hasText3()) dialogue.getTextPanel().addParagraph(desc.getText3());

        FireBest.fire(null, dialogue, dialogue.getPlugin().getMemoryMap(), "MarketPostOpen");
    }
}
