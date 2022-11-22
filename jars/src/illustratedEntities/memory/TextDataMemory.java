package illustratedEntities.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.HashMap;
import java.util.Map;

public class TextDataMemory {

    public static final String TEXT_MEMORY = "$illustrated_entities_text_memory";
    private Map<Integer, TextDataEntry> dataMap = new HashMap<>();

    public int getNexFreetNum(){
        return dataMap.size() + 1; //nextnum;
    }

    public static TextDataMemory getInstance(){
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.contains(TEXT_MEMORY)) return (TextDataMemory) mem.get(TEXT_MEMORY);
        else {
            TextDataMemory memory = new TextDataMemory();
            mem.set(TEXT_MEMORY, memory);
            return memory;
        }
    }

    public TextDataEntry get(int i){
        return dataMap.get(i);
    }

    public void set(int i, TextDataEntry entry){
        dataMap.put(i, entry);
    }

    public Map<Integer, TextDataEntry> getDataMap(){
        return dataMap;
    }
}
