package illustratedEntities.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.loading.Description;
import illustratedEntities.helper.TextHandler;

import java.util.HashMap;
import java.util.Map;

public class TextDataEntry {
    // TODO: 20/11/2022 descriptions get cleared once the game is restarted, so we have to save them, then regenerate and apply on game start

    public static final String ID_MEM_KEY = "$Illent_TextMemoryID";
    public static final String DEFAULT_PREFIX = "illent_desc_";
    public static int LINE_AMT = 7;

    public int descriptionNum;
    public String entityID;
    public Map<Integer, String> stringMap1 = new HashMap<>();
    public Map<Integer, String> stringMap2 = new HashMap<>();

    public TextDataEntry(int descriptionNum, String entityID) {
        this.descriptionNum = descriptionNum;
        this.entityID = entityID;
    }

    public void setString(int textNum, int lineNum, String s){
        if (textNum == 1){
            stringMap1.put(lineNum, s);
        } else stringMap2.put(lineNum, s);
    }

    public String getString(int textNum, int lineNum){
        if (textNum == 1){
            return stringMap1.get(lineNum);
        } else return stringMap2.get(lineNum);
    }

    public String getDescriptionID(){
        return DEFAULT_PREFIX + descriptionNum;
    }

    public Description getDescription(){
        return Global.getSettings().getDescription(getDescriptionID(), Description.Type.CUSTOM);
    }

    public void apply(){
        SectorEntityToken t = Global.getSector().getEntityById(entityID);
        if (t != null) {
            t.getMemoryWithoutUpdate().set(ID_MEM_KEY, descriptionNum);
            getDescription().setText1(parseStringMap(stringMap1));
            getDescription().setText3(parseStringMap(stringMap2));
            t.setCustomDescriptionId(getDescriptionID());
        }
    }

    public boolean isValid(){
        for (String s : stringMap1.values()){
            if(!s.isEmpty()) return true;
        }

        for (String s : stringMap2.values()){
            if(!s.isEmpty()) return true;
        }

        return false;
    }

    public String parseStringMap(Map<Integer, String> stringMap){
        StringBuilder out = new StringBuilder();

        for (int i = 1; i <= LINE_AMT; i++){
            String s = stringMap.containsKey(i) ? stringMap.get(i) : "";

            if (s.isEmpty() && stringMap.containsKey(i +1) && !stringMap.get(i + 1).isEmpty()) {
                s = "\n\n"; //its an empty line and there is one following it
                out.append(s);
            } else if (!s.isEmpty()){
                if (stringMap.containsKey(i -1) && !stringMap.get(i - 1).isEmpty()) out.append(" ");
                out.append(s);
            }
        }

        return out.toString();
    }
}
