package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.sun.javafx.iio.common.ImageTools;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;

import java.awt.*;

public class ImageDisplayPanel {

    protected static final float PANEL_WIDTH_1 = 480;
    protected static final float BUTTON_HEIGHT = 30;
    protected static final float SELECT_BUTTON_WIDTH = 95f;

    public void showPanel(InteractionDialogAPI dialogue) {
        VisualCustomPanel.createPanel(dialogue, true);
        showInitialPanel(dialogue);
    }

    private void showInitialPanel(final InteractionDialogAPI dialogue) {
        float opad = 10f;
        float spad = 3f;

        final CustomPanelAPI panel = VisualCustomPanel.getPanel();
        TooltipMakerAPI panelTooltip = VisualCustomPanel.getTooltip();

        //dialogue.getVisualPanel().showImageVisual(dialogue.getInteractionTarget().getCustomInteractionDialogImageVisual());

        Color baseColor = Color.WHITE;
        Color bgColour = Color.darkGray;

        CustomPanelAPI imagePanel = panel.createCustomPanel(PANEL_WIDTH_1, 300f, new NoFrameCustomPanelPlugin());

        //NAME PANEL
        TooltipMakerAPI imageTooltip = imagePanel.createUIElement(480, 310, false);
        imageTooltip.addImage(dialogue.getInteractionTarget().getCustomInteractionDialogImageVisual().getSpriteName(), 0f);
        imagePanel.addUIElement(imageTooltip).inTL(0f, 0f);
        panelTooltip.addCustom(imagePanel, 0f);

        String buttonId = "change_interaction_visual";
        boolean devmode = Global.getSettings().isDevMode();
        String buttonName = devmode ? "[" + ImageHandler.getImageId(dialogue.getInteractionTarget()) + "] change" : "change";
        float width = devmode ? SELECT_BUTTON_WIDTH * 1.5f : SELECT_BUTTON_WIDTH;

        CustomPanelAPI buttonPanel = panel.createCustomPanel(PANEL_WIDTH_1, BUTTON_HEIGHT, new NoFrameCustomPanelPlugin());
        TooltipMakerAPI anchor = buttonPanel.createUIElement(PANEL_WIDTH_1, BUTTON_HEIGHT, false);
        ButtonAPI button = anchor.addButton(buttonName, buttonId, baseColor, bgColour, Alignment.MID, CutStyle.BOTTOM, width, BUTTON_HEIGHT * 0.8f, 0);
        InteractionDialogCustomPanelPlugin.ButtonEntry entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                new ImageSelectorPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        PositionAPI pos = buttonPanel.addUIElement(anchor).inBR(0f, 0f);
        pos.setXAlignOffset(PANEL_WIDTH_1 - width);

        panelTooltip.addCustom(buttonPanel, opad); //add panel

        VisualCustomPanel.addTooltipToPanel();
    }
}
