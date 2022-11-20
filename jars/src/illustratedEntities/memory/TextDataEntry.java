package illustratedEntities.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.util.Pair;
import illustratedEntities.helper.TextHandler;

import java.util.HashMap;
import java.util.Map;

public class TextDataEntry {
    // TODO: 20/11/2022 descriptions get cleared once the game is restarted, so we have to save them, then regenerate and apply on game start

    public int descriptionNum;
    public String entityID;
    public Map<Integer, String> stringMap = new HashMap<>();

    public String getDescriptionID(){
        return TextHandler.DEFAULT_PREFIX + descriptionNum;
    }

    public Description getDescription(){
        return Global.getSettings().getDescription(getDescriptionID(), Description.Type.CUSTOM);
    }

    public void apply(){
        SectorEntityToken t = Global.getSector().getEntityById(entityID);
        if (t != null) t.setCustomDescriptionId(getDescriptionID());
    }
}
