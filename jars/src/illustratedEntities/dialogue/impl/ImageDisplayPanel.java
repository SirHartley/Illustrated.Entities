package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.Settings;

import java.awt.*;

public class ImageDisplayPanel {

    protected static final float PANEL_WIDTH_1 = 480;
    protected static final float BUTTON_HEIGHT = 30;
    protected static final float SELECT_BUTTON_WIDTH = 80f;

    public void showPanel(InteractionDialogAPI dialogue) {
        VisualCustomPanel.createPanel(dialogue, true);
        showInitialPanel(dialogue);
    }

    private void showInitialPanel(final InteractionDialogAPI dialogue) {

        float opad = 10f;
        float spad = 3f;

        final CustomPanelAPI panel = VisualCustomPanel.getPanel();
        TooltipMakerAPI panelTooltip = VisualCustomPanel.getTooltip();

        Color baseColor = Color.WHITE;
        Color bgColour = Color.darkGray;
        Color brightColor = Misc.getBrightPlayerColor();

        TooltipMakerAPI lastUsedAnchor;
        TooltipMakerAPI anchor;
        String buttonId;
        ButtonAPI button;
        InteractionDialogCustomPanelPlugin.ButtonEntry entry;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        //IMAGE PANEL

        CustomPanelAPI imagePanel = panel.createCustomPanel(PANEL_WIDTH_1, 300f, new NoFrameCustomPanelPlugin());
        TooltipMakerAPI imageAnchor = imagePanel.createUIElement(480, 310, false);
        imageAnchor.addImage(dialogue.getInteractionTarget().getCustomInteractionDialogImageVisual().getSpriteName(), 0f);
        imagePanel.addUIElement(imageAnchor).inTL(0f, 0f);
        panelTooltip.addCustom(imagePanel, 0f);

        CustomPanelAPI selectionPanel = panel.createCustomPanel(PANEL_WIDTH_1, BUTTON_HEIGHT, new NoFrameCustomPanelPlugin());

        //change image button
        buttonId = "change_interaction_visual";
        boolean devmode = Global.getSettings().isDevMode();
        String buttonName = devmode ? "[" + ImageHandler.getImageId(dialogue.getInteractionTarget()) + "] image" : "image";
        float width = devmode ? SELECT_BUTTON_WIDTH * 1.5f : SELECT_BUTTON_WIDTH;

        anchor = selectionPanel.createUIElement(width, BUTTON_HEIGHT, false);
        button = anchor.addButton(buttonName, buttonId, baseColor, bgColour, Alignment.MID, CutStyle.BOTTOM, width, BUTTON_HEIGHT * 0.8f, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                new ImageSelectorPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).inTR(0f, 0f); //last in row
        lastUsedAnchor = anchor;

        //desc
        if (Settings.ENABLE_TEXT_CHANGER){
            buttonId = "desc";
            anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
            button = anchor.addButton("desc.", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.BOTTOM, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT * 0.8f, 0);
            entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
                @Override
                public void onToggle() {
                    //reset to what it was when the panel was first opened
                    new TextChangerPanel().showPanel(dialogue);
                }
            };

            VisualCustomPanel.getPlugin().addButton(entry);
            selectionPanel.addUIElement(anchor).leftOfMid(lastUsedAnchor, opad); //first in row
            lastUsedAnchor = anchor;
        }

        panelTooltip.addCustom(selectionPanel, 0f); //add panel

        VisualCustomPanel.addTooltipToPanel();
    }
}
