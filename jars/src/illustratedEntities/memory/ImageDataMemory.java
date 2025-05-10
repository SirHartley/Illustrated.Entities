package illustratedEntities.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import illustratedEntities.loading.Importer;

import java.util.HashMap;
import java.util.Map;

public class ImageDataMemory {
    public static final String IMAGE_MEMORY = "$illustrated_entities_data_memory";

    private Map<Integer, ImageDataEntry> dataMap = new HashMap<>();

    public void loadInitialData(){
        if(dataMap.isEmpty()) dataMap = Importer.loadImageData();
    }

    public static ImageDataMemory getInstance(){
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        if (mem.contains(IMAGE_MEMORY)) return (ImageDataMemory) mem.get(IMAGE_MEMORY);
        else {
            ImageDataMemory memory = new ImageDataMemory();
            memory.loadInitialData();

            mem.set(IMAGE_MEMORY, memory);
            return memory;
        }
    }

    public ImageDataEntry get(int i){
        return dataMap.get(i);
    }

    public void set(int i, ImageDataEntry entry){
        dataMap.put(i, entry);
    }

    public Map<Integer, ImageDataEntry> getDataMap(){
        return dataMap;
    }

    public void forceRefresh(){
        dataMap = new HashMap<>();
        loadInitialData();
    }
}
