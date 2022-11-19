package illustratedEntities.dialogue.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.dialogue.panel.FramedCustomPanelPlugin;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.NoFrameCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.ImagePicker;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ImageSelectorPanel {

    protected static final float PANEL_WIDTH_1 = 580;
    protected static final float BUTTON_HEIGHT = 30;
    protected static final float ARROW_BUTTON_SIZE = 20;
    protected static final float SELECT_BUTTON_WIDTH = 95f;
    protected static final float TAG_PANEL_HEGHT = 210f;
    protected static final float IMAGE_PANEL_HEIGHT = 300f; //260
    protected static final float IMAGE_WIDTH = 160f;
    protected static final float IMAGE_HEIGHT = 100f;

    protected static final String SHOW_FILTER = "$illent_showFilter";
    protected static final String ALLOW_DUPES = "$illent_dupes";
    protected static final String TAG_SET = "$illent_tagList";

    public void showPanel(InteractionDialogAPI dialogue) {
        VisualCustomPanel.createPanel(dialogue, true);
        showTagPanel(dialogue);
    }

    private void showTagPanel(final InteractionDialogAPI dialogue) {
        float opad = 10f;
        float spad = 3f;

        final CustomPanelAPI panel = VisualCustomPanel.getPanel();
        TooltipMakerAPI panelTooltip = VisualCustomPanel.getTooltip();

        Color baseColor = Misc.getButtonTextColor();
        Color bgColour = Misc.getDarkPlayerColor();
        Color brightColor = Misc.getBrightPlayerColor();

        TooltipMakerAPI lastUsedAnchor;
        TooltipMakerAPI anchor;
        String buttonId;
        ButtonAPI button;
        InteractionDialogCustomPanelPlugin.ButtonEntry entry;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        boolean allowDupes = mem.getBoolean(ALLOW_DUPES);
        boolean showFilter = mem.getBoolean(SHOW_FILTER);

        if (!mem.contains(TAG_SET)) mem.set(TAG_SET, ImagePicker.getTags(dialogue.getInteractionTarget()), 0f);
        Set<String> requiredTags = (Set<String>) mem.get(TAG_SET);

        panelTooltip.addSectionHeading("Change planet visuals", Alignment.MID, opad);

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

        buttonId = "showFilter";
        anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
        button = anchor.addButton(!showFilter ? "Show Filter" : "Hide Filter", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
        entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
            @Override
            public void onToggle() {
                MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
                boolean show = mem.getBoolean(SHOW_FILTER);
                mem.set(SHOW_FILTER, !show, 0f);
                new ImageSelectorPanel().showPanel(dialogue);
            }
        };

        VisualCustomPanel.getPlugin().addButton(entry);
        selectionPanel.addUIElement(anchor).inTL(spad, opad); //first in row
        lastUsedAnchor = anchor;

        //FILTER OPTIONS
        if(showFilter){
            buttonId = "suggested";
            anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
            button = anchor.addButton("Reset Tags", buttonId, baseColor, bgColour, Alignment.MID, CutStyle.C2_MENU, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
            entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
                @Override
                public void onToggle() {
                    Global.getSector().getMemoryWithoutUpdate().set(TAG_SET, ImagePicker.getTags(dialogue.getInteractionTarget()), 0f);
                    new ImageSelectorPanel().showPanel(dialogue);
                }
            };

            VisualCustomPanel.getPlugin().addButton(entry);
            selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
            lastUsedAnchor = anchor;

            buttonId = "duplicates";
            anchor = selectionPanel.createUIElement(SELECT_BUTTON_WIDTH * 1.5f, BUTTON_HEIGHT, false);
            button = anchor.addAreaCheckbox(allowDupes ? "Duplicates: Yes" : "Duplicates: No", new Object(), baseColor, bgColour, brightColor, //new Color(255,255,255,0)
                    SELECT_BUTTON_WIDTH * 1.5f,
                    BUTTON_HEIGHT,
                    0f,
                    false);

            button.setChecked(allowDupes);
            entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
                @Override
                public void onToggle() {
                    MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
                    boolean allow = mem.getBoolean(ALLOW_DUPES);
                    mem.set(ALLOW_DUPES, !allow, 0f);

                    new ImageSelectorPanel().showPanel(dialogue);
                }
            };

            VisualCustomPanel.getPlugin().addButton(entry);
            selectionPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
            lastUsedAnchor = anchor;
        }

        panelTooltip.addCustom(selectionPanel, 0f); //add panel

        //TAG SELECTION
        if(showFilter){
            final CustomPanelAPI tagPanel = panel.createCustomPanel(PANEL_WIDTH_1, TAG_PANEL_HEGHT, new FramedCustomPanelPlugin(0.1f, bgColour, false));
            TooltipMakerAPI tagPanelTooltip = tagPanel.createUIElement(PANEL_WIDTH_1, TAG_PANEL_HEGHT, false);

            float tagsPerRow = 5f;
            int rows = (int) Math.ceil(ImageTags.tagNameList.size() / tagsPerRow);
            int index = 0;

            //add a row
            for (int i = 0; i < rows; i++){
                CustomPanelAPI rowPanel = tagPanel.createCustomPanel(PANEL_WIDTH_1, 40f, new NoFrameCustomPanelPlugin());

                //add the tags in that row
                for(int j = 0; j < (int) tagsPerRow; j++){
                    if (index == ImageTags.tagNameList.size()) break;

                    final String tag = ImageTags.tagNameList.get(index).one;
                    if (tag == null) continue;

                    //button "+"
                    buttonId = tag + (requiredTags.contains(tag) ? "-" : "+");
                    anchor = rowPanel.createUIElement(ARROW_BUTTON_SIZE, ARROW_BUTTON_SIZE, false);
                    bgColour = requiredTags.contains(tag) ? new Color(50, 130, 0, 255) : Misc.getGrayColor();
                    button = anchor.addButton("+", buttonId, Color.BLACK, bgColour, ARROW_BUTTON_SIZE, ARROW_BUTTON_SIZE, 0);
                    entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
                        @Override
                        public void onToggle() {
                            MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
                            Set<String> tagSet = (Set<String>) mem.get(TAG_SET);

                            if (tagSet.contains(tag)) tagSet.remove(tag);
                            else tagSet.add(tag);

                            mem.set(TAG_SET, tagSet, 0f);
                            new ImageSelectorPanel().showPanel(dialogue);
                        }
                    };

                    VisualCustomPanel.getPlugin().addButton(entry);

                    if (j == 0) rowPanel.addUIElement(anchor).inTL(opad, 7f);
                    else rowPanel.addUIElement(anchor).rightOfMid(lastUsedAnchor, opad);
                    lastUsedAnchor = anchor;

                    //tag label
                    TooltipMakerAPI desc = rowPanel.createUIElement(SELECT_BUTTON_WIDTH * 0.8f, BUTTON_HEIGHT, false);
                    LabelAPI label = desc.addPara(ImageTags.tagNameList.get(index).two, opad);

                    rowPanel.addUIElement(desc).rightOfMid(lastUsedAnchor, opad);
                    lastUsedAnchor = desc;

                    index++;
                }

                //add the row to the panel
                tagPanelTooltip.addCustom(rowPanel, i == 0 ? opad : 0f);
            }

            //add the tooltip to the tag panel
            tagPanel.addUIElement(tagPanelTooltip);

            //add the panel to the tooltip
            panelTooltip.addCustom(tagPanel, opad);
        }

        //PICTURES
        List<Integer> imageList = new ArrayList<Integer>(ImagePicker.generateChoices(requiredTags, null, !allowDupes).keySet());
        Collections.sort(imageList);

        ImageDataMemory imageDataMemory = ImageDataMemory.getInstance();

        bgColour = Misc.getDarkPlayerColor();

        float height = showFilter ? IMAGE_PANEL_HEIGHT : IMAGE_PANEL_HEIGHT + TAG_PANEL_HEGHT;

        final CustomPanelAPI imagePanel = panel.createCustomPanel(PANEL_WIDTH_1, height, new FramedCustomPanelPlugin(0.1f, bgColour, false));
        TooltipMakerAPI imagePanelTooltip = imagePanel.createUIElement(PANEL_WIDTH_1, height, true);

        float imagesPerRow = 2f;
        int rows = (int) Math.ceil(imageList.size() / imagesPerRow);
        int index = 0;

        //add a row
        for (int i = 0; i < rows; i++) {
            CustomPanelAPI rowPanel = imagePanel.createCustomPanel(PANEL_WIDTH_1, IMAGE_HEIGHT, new NoFrameCustomPanelPlugin());

            //add the images in that row
            for (int j = 0; j < (int) imagesPerRow; j++) {
                if (index == imageList.size()) break;

                final ImageDataEntry imageData = imageDataMemory.get(imageList.get(index));
                if (imageData == null) continue;

                imageData.load();
                final String path = imageData.getImagePath();

                anchor = rowPanel.createUIElement(IMAGE_WIDTH, IMAGE_HEIGHT, false);
                anchor.addImage(path, IMAGE_WIDTH, 0f);

                if (j == 0) rowPanel.addUIElement(anchor).inTL(opad, 0f);
                else rowPanel.addUIElement(anchor).rightOfTop(lastUsedAnchor, opad);
                lastUsedAnchor = anchor;

                buttonId = imageData.id + "_preview";
                anchor = rowPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
                button = anchor.addButton("Preview", buttonId, baseColor, bgColour, SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);
                entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
                    @Override
                    public void onToggle() {
                        dialogue.getTextPanel().clear();
                        dialogue.getTextPanel().addImage(path);
                    }
                };

                VisualCustomPanel.getPlugin().addButton(entry);
                PositionAPI pos = rowPanel.addUIElement(anchor).rightOfTop(lastUsedAnchor, opad);
                //pos.setYAlignOffset(-(IMAGE_HEIGHT - BUTTON_HEIGHT * 2 - opad) / 2);

                lastUsedAnchor = anchor;

                buttonId = imageData.id + "_set";
                anchor = rowPanel.createUIElement(SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, false);
                button = anchor.addButton("Choose", buttonId, Color.BLACK, new Color(50, 130, 0, 255), SELECT_BUTTON_WIDTH, BUTTON_HEIGHT, 0);

                entry = new InteractionDialogCustomPanelPlugin.ButtonEntry(button, buttonId) {
                    @Override
                    public void onToggle() {
                        ImageHandler.setImage(dialogue.getInteractionTarget(), imageData);
                        dialogue.getTextPanel().clear();
                        printDefaultText(dialogue);
                        new ImageDisplayPanel().showPanel(dialogue);
                    }
                };

                VisualCustomPanel.getPlugin().addButton(entry);
                rowPanel.addUIElement(anchor).belowMid(lastUsedAnchor, opad);

                if (Global.getSettings().isDevMode()){
                    TooltipMakerAPI desc = rowPanel.createUIElement(SELECT_BUTTON_WIDTH * 0.8f, BUTTON_HEIGHT, false);
                    desc.addPara(imageData.id + "", 0f);
                    rowPanel.addUIElement(desc).belowMid(anchor, opad);
                }

                index++;
            }

            imagePanelTooltip.addCustom(rowPanel, opad);
        }

        imagePanel.addUIElement(imagePanelTooltip);
        panelTooltip.addCustom(imagePanel, opad);

        VisualCustomPanel.addTooltipToPanel();
    }

    private void printDefaultText(InteractionDialogAPI dialogue){
        Description desc = Global.getSettings().getDescription(dialogue.getInteractionTarget().getCustomDescriptionId(), Description.Type.CUSTOM);
        if (desc.hasText3()) dialogue.getTextPanel().addParagraph(desc.getText3());

        FireBest.fire(null, dialogue, dialogue.getPlugin().getMemoryMap(), "MarketPostOpen");
    }
}
