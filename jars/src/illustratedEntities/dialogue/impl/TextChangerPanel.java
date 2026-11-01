package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.MultilineTextField;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.memory.TextDataMemory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TextChangerPanel {

    protected static final float PANEL_WIDTH_1 = 580;
    protected static final float BUTTON_HEIGHT = 30;
    protected static final float SELECT_BUTTON_WIDTH = 95f;
    protected static final float TEXT_FIELD_HEIGHT = 200f;

    public static final String CLEAR = "$Illent_clearTextfields";
    private static final String NO_DESCRIPTION = "No description... yet";

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

        UIComponentAPI lastUsedAnchor;
        TooltipMakerAPI anchor;
        String buttonId;
        ButtonAPI button;
        InteractionDialogCustomPanelPlugin.ButtonEntry entry;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        final List<MultilineTextField> textFields = new ArrayList<>();

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
        baseColor = Color.BLACK;
        bgColour = Color.GRAY;
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
        TooltipMakerAPI firstAnchor = anchor;

        buttonId = "clear";
        bgColour = new Color(200, 160, 10, 255);
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Clear", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
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

        buttonId = "delete";
        bgColour = new Color(160, 30, 20, 255);
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Delete", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                new ConfirmDialoguePanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
        lastUsedAnchor = anchor;

        buttonId = "confirm";
        bgColour = new Color(50, 130, 0, 255);
        baseColor = Color.BLACK;
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Apply", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                //save text field contents to planet desc
                SectorEntityToken t = dialogue.getInteractionTarget();
                TextDataEntry data = TextHandler.getDataForEntity(t);
                TextDataMemory dataMemory = TextDataMemory.getInstance();

                if (data == null) {
                    int i = dataMemory.getNexFreetNum();
                    data = new TextDataEntry(i, t.getId());
                }

                for (int textNum = 1; textNum <= 2; textNum++) {
                    data.setText(textNum, textFields.get(textNum - 1).getText());
                }

                if (data.isValid()){
                    data.apply();
                    dataMemory.set(data.descriptionNum, data);

                    dialogue.getTextPanel().addPara("Planet Description");
                    dialogue.getTextPanel().addPara(data.getText(1));
                    dialogue.getTextPanel().addPara("");
                    dialogue.getTextPanel().addPara("Colony Description");
                    dialogue.getTextPanel().addPara(data.getText(2));
                } else dialogue.getTextPanel().addPara("Enter a description to change to.");
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
        TextDataEntry textDataEntry = TextHandler.getDataForEntity(dialogue.getInteractionTarget());

        lastUsedAnchor = firstAnchor;

        for (int textNum = 1; textNum <= 2; textNum++) {
            anchor = selectionPanel.createUIElement(PANEL_WIDTH_1, BUTTON_HEIGHT, false);

            String heading = textNum == 1 ? " [displayed on the planet]" : " [displayed when docking]";

            anchor.addSectionHeading("Description " + textNum + heading, Alignment.MID, 5f);
            selectionPanel.addUIElement(anchor).belowLeft(lastUsedAnchor, 10f); //first in row

            String text = mem.getBoolean(CLEAR) ? "" : getStartingText(dialogue, textDataEntry, textNum);
            MultilineTextField textField = new MultilineTextField(text, textFields);
            CustomPanelAPI textFieldPanel = selectionPanel.createCustomPanel(PANEL_WIDTH_1 - 4f, TEXT_FIELD_HEIGHT, textField);
            selectionPanel.addComponent(textFieldPanel).belowLeft(anchor, spad);
            lastUsedAnchor = textFieldPanel;
        }

        mem.unset(CLEAR);
        panelTooltip.addCustom(selectionPanel, 0f); //add panel
        VisualCustomPanel.addTooltipToPanel();
    }

    private String getStartingText(InteractionDialogAPI dialogue, TextDataEntry textDataEntry, int textNum) {
        if (textDataEntry != null) return textDataEntry.getText(textNum);

        String descriptionId = dialogue.getInteractionTarget().getCustomDescriptionId();
        Description description = Global.getSettings().getDescription(descriptionId, Description.Type.PLANET);
        if (description == null || (description.getText1().contains(NO_DESCRIPTION) && description.getText3().contains(NO_DESCRIPTION))) description = Global.getSettings().getDescription(descriptionId, Description.Type.CUSTOM);
        if (description == null || description.getText1().contains(NO_DESCRIPTION) || description.getText3().contains(NO_DESCRIPTION)) return "";

        return textNum == 1 ? description.getText1() : description.getText3();
    }

    private void printDefaultText(InteractionDialogAPI dialogue) {
        Description desc = Global.getSettings().getDescription(dialogue.getInteractionTarget().getCustomDescriptionId(), Description.Type.CUSTOM);
        if (desc.hasText3()) dialogue.getTextPanel().addParagraph(desc.getText3());

        FireBest.fire(null, dialogue, dialogue.getPlugin().getMemoryMap(), "MarketPostOpen");
    }
}
