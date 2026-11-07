package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.panel.ImageVisualPlugin;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.Setting;

import java.awt.*;

public class ImageDisplayPanel {

    protected static final float BUTTON_HEIGHT = 30;
    protected static final float SELECT_BUTTON_WIDTH = 80f;
    // a tooltip draws its first element 5 right of its origin
    protected static final float TOOLTIP_INSET = 5f;

    public void showPanel(InteractionDialogAPI dialogue) {
        VisualCustomPanel.createPanel(dialogue, true, false);
        showInitialPanel(dialogue);
    }

    private void showInitialPanel(final InteractionDialogAPI dialogue) {

        float opad = 10f;
        float spad = 3f;

        final CustomPanelAPI panel = VisualCustomPanel.getPanel();

        Color baseColor = Color.WHITE;
        Color bgColour = Color.darkGray;
        Color brightColor = Misc.getBrightPlayerColor();

        TooltipMakerAPI lastUsedAnchor = null;
        TooltipMakerAPI anchor;
        String buttonId;
        ButtonAPI button;
        InteractionDialogCustomPanelPlugin.ButtonEntry entry;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        //IMAGE PANEL

        InteractionDialogImageVisual visual = dialogue.getInteractionTarget().getCustomInteractionDialogImageVisual();
        float imageWidth = visual.getSubImageDisplayWidth();

        // vanilla places its image visual with the same rightOfTop as this panel, so both share the top left
        CustomPanelAPI imagePanel = panel.createCustomPanel(imageWidth, visual.getSubImageDisplayHeight(), new ImageVisualPlugin(visual));
        panel.addComponent(imagePanel).inTL(0f, 0f);

        CustomPanelAPI selectionPanel = panel.createCustomPanel(imageWidth, BUTTON_HEIGHT, new NoFrameCustomPanelPlugin());

        //change image button
        if (Setting.ENABLE_IMAGES.enabled()) {
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
            selectionPanel.addUIElement(anchor).inTR(TOOLTIP_INSET, 0f); //last in row
            lastUsedAnchor = anchor;
        }

        //desc
        if (Setting.ENABLE_TEXT.enabled()){
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
            if (lastUsedAnchor == null) selectionPanel.addUIElement(anchor).inTR(TOOLTIP_INSET, 0f);
            else selectionPanel.addUIElement(anchor).leftOfMid(lastUsedAnchor, opad); //first in row
            lastUsedAnchor = anchor;
        }

        panel.addComponent(selectionPanel).belowLeft(imagePanel, 0f);
    }
}
