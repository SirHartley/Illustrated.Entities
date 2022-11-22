package illustratedEntities.helper;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.memory.TextDataMemory;
import illustratedEntities.plugins.ModPlugin;

public class TextHandler {
    // TODO: 20/11/2022 needs methods to apply all descs, apply single desc ect

    public static TextDataEntry getDataForEntity(SectorEntityToken t){
        int i = t.getMemoryWithoutUpdate().getInt(TextDataEntry.ID_MEM_KEY);

        ModPlugin.log.info("returning text data " + i);
        return TextDataMemory.getInstance().get(i);
    }

    public static void applyAllEntries(){
        for (TextDataEntry e : TextDataMemory.getInstance().getDataMap().values()){
            e.apply();
        }
    }

    public static boolean deleteDescription(SectorEntityToken t){
        TextDataEntry e = getDataForEntity(t);
        if (e == null) return false;

        t.setCustomDescriptionId(null);
        t.getMemoryWithoutUpdate().unset(TextDataEntry.ID_MEM_KEY);
        TextDataMemory.getInstance().remove(e.descriptionNum);

        return true;
    }
}
