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
import illustratedEntities.dialogue.panel.FramedCustomPanelPlugin;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.helper.WordUtils;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.memory.TextDataMemory;
import org.lwjgl.opencl.CL;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextChangerPanel {

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
        selectionPanel.addUIElement(anchor).inTL(spad, opad);
        lastUsedAnchor = anchor;

        buttonId = "delete";
        bgColour = new Color(160, 30, 20, 255);
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton("Delete", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                //reset to what it was when the panel was first opened
                Global.getSector().getCampaignUI().showConfirmDialog(
                        "Are you sure? This will clear the description and cannot be undone.",
                        "Confirm",
                        "Return",
                        new Script() {
                            @Override
                            public void run() {
                                boolean success = TextHandler.deleteDescription(dialogue.getInteractionTarget());

                                if(success){
                                    Global.getSector().getMemoryWithoutUpdate().set(CLEAR, true, 0f);
                                    dialogue.getTextPanel().addPara("Description deleted.", Misc.getHighlightColor());
                                } else  dialogue.getTextPanel().addPara("There is no description to delete!", Misc.getHighlightColor());

                                new TextChangerPanel().showPanel(dialogue);
                            }
                        },
                        null);
                Global.getSector().getMemoryWithoutUpdate().set(CLEAR, true, 0f);
                new TextChangerPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).inTL(spad, opad);
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
                MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

                if (data == null) {
                    int i = dataMemory.getNexFreetNum();
                    data = new TextDataEntry(i, t.getId());
                }

                for (int textNum = 1; textNum <= 2; textNum++) {
                    for (int lineNum = 1; lineNum <= TextDataEntry.LINE_AMT; lineNum++) {
                        TextFieldAPI s = (TextFieldAPI) mem.get(TEXTFIELD_KEY + textNum + lineNum);
                        data.setString(textNum, lineNum, s.getText());
                    }
                }

                if (data.isValid()){
                    data.apply();
                    dataMemory.set(data.descriptionNum, data);

                    dialogue.getTextPanel().addPara("Planet Description");
                    dialogue.getTextPanel().addPara(data.parseStringMap(data.stringMap1));
                    dialogue.getTextPanel().addPara("");
                    dialogue.getTextPanel().addPara("Colony Description");
                    dialogue.getTextPanel().addPara(data.parseStringMap(data.stringMap2));
                } else dialogue.getTextPanel().addPara("Enter a description to change to.");
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
        TooltipMakerAPI lastMainPanelAnchor = anchor;

        TextDataEntry textDataEntry = TextHandler.getDataForEntity(dialogue.getInteractionTarget());

        for (int textNum = 1; textNum <= 2; textNum++) {

            //create new panel
            final CustomPanelAPI textFieldPanel = selectionPanel.createCustomPanel(PANEL_WIDTH_1, TEXT_FIELD_PANEL_HEIGHT, new FramedCustomPanelPlugin(0.1f, bgColour, false));
            TooltipMakerAPI textFieldPanelAnchor = selectionPanel.createUIElement(PANEL_WIDTH_1, TEXT_FIELD_PANEL_HEIGHT, true);

            anchor = textFieldPanel.createUIElement(PANEL_WIDTH_1, BUTTON_HEIGHT, false);

            String heading = textNum == 1 ? " [displayed on the planet]" : " [displayed when docking]";

            anchor.addSectionHeading("Description " + textNum + heading, Alignment.MID, 5f);
            textFieldPanel.addUIElement(anchor).inTL(0f, 0f); //first in row
            lastUsedAnchor = anchor;

            Description description = null;
            String illegalString = "No description... yet";

            if (textDataEntry == null) description = Global.getSettings().getDescription(dialogue.getInteractionTarget().getCustomDescriptionId(), Description.Type.PLANET);
            if (description == null || (description.getText1().contains(illegalString) && description.getText3().contains(illegalString))) description = Global.getSettings().getDescription(dialogue.getInteractionTarget().getCustomDescriptionId(), Description.Type.CUSTOM);

            for (int lineNum = 1; lineNum <= TextDataEntry.LINE_AMT; lineNum++) { //1 to 10
                anchor = textFieldPanel.createUIElement(PANEL_WIDTH_1, BUTTON_HEIGHT, false);
                TextFieldAPI t1 = anchor.addTextField(PANEL_WIDTH_1 - 4f, BUTTON_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
                t1.setHandleCtrlV(true);

                if (!mem.getBoolean(CLEAR)){
                    if (textDataEntry != null) {
                        String s = textDataEntry.getString(textNum, lineNum);
                        if (s != null && !s.startsWith("\n")) t1.setText(s);
                    } else if(!description.getText1().contains(illegalString) && !description.getText3().contains(illegalString)){
                        int charNum = 80;
                        String s = textNum == 1 ? description.getText1() : description.getText3();

                        s = WordUtils.wrap(s, charNum);
                        String[] stringArray = s.split("\\r?\\n");
                        List<String> cleanedStringList = new ArrayList<>();

                        for (String line : stringArray){
                            StringBuilder builder = new StringBuilder(line);
                            if (Character.isWhitespace(builder.charAt(0))) builder.deleteCharAt(0);
                            if (Character.isWhitespace(builder.charAt(builder.toString().length()-1))) builder.deleteCharAt(builder.toString().length()-1);

                            cleanedStringList.add(builder.toString());
                        }

                        if (lineNum <= cleanedStringList.size()) t1.setText(cleanedStringList.get(lineNum - 1));
                    }
                } else mem.unset(CLEAR);

                mem.set(TEXTFIELD_KEY + textNum + lineNum, t1, 0f);

                textFieldPanel.addUIElement(anchor).belowLeft(lastUsedAnchor, spad); //first in row
                lastUsedAnchor = anchor;
            }

            textFieldPanelAnchor.addCustom(textFieldPanel, 0f);
            selectionPanel.addUIElement(textFieldPanelAnchor).belowMid(lastMainPanelAnchor, 0f);
            lastMainPanelAnchor = textFieldPanelAnchor;
        }


        panelTooltip.addCustom(selectionPanel, 0f); //add panel
        VisualCustomPanel.addTooltipToPanel();
    }

    private void printDefaultText(InteractionDialogAPI dialogue) {
        Description desc = Global.getSettings().getDescription(dialogue.getInteractionTarget().getCustomDescriptionId(), Description.Type.CUSTOM);
        if (desc.hasText3()) dialogue.getTextPanel().addParagraph(desc.getText3());

        FireBest.fire(null, dialogue, dialogue.getPlugin().getMemoryMap(), "MarketPostOpen");
    }
}
