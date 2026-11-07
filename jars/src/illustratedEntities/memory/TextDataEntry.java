package illustratedEntities.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.loading.Description;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.plugins.ModPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextDataEntry {
    public static final String ID_MEM_KEY = "$Illent_TextMemoryID";
    public static final String DEFAULT_PREFIX = "illent_desc_";

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

    // the editor keeps the whole text in line 1, older saves and tasc copies have one entry per line
    public void setText(int textNum, String text){
        Map<Integer, String> map = textNum == 1 ? stringMap1 : stringMap2;
        map.clear();
        map.put(1, text);
    }

    public String getText(int textNum){
        return parseStringMap(textNum == 1 ? stringMap1 : stringMap2);
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
            String current = t.getCustomDescriptionId();
            if (current != null && !current.startsWith(DEFAULT_PREFIX)) t.getMemoryWithoutUpdate().set(ModPlugin.VANILLA_DESCRIPTION, current);

            t.getMemoryWithoutUpdate().set(ID_MEM_KEY, descriptionNum);
            getDescription().setText1(parseStringMap(stringMap1));
            getDescription().setText3(parseStringMap(stringMap2));
            t.setCustomDescriptionId(getDescriptionID());
        }
    }

    public boolean isValid(){
        for (String s : stringMap1.values()){
            if(s != null && !s.isBlank()) return true;
        }

        for (String s : stringMap2.values()){
            if(s != null && !s.isBlank()) return true;
        }

        return false;
    }

    public String parseStringMap(Map<Integer, String> stringMap){
        StringBuilder out = new StringBuilder();
        int lastLine = 0;
        for (int lineNum : stringMap.keySet()) lastLine = Math.max(lastLine, lineNum);

        for (int i = 1; i <= lastLine; i++){
            String s = line(stringMap, i);

            if (s.isEmpty() && !line(stringMap, i + 1).isEmpty()) {
                out.append("\n\n"); //its an empty line and there is one following it
            } else if (!s.isEmpty()){
                if (!line(stringMap, i - 1).isEmpty() && !Character.isWhitespace(out.charAt(out.length()-1))) out.append(" ");
                out.append(s);
            }
        }

        return out.toString();
    }

    // tasc copies missing lines as null
    private static String line(Map<Integer, String> stringMap, int lineNum){
        String s = stringMap.get(lineNum);
        return s == null ? "" : s;
    }
}
